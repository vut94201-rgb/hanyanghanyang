package com.personal.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body cho {@code POST /api/v1/auth/register}.
 *
 * <p><b>Validation:</b>
 * <ul>
 *   <li>{@code username}: 3-50 ký tự, chỉ chữ/số/dấu chấm/underscore/gạch ngang.
 *       KHÔNG cho dấu cách hoặc ký tự đặc biệt → tránh attack qua username
 *       (vd: SQL injection mặc dù JPA đã safe, hoặc render HTML).</li>
 *   <li>{@code emailAddress}: theo RFC 5322 (Bean Validation {@code @Email}).</li>
 *   <li>{@code password}: 8-72 ký tự. BCrypt max input là 72 byte - input dài hơn
 *       sẽ bị truncate silent. Chặn ở DTO để báo lỗi rõ ràng.</li>
 *   <li>{@code fullName}: optional, max 100 ký tự.</li>
 * </ul>
 *
 * <p><b>Không validate password strength (uppercase, số, ký tự đặc biệt) ở đây.</b>
 * NIST 800-63B mới (2017+) khuyên: độ dài > complexity. Bắt user phải có "Aa1!"
 * thực ra làm password yếu hơn (user tạo "Password1!" thay vì passphrase dài).
 * Nếu yêu cầu strict, thêm @Pattern sau, nhưng đừng bắt buộc cho MVP.
 */
        public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "username chỉ được chứa chữ, số, dấu chấm, underscore, gạch ngang")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String emailAddress,

        @NotBlank
        @Size(min = 8, max = 72,
                message = "password phải từ 8 đến 72 ký tự (BCrypt giới hạn 72)")
        String password,

        @Size(max = 100)
        String fullName

) {
}