package com.personal.identity.core.domain.session;
/**
 * A bundle of information extracted from the HTTP request that the domain layer needs to know
 * when creating a session or rotating a refresh token.
 *
 * <p>The API layer extracts this from {@link HttpServletRequest} (via {@code RequestContextExtractor}
 * which will be implemented in a later step), and then passes it down as a value object. Thanks to this,
 * the core service has ZERO knowledge of {@link HttpServletRequest} - making it easy to test (only requiring a new RequestContext).
 *
 * @param ipAddress     The IP address, processed for X-Forwarded-For if a proxy exists. Cannot be null.
 * @param rawUserAgent  The raw User-Agent header. Null if the client does not send it (e.g., bare curl).
 */
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
