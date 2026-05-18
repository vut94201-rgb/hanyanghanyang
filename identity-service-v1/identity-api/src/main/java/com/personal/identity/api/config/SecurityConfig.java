package com.personal.identity.api.config;

import com.personal.identity.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security config thật - replace permitAll-all-the-things ở giai đoạn skeleton.
 *
 * <h2>Endpoint matchers</h2>
 * <table>
 *   <tr><th>Path</th><th>Method</th><th>Auth</th></tr>
 *   <tr><td>/api/v1/auth/register</td><td>POST</td><td>PUBLIC</td></tr>
 *   <tr><td>/api/v1/auth/login</td><td>POST</td><td>PUBLIC</td></tr>
 *   <tr><td>/api/v1/auth/refresh</td><td>POST</td><td>PUBLIC</td></tr>
 *   <tr><td>/api/v1/auth/logout</td><td>POST</td><td>AUTH</td></tr>
 *   <tr><td>/api/v1/auth/change-password</td><td>POST</td><td>AUTH</td></tr>
 *   <tr><td>/api/health</td><td>*</td><td>PUBLIC</td></tr>
 *   <tr><td>/v3/api-docs/**, /swagger-ui/**</td><td>*</td><td>PUBLIC</td></tr>
 *   <tr><td>Còn lại</td><td>*</td><td>AUTH</td></tr>
 * </table>
 *
 * <h2>Filter chain</h2>
 *
 * <p>{@link JwtAuthenticationFilter} đặt TRƯỚC {@link UsernamePasswordAuthenticationFilter}:
 * <ul>
 *   <li>Filter của ta chạy đầu - extract JWT, set Authentication nếu valid.</li>
 *   <li>Filter Spring built-in không dùng (ta không có form login).</li>
 *   <li>Đặt before UsernamePasswordAuthenticationFilter là pattern chuẩn cho JWT
 *       trong Spring Security 6.</li>
 * </ul>
 *
 * <h2>Stateless</h2>
 *
 * <p>{@code STATELESS} = không tạo HttpSession server-side. Mọi state nằm trong
 * JWT. Spring sẽ không sinh JSESSIONID cookie, không lưu SecurityContext giữa
 * các request. Mỗi request đi qua filter chain mới đầu.
 *
 * <h2>CSRF</h2>
 *
 * <p>Disable. CSRF chỉ cần cho session-based auth (cookie). JWT trong header
 * không bị CSRF attack vì header phải được set bởi JS - mà cross-origin JS
 * không tự thêm header tuỳ ý vào request được (đó là điểm khác cookie).
 *
 * <h2>{@code @Bean PasswordEncoder} đã xóa</h2>
 *
 * <p>Trước đây có bean Spring's {@code PasswordEncoder} - đã xóa vì:
 * <ul>
 *   <li>Không có code nào inject - core dùng port riêng
 *       {@code core.security.PasswordEncoder} (qua {@code BCryptPasswordEncoderAdapter}).</li>
 *   <li>Spring Security không tự inject vì ta không dùng {@code DaoAuthenticationProvider}.</li>
 *   <li>Giữ lại sẽ confuse người đọc code.</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        // Public utility endpoints
                        .requestMatchers(
                                "/api/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Còn lại phải authenticated (gồm logout, change-password, future API)
                        .anyRequest().authenticated()
                )
                // Tắt form login + http basic - chỉ dùng JWT.
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Chèn JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}