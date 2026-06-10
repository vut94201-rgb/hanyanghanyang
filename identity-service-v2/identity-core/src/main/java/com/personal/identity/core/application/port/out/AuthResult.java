package com.personal.identity.core.application.port.out;

import java.time.Instant;
/**
 * Represents the result of a successful authentication execution — returned from
 * both {@code LoginUseCase} and {@code RefreshTokenUseCase}.
 *
 * <p><b>Why it is a Java record instead of a class:</b> It is an immutable value object
 * with no encapsulated behaviors; its sole responsibility is to transport structured data ⟿ a record
 * is the most precise semantic fit.
 *
 * <p><b>Critical: {@code rawRefreshToken} is ONLY available in THIS response object.</b>
 * The server DOES NOT persist this raw value — it only stores its SHA-256 hash in the database.
 * The client is strictly responsible for self-persisting this raw token string (e.g., inside a secure,
 * HttpOnly cookie or an equivalent secure storage mechanism). If this raw token is lost, there is
 * no recovery mechanism on the server side ⟿ the user must perform a full re-authentication (re-login).
 *
 * <p><b>Why {@code accessTokenExpiresAt} is included:</b> The client application needs to know exactly
 * when the access token will expire so it can proactively initiate a refresh sequence (mitigating the risk
 * of an active request suddenly failing mid-flight due to a 401 Unauthorized error). This mirrors the standard
 * {@code expires_in} pattern specified in the OAuth2 token response specification.
 *
 * <p><b>Why {@code sessionId} is included:</b> Allows the client application to display current session
 * information to the end-user (e.g., within a "List active devices/sessions for this account" UI view).
 * The frontend also requires this identifier to invoke target revocation endpoints via {@code DELETE /sessions/{id}}.
 *
 * @param accessToken           The signed JWT. To be attached to the HTTP request header as {@code Authorization: Bearer ...}.
 * @param rawRefreshToken       The raw cryptographic refresh token string. Kept client-side, transmitted exclusively via the refresh endpoint.
 * @param accessTokenExpiresAt  The point in time when the access token expires (UTC instant).
 * @param sessionId             The unique UUID of the session — used by the client to identify the current active session instance.
 */
public record AuthResult(
        String accessToken,
        String rawRefreshToken,
        Instant accessTokenExpiresAt,
        String sessionId
) {
    public AuthResult {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("rawRefreshToken is required");
        }
        if (accessTokenExpiresAt == null) {
            throw new IllegalArgumentException("accessTokenExpiresAt is required");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
    }
}