package com.personal.identity.infrastructure.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(DataRedisProperties redisProperties) {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withTimeout(Duration.ofSeconds(2));

        String password = redisProperties.getPassword();
        if (password != null && !password.isBlank()) {
            uriBuilder.withPassword(password.toCharArray());
        }

        if (redisProperties.getDatabase() != 0) {
            uriBuilder.withDatabase(redisProperties.getDatabase());
        }

        return RedisClient.create(uriBuilder.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> bucket4jRedisConnection(RedisClient client) {
        return client.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public ProxyManager<byte[]> bucket4jProxyManager(
            StatefulRedisConnection<byte[], byte[]> connection
    ) {
        ExpirationAfterWriteStrategy expirationAfterWriteStrategy = ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofHours(2));
        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault().withExpirationAfterWriteStrategy(
                expirationAfterWriteStrategy);
        return LettuceBasedProxyManager.builderFor(connection)
                .withClientSideConfig(clientSideConfig)
                .build();
    }
}