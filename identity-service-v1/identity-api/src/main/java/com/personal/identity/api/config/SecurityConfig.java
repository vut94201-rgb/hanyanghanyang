package com.personal.identity.api.config;

import com.personal.identity.api.ratelimit.RateLimitFilter;
import com.personal.identity.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security config thật - replace permitAll-all-the-things ở giai đoạn skeleton.
 *
 * <h2>Endpoint matchers</h2>
 * <table>
 *   <tr><th>Path</th><th>Method</th><th>Auth</th></tr>
 *   <tr><td>/api/v1/auth/register</td><td>POST</td><td>PUBLIC (có rate limit)</td></tr>
 *   <tr><td>/api/v1/auth/login</td><td>POST</td><td>PUBLIC (có rate limit)</td></tr>
 *   <tr><td>/api/v1/auth/refresh</td><td>POST</td><td>PUBLIC (có rate limit)</td></tr>
 *   <tr><td>/api/v1/auth/logout</td><td>POST</td><td>AUTH</td></tr>
 *   <tr><td>/api/v1/auth/change-password</td><td>POST</td><td>AUTH</td></tr>
 *   <tr><td>/api/health</td><td>*</td><td>PUBLIC</td></tr>
 *   <tr><td>/v3/api-docs/**, /swagger-ui/**</td><td>*</td><td>PUBLIC (tắt ở prod)</td></tr>
 *   <tr><td>Còn lại</td><td>*</td><td>AUTH</td></tr>
 * </table>
 *
 * <h2>Filter chain order</h2>
 *
 * <pre>
 *   1. RateLimitFilter           ← chặn brute force trước khi tốn CPU parse JWT
 *   2. JwtAuthenticationFilter   ← validate JWT, set Authentication
 *   3. UsernamePasswordAuthenticationFilter (Spring built-in, không dùng)
 *   4. ... rest of chain
 * </pre>
 *
 * <p>RateLimitFilter là optional (chỉ tạo khi {@code app.rate-limit.enabled=true}).
 * Dùng {@link ObjectProvider} để inject "có thể null" - filter không có thì
 * skip register, app vẫn chạy.
 *
 * <h2>Stateless / CSRF / @Bean PasswordEncoder</h2>
 *
 * <p>STATELESS = không tạo HttpSession. CSRF disable vì JWT không bị CSRF.
 * Bean PasswordEncoder đã xoá ở phiên bản trước - core dùng port riêng
 * {@code core.security.PasswordEncoder} qua {@code BCryptPasswordEncoderAdapter}.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectProvider<RateLimitFilter> rateLimitFilterProvider;

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

                        // Còn lại phải authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Rate limit filter (optional) - đặt TRƯỚC JWT filter trong chain.
        // ObjectProvider.ifAvailable: khi @ConditionalOnProperty tắt rate limit,
        // bean không tồn tại → ifAvailable không chạy → app start bình thường.
        RateLimitFilter rateLimitFilter = rateLimitFilterProvider.getIfAvailable();
        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }
}
