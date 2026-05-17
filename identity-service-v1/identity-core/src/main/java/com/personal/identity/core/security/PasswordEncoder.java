package com.personal.identity.core.security;

/**
 * <b>PORT</b> cho mã hóa và verify password.
 *
 * <p>Implementation mặc định: {@code BCryptPasswordEncoderAdapter} wrap
 * {@code org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}.
 *
 * <p>Đặt port ở core (không dùng thẳng class của Spring Security) để:
 * <ul>
 *   <li>Core không phụ thuộc trực tiếp Spring Security.</li>
 *   <li>Test service không cần load BCrypt thật - mock interface này.</li>
 *   <li>Có thể swap sang Argon2, PBKDF2 nếu sau này yêu cầu.</li>
 * </ul>
 *
 * <p><b>LƯU Ý:</b> trùng tên với {@code org.springframework.security.crypto.password.PasswordEncoder}.
 * Khi import nhớ chọn đúng package.
 */
public interface PasswordEncoder {

    /**
     * Mã hóa plain password thành hash (vd: BCrypt $2a$10$...).
     */
    String encode(String rawPassword);

    /**
     * Verify plain password có khớp với hash không.
     * KHÔNG hash plain rồi so sánh string - BCrypt mỗi lần hash ra kết quả khác
     * (do salt random). Phải dùng method matches của thư viện.
     */
    boolean matches(String rawPassword, String encodedPassword);
}
