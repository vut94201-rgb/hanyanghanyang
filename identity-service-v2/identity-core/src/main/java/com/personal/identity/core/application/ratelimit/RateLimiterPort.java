    package com.personal.identity.core.application.ratelimit;

    public interface RateLimiterPort {

        RateLimitDecision consumeLogin(String clientIp);

        RateLimitDecision consumeRegister(String clientIp);

        RateLimitDecision consumeRefresh(String refreshKey);
    }