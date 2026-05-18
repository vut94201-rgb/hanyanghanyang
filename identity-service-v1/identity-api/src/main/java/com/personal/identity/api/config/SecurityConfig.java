package com.personal.identity.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security <b>TẠM THỜI</b> cho giai đoạn "khung xương":
 * <ul>
 *   <li>Tắt CSRF (REST API stateless, không có form).</li>
 *   <li>Stateless session (chuẩn JWT).</li>
 *   <li>Cho phép TẤT CẢ request - vì chưa có JwtAuthenticationFilter.</li>
 * </ul>
 *
 * <p>Sẽ thay thế ở bước Auth (B5 hoặc tương đương) bằng:
 * <pre>{@code
 *   .authorizeHttpRequests(auth -> auth
 *       .requestMatchers("/api/auth/**", "/api/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
 *       .anyRequest().authenticated())
 *   .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
 * }</pre>
 *
 * <p>{@link #passwordEncoder()} đặt ở đây ngay để các module khác inject được sớm
 * (vd: V3 seed migration đã có hash BCrypt $2a, password mới tạo qua API cũng sẽ
 * dùng encoder này).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                     
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * BCryptPasswordEncoder mặc định: strength=10, version $2a.
     * Khớp với hash đã seed trong V3 migration.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
