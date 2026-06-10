package com.personal.identity.core.domain.token;

import java.util.Optional;

/**
 * <b>PORT</b> for generating and verifying JWT access tokens.
 *
 * <p>Default implementation: {@code JwtTokenProviderAdapter} utilizing JJWT at the
 * infrastructure layer. The Core domain is completely unaware of HS256, RS256, or JJWT — it only knows
 * "there is a way to generate a token string and parse it."
 */
public interface TokenProvider {
    /**
     * Generates a JWT access token with the minimum required claims.
     *
     * @param claims information attached to the token (userId, sessionId, roles, ...)
     * @return the signed JWT string
     */
    String generateAccessToken(TokenClaims tokenClaims);

    /**
     * Parses and verifies a JWT. Returns {@code Optional.empty()} if:
     * - The signature is invalid, OR
     * - The token has expired, OR
     * - The format is invalid.
     */
    Optional<TokenClaims> parseAndVerify(String token);
}
