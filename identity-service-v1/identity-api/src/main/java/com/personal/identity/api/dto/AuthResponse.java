package com.personal.identity.api.dto;


import com.personal.identity.core.service.AuthResult;

import java.time.Duration;
import java.time.Instant;

/**
 * Response cho {@code POST /login} và {@code POST /refresh}.
 *
 * <p><b>Format khớp OAuth2:</b>
 * <ul>
 *   <li>{@code accessToken} - JWT để đặt vào {@code Authorization: Bearer ...}</li>
 *   <li>{@code refreshToken} - raw, client lưu để gọi /refresh sau này.
 *       Server KHÔNG bao giờ lưu raw - chỉ lưu hash trong DB.</li>
 *   <li>{@code tokenType} - hằng {@code "Bearer"} theo OAuth2 RFC 6749.</li>
 *   <li>{@code expiresIn} - số GIÂY (không phải ms) đến lúc access token expire.
 *       Format chuẩn của OAuth2 dùng giây.</li>
 *   <li>{@code sessionId} - extension không có trong OAuth2 chuẩn, để client biết
 *       session nào đang dùng (cho "List devices" UI).</li>
 * </ul>
 *
 * <p><b>Vì sao có factory {@code from(AuthResult)}:</b> tách concern giữa "core
 * trả gì" và "api trả gì". Core dùng {@link AuthResult} với {@code Instant};
 * client thường muốn {@code expiresIn} dạng số giây. Factory làm việc convert.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String sessionId
) {

    /**
     * Build từ {@link AuthResult} của core. {@code expiresIn} tính từ now → expiresAt.
     */
    public static AuthResponse from(AuthResult result) {
        long expiresInSeconds = Duration.between(Instant.now(), result.accessTokenExpiresAt())
                .getSeconds();
        // Negative chỉ xảy ra nếu clock skew lạ; floor về 0 cho khỏi confuse client.
        if (expiresInSeconds < 0) {
            expiresInSeconds = 0;
        }
        return new AuthResponse(
                result.accessToken(),
                result.rawRefreshToken(),
                "Bearer",
                expiresInSeconds,
                result.sessionId()
        );
    }
}