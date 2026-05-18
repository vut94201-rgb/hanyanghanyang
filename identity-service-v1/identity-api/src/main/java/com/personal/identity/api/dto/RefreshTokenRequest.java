package com.personal.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cho {@code POST /api/v1/auth/refresh}.
 *
 * <p>Raw refresh token là Base64 URL-safe 32 byte → 43 ký tự. Cho thêm biên độ
 * tới 256 ký tự đề phòng format đổi.
 */
public record RefreshTokenRequest(

        @NotBlank
        @Size(max = 256)
        String refreshToken

) {
}