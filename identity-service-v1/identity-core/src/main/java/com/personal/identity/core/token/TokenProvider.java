package com.personal.identity.core.token;

import java.util.Optional;

/**
 * <b>PORT</b> cho việc sinh và verify JWT access token.
 *
 * <p>Implementation mặc định: {@code JwtTokenProviderAdapter} dùng JJWT ở
 * infrastructure. Core không biết HS256, RS256, hay JJWT - chỉ biết "có cách
 * tạo string token và parse nó".
 */
public interface TokenProvider {

    /**
     * Tạo JWT access token với claims tối thiểu.
     *
     * @param claims thông tin gắn vào token (userId, sessionId, roles, ...)
     * @return string JWT đã sign
     */
    String generateAccessToken(TokenClaims claims);

    /**
     * Parse và verify JWT. Trả Optional.empty() nếu:
     * - chữ ký sai, hoặc
     * - token expired, hoặc
     * - format không hợp lệ.
     */
    Optional<TokenClaims> parseAndVerify(String token);
}
