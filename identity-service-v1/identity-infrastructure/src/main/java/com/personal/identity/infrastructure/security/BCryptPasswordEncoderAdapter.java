package com.personal.identity.infrastructure.security;


import com.personal.identity.core.security.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter implements {@link PasswordEncoder} (port của core) bằng cách wrap
 * {@link BCryptPasswordEncoder} của Spring Security.
 *
 * <p><b>Strength = 10</b> (mặc định của Spring) - đây cũng là strength dùng để
 * sinh hash của user seed {@code admin} trong migration {@code V3__seed_data.sql}.
 * Đổi strength sẽ làm hash cũ không verify được, nên KHÔNG đổi tuỳ tiện sau khi
 * đã có user trong DB.
 *
 * <p><b>Lưu ý naming:</b> port của core trùng tên với
 * {@code org.springframework.security.crypto.password.PasswordEncoder}. Khi inject
 * vào service ở core, Spring sẽ resolve theo TYPE - tức là core PasswordEncoder
 * (chỉ adapter này implement) - nên không có nhập nhằng. Còn ở identity-api,
 * nếu cần Spring's PasswordEncoder cho {@code DaoAuthenticationProvider} thì
 * sẽ khai báo bean RIÊNG ở SecurityConfig (bước F), không inject adapter này.
 *
 * <p><b>Vì sao là {@code @Component}, không phải {@code @Service}:</b> adapter là
 * tầng infrastructure thuần kỹ thuật, không phải nghiệp vụ. {@code @Component}
 * trung tính hơn về mặt ngữ nghĩa.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    /**
     * Stateless và thread-safe - khai báo final, dùng chung cho mọi request.
     * KHÔNG tạo mới mỗi lần encode để khỏi tốn CPU init.
     */
    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderAdapter() {
        // Strength 10 = 2^10 = 1024 vòng. ~100ms/hash trên CPU thường - cân bằng
        // giữa bảo mật (chống brute-force) và UX (login không lag).
        this.delegate = new BCryptPasswordEncoder(10);
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        // BCryptPasswordEncoder.matches() đã xử lý null-safe và constant-time compare
        // (chống timing attack). Không tự viết lại.
        return delegate.matches(rawPassword, encodedPassword);
    }
}