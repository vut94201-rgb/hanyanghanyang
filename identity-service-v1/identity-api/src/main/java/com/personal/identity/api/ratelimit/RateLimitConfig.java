package com.personal.identity.api.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Cấu hình Bucket4j với Redis backend qua Lettuce.
 *
 * <h2>Vì sao Redis backend, không phải in-memory</h2>
 *
 * <p>In-memory bucket (HashMap, Caffeine, ...) chỉ work trên 1 JVM. Khi app
 * scale ra nhiều instance, attacker spread request qua các instance → mỗi
 * instance đếm riêng → tổng request vượt limit mong muốn. Redis là source
 * of truth chung.
 *
 * <h2>Vì sao Lettuce, không phải Jedis</h2>
 *
 * <p>Spring Boot 3.x ship Lettuce mặc định. Project đã có StringRedisTemplate
 * chạy trên Lettuce sẵn → consistent.
 *
 * <h2>Vì sao tự tạo {@link RedisClient}</h2>
 *
 * <p>{@code bucket4j-redis} cần connection raw với {@code ByteArrayCodec},
 * không qua RedisTemplate. Spring Boot không expose connection raw đúng codec
 * → ta tạo client mới đọc cùng RedisProperties. Lettuce share TCP qua
 * multiplexing nên 2 client trỏ cùng Redis không phát sinh nhiều socket.
 *
 * <h2>Conditional bean</h2>
 *
 * <p>{@code @ConditionalOnProperty} - khi yml set {@code enabled=false}, bean
 * không tạo, filter cũng tự skip. Tiện cho test cũ không liên quan rate limit.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    /**
     * Lettuce client đọc host/port từ Spring RedisProperties. Hook shutdown để
     * close khi context dừng (destroyMethod).
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(RedisProperties redisProperties) {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withTimeout(Duration.ofSeconds(2));

        String password = redisProperties.getPassword();
        if (password != null && !password.isBlank()) {
            uriBuilder.withPassword(password.toCharArray());
        }

        return RedisClient.create(uriBuilder.build());
    }

    /**
     * Raw connection với {@link ByteArrayCodec} (yêu cầu của bucket4j-redis).
     */
    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection(RedisClient client) {
        return client.connect(ByteArrayCodec.INSTANCE);
    }

    /**
     * ProxyManager: factory tạo bucket theo key. Bucket key được TTL 2h sau khi
     * fill đầy lại (basedOnTimeForRefillingBucketUpToMax) - đảm bảo bucket đủ
     * dài để cover long window (1h) mà vẫn dọn rác sau khi không dùng.
     */
    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager(
            StatefulRedisConnection<byte[], byte[]> connection
    ) {
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(2)))
                .build();
    }
}
