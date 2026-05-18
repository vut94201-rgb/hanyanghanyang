package com.personal.identity.core.service;

import java.time.Instant;

/**
 * Kết quả của 1 lần authenticate thành công - trả về từ
 * {@code LoginUseCase} và {@code RefreshTokenUseCase}.
 *
 * <p><b>Vì sao là record, không class:</b> immutable, không có behavior, chỉ
 * mang data → record là chính xác.
 *
 * <p><b>Quan trọng: {@code rawRefreshToken} chỉ có trong response NÀY.</b>
 * Server KHÔNG lưu raw - chỉ lưu SHA-256 hash trong DB. Client phải tự lưu raw
 * (cookie httpOnly hoặc storage an toàn tương đương). Nếu mất, không có cách
 * recover - phải login lại.
 *
 * <p><b>Vì sao có {@code accessTokenExpiresAt}:</b> client cần biết khi nào access
 * token hết hạn để chủ động gọi refresh trước (tránh request lỡ giữa chừng bị
 * 401). Đây là pattern chuẩn của OAuth2 ({@code expires_in} trong token response).
 *
 * <p><b>Vì sao có {@code sessionId}:</b> client có thể hiển thị cho user (vd: trong
 * "List devices: this session"), và frontend dùng để gọi {@code DELETE /sessions/{id}}.
 *
 * @param accessToken           JWT, đã sign. Đặt vào header {@code Authorization: Bearer ...}.
 * @param rawRefreshToken       Raw refresh token. Lưu client-side, gửi qua endpoint refresh.
 * @param accessTokenExpiresAt  Thời điểm access token expire (UTC instant).
 * @param sessionId             UUID của session - cho client biết phiên hiện tại.
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