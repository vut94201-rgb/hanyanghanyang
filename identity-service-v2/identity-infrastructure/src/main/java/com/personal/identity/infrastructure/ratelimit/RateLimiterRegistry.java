package com.personal.identity.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Component
@ConditionalOnBean(ProxyManager.class)
@RequiredArgsConstructor
public class RateLimiterRegistry {

    private static final String KEY_PREFIX_LOGIN = "rl:login:";
    private static final String KEY_PREFIX_REGISTER = "rl:register:";
    private static final String KEY_PREFIX_REFRESH = "rl:refresh:";

    private final ProxyManager<byte[]> proxyManager;
    private final RateLimitProperties properties;

    public BucketProxy forLogin(String clientIp) {
        return bucketFor(KEY_PREFIX_LOGIN + clientIp, () -> buildConfig(properties.login()));
    }

    public BucketProxy forRegister(String clientIp) {
        return bucketFor(KEY_PREFIX_REGISTER + clientIp, () -> buildConfig(properties.register()));
    }

    public BucketProxy forRefresh(String refreshKey) {
        return bucketFor(KEY_PREFIX_REFRESH + refreshKey, () -> buildConfig(properties.refresh()));
    }

    private BucketProxy bucketFor(String key, Supplier<BucketConfiguration> configSupplier) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return proxyManager.builder().build(keyBytes, configSupplier);
    }

    private BucketConfiguration buildConfig(RateLimitProperties.EndpointLimit limit) {
        if (limit == null) {
            throw new IllegalStateException("Rate limit endpoint config must not be null");
        }

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