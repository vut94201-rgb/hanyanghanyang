package com.personal.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cho {@code POST /api/v1/auth/change-password}.
 *
 * <p>{@code userId} và {@code currentSessionId} KHÔNG ở đây - lấy từ JWT claims
 * trong filter, không tin DTO. Cho client gửi userId trong body sẽ là lỗ hổng
 * privilege escalation (gửi userId người khác).
 */
public record ChangePasswordRequest(

        @NotBlank
        @Size(max = 72)
        String currentPassword,

        @NotBlank
        @Size(min = 8, max = 72,
                message = "password mới phải từ 8 đến 72 ký tự")
        String newPassword

) {
}