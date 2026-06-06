package com.personal.identity.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        EndpointLimit login,
        EndpointLimit register,
        EndpointLimit refresh
) {

    public record EndpointLimit(
            long shortWindowCapacity,
            Duration shortWindowDuration,
            long longWindowCapacity,
            Duration longWindowDuration
    ) {
    }
}