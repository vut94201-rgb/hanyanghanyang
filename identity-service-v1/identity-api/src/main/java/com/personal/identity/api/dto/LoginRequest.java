package com.personal.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cho {@code POST /api/v1/auth/login}.
 *
 * <p><b>Vì sao validation rất nhẹ:</b> không validate format username/password
 * chặt ở login (khác với register). Lý do: nếu user nhập sai format username,
 * trả "Invalid username or password" (qua {@code InvalidCredentialsException})
 * vẫn là behavior đúng - không cần phân biệt "format sai" vs "không tồn tại".
 * Bắt format chặt sẽ leak thông tin username phải có cấu trúc nào đó.
 *
 * <p>Chỉ check {@code @NotBlank} để tránh request rỗng gọi xuống use case không cần thiết.
 */
public record LoginRequest(

        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        @Size(max = 72)
        String password

) {
}