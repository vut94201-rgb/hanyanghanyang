package com.personal.identity.core.domain.session;

public record RequestContext(
        String ipAddress,
        String rawUserAgent
) {
    public RequestContext {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("ipAddress is required");
        }
    }
}
