package com.personal.identity.infrastructure.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind config namespace {@code app.jwt.*} từ application.yml.
 *
 * <p><b>Sample yml (dev):</b>
 * <pre>
 * app:
 *   jwt:
 *     secret: wDzArL/Cxy/WhtBCG/+fZtp6EQ2so9SxlW0CaAOlJveLY22fpf3mjqnx1BafgNW7
 *     issuer: identity-service
 *     access-token-ttl-minutes: 15
 *     refresh-token-ttl-days: 30
 * </pre>
 *
 * <p><b>Production:</b> {@code secret} phải set qua env {@code JWT_SECRET}, KHÔNG
 * commit vào git. Sinh secret mới: {@code openssl rand -base64 48} (cho 384 bit -
 * dư biên độ so với 256 bit yêu cầu của HS256).
 *
 * <p><b>Vì sao access TTL = 15 phút, refresh TTL = 30 ngày:</b>
 * <ul>
 *   <li>Access ngắn (15p): bị leak cũng nhanh hết hạn → hạn chế damage.</li>
 *   <li>Refresh dài (30 ngày): user không phải login lại liên tục → UX OK.</li>
 *   <li>Trade-off: nếu refresh bị leak, hacker có 30 ngày dùng (cho đến khi
 *       user logout hoặc rotation phát hiện reuse - xem token reuse detection
 *       ở core's RefreshToken).</li>
 * </ul>
 *
 * @param secret               Base64-encoded secret >= 256 bit cho HS256
 * @param issuer               Claim {@code iss} - giúp verify token đúng nguồn
 * @param accessTokenTtlMinutes TTL access token (phút)
 * @param refreshTokenTtlDays   TTL refresh token (ngày) - dùng ở token rotation service
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTokenTtlMinutes,
        long refreshTokenTtlDays
) {
}