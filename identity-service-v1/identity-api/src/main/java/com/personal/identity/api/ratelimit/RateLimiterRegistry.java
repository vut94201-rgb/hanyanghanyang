package com.personal.identity.api.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Registry trung gian giữa filter và Bucket4j ProxyManager.
 *
 * <p><b>Vì sao có lớp này:</b>
 * <ul>
 *   <li>Đóng gói việc build {@link BucketConfiguration} từ properties - filter
 *       không phải biết cách tạo bandwidth.</li>
 *   <li>Tạo key Redis có namespace ("rl:login:1.2.3.4") - prefix nhất quán với
 *       blacklist:access:* để dễ debug bằng redis-cli.</li>
 *   <li>Mỗi endpoint là 1 method để filter gọi rõ ràng:
 *       {@code registry.forLogin(ip)} thay vì truyền config dài dòng.</li>
 * </ul>
 *
 * <h2>Pattern: 2 bandwidth trong cùng 1 bucket</h2>
 *
 * <p>Bucket4j cho phép 1 bucket có nhiều limit cùng lúc. Khi consume 1 token,
 * cả 2 bandwidth đều bị trừ. Request đi qua khi cả 2 còn dư token. Đây là cách
 * implement multi-tier rate limit (short + long window) trong 1 bucket atomic.
 */
@Component
@ConditionalOnBean(ProxyManager.class)
@RequiredArgsConstructor
public class RateLimiterRegistry {

    private static final String KEY_PREFIX_LOGIN = "rl:login:";
    private static final String KEY_PREFIX_REGISTER = "rl:register:";
    private static final String KEY_PREFIX_REFRESH = "rl:refresh:";

    private final ProxyManager<byte[]> proxyManager;
    private final RateLimitProperties properties;

    /**
     * Lấy bucket cho endpoint login, key theo IP client.
     */
    public BucketProxy forLogin(String clientIp) {
        return bucketFor(KEY_PREFIX_LOGIN + clientIp, () -> buildConfig(properties.login()));
    }

    /**
     * Lấy bucket cho endpoint register, key theo IP client.
     */
    public BucketProxy forRegister(String clientIp) {
        return bucketFor(KEY_PREFIX_REGISTER + clientIp, () -> buildConfig(properties.register()));
    }

    /**
     * Lấy bucket cho endpoint refresh, key theo 8 ký tự đầu của refresh token hash.
     *
     * <p>KHÔNG dùng raw token làm key (lộ token trong Redis log). Hash prefix
     * cũng đủ unique để rate-limit (cùng token chắc chắn hash giống nhau).
     */
    public BucketProxy forRefresh(String tokenHashPrefix) {
        return bucketFor(KEY_PREFIX_REFRESH + tokenHashPrefix,
                () -> buildConfig(properties.refresh()));
    }

    /**
     * Helper chung - lấy hoặc tạo bucket cho key.
     *
     * <p>{@code getProxy} là lazy: bucket Redis chỉ được tạo lúc consume() đầu
     * tiên, không phải lúc gọi method này. Nên không tốn round-trip Redis cho
     * mọi request vào method.
     */
    private BucketProxy bucketFor(String key, Supplier<BucketConfiguration> configSupplier) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return proxyManager.builder().build(keyBytes, configSupplier);
    }

    /**
     * Tạo {@link BucketConfiguration} với 2 bandwidth (short + long window).
     *
     * <p>{@code Bandwidth.builder().capacity(N).refillGreedy(N, duration)}:
     * bucket bắt đầu đầy N token, mỗi {@code duration} refill đầy lại N token.
     * "Greedy" nghĩa là refill liên tục (1 token mỗi duration/N giây) thay vì
     * 1 cục N token mỗi duration giây - smoothing hơn cho UX.
     */
    private BucketConfiguration buildConfig(RateLimitProperties.EndpointLimit limit) {
        Bandwidth shortBandwidth = Bandwidth.builder()
                .capacity(limit.shortWindowCapacity())
                .refillGreedy(limit.shortWindowCapacity(), limit.shortWindowDuration())
                .build();

        Bandwidth longBandwidth = Bandwidth.builder()
                .capacity(limit.longWindowCapacity())
                .refillGreedy(limit.longWindowCapacity(), limit.longWindowDuration())
                .build();

        return BucketConfiguration.builder()
                .addLimit(shortBandwidth)
                .addLimit(longBandwidth)
                .build();
    }
}
