package com.personal.identity.core.application.ratelimit;

import java.time.Duration;

public record RateLimitDecision(
        boolean allowed,
        long remainingTokens,
        Duration retryAfter
) {
    public static RateLimitDecision allowed(long remainingTokens) {
        return new RateLimitDecision(true, remainingTokens, Duration.ZERO);
    }

    public static RateLimitDecision rejected(long remainingTokens, Duration retryAfter) {
        return new RateLimitDecision(false, remainingTokens, retryAfter);
    }
}