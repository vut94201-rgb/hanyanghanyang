package com.personal.identity.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the configuration namespace {@code app.jwt.*} from {@code application.yml}.
 *
 * <p><b>Sample yml (dev):</b>
 * <pre>
 * app:
 * jwt:
 * secret: wDzArL/Cxy/WhtBCG/+fZtp6EQ2so9SxlW0CaAOLJveLY22fpf3mjqnx1BafgNW7
 * issuer: identity-service
 * access-token-ttl-minutes: 15
 * refresh-token-ttl-days: 30
 * </pre>
 *
 * <p><b>Production:</b> {@code secret} MUST be set via the environment variable {@code JWT_SECRET},
 * and NEVER committed to Git. Generate a new secure secret using: {@code openssl rand -base64 48}
 * (provides a 384-bit key, ensuring a comfortable cryptographic margin over the 256-bit minimum required for HS256).
 *
 * <p><b>Why access TTL = 15 minutes, refresh TTL = 30 days:</b>
 * <ul>
 * <li>Short Access Token (15m): If leaked, it expires quickly anyway — limiting the blast radius and minimizing potential damage.</li>
 * <li>Long Refresh Token (30 days): The user does not have to continuously log back in — keeping the UX smooth.</li>
 * <li>Trade-off: If a refresh token is leaked, a hacker could potentially maintain access for up to 30 days — until
 * the user explicitly logs out or token rotation detects a breach (see token reuse detection
 * inside the core's {@code RefreshToken} entity logic).</li>
 * </ul>
 *
 * @param accessTokenTtlMinutes TTL for the access token (in minutes).
 * @param refreshTokenTtlDays   TTL for the refresh token (in days) — utilized by the token rotation service.
 * @param secret                Base64-encoded secret >= 256 bits for HS256 signatures.
 * @param issuer                The {@code iss} claim — helps verify the token originates from the correct trusted source.
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}