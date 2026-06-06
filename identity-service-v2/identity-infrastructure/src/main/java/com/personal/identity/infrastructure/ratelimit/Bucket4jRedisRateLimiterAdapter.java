package com.personal.identity.infrastructure.ratelimit;

import com.personal.identity.core.application.ratelimit.RateLimitDecision;
import com.personal.identity.core.application.ratelimit.RateLimiterPort;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(RateLimiterRegistry.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class Bucket4jRedisRateLimiterAdapter implements RateLimiterPort {

    private final RateLimiterRegistry registry;

    @Override
    public RateLimitDecision consumeLogin(String clientIp) {
        return consume(registry.forLogin(clientIp));
    }

    @Override
    public RateLimitDecision consumeRegister(String clientIp) {
        return consume(registry.forRegister(clientIp));
    }

    @Override
    public RateLimitDecision consumeRefresh(String refreshKey) {
        return consume(registry.forRefresh(refreshKey));
    }

    private RateLimitDecision consume(BucketProxy bucket) {
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            return RateLimitDecision.allowed(probe.getRemainingTokens());
        }

        return RateLimitDecision.rejected(
                probe.getRemainingTokens(),
                Duration.ofNanos(probe.getNanosToWaitForRefill())
        );
    }
}