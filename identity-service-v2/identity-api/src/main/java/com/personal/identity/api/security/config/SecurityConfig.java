package com.personal.identity.api.security.config;

import com.personal.identity.api.ratelimit.RateLimitFilter;
import com.personal.identity.api.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Production-ready Spring Security configuration — replacing the temporary skeleton-phase permitAll-all-the-things settings.
 *
 * <h2>Endpoint Matchers</h2>
 * <table>
 * <tr><th>Request Path</th><th>HTTP Method</th><th>Authentication Policy</th></tr>
 * <tr><td>/api/v1/auth/register</td><td>POST</td><td>PUBLIC (Rate limited)</td></tr>
 * <tr><td>/api/v1/auth/login</td><td>POST</td><td>PUBLIC (Rate limited)</td></tr>
 * <tr><td>/api/v1/auth/refresh</td><td>POST</td><td>PUBLIC (Rate limited)</td></tr>
 * <tr><td>/api/v1/auth/logout</td><td>POST</td><td>AUTHENTICATED</td></tr>
 * <tr><td>/api/v1/auth/change-password</td><td>POST</td><td>AUTHENTICATED</td></tr>
 * <tr><td>/api/health</td><td>*</td><td>PUBLIC</td></tr>
 * <tr><td>/v3/api-docs/**, /swagger-ui/**</td><td>*</td><td>PUBLIC (Disabled in production)</td></tr>
 * <tr><td>All remaining paths</td><td>*</td><td>AUTHENTICATED</td></tr>
 * </table>
 *
 * <h2>Filter Chain Order Execution</h2>
 * <pre>
 * 1. RateLimitFilter         ⟿ Thwarts brute-force attacks prior to consuming CPU parsing heavy JWTs.
 * 2. JwtAuthenticationFilter ⟿ Validates the incoming JWT and populates the Authentication token.
 * 3. UsernamePasswordAuthenticationFilter (Spring built-in mechanism — explicitly bypassed/unused).
 * 4. ... rest of the filter chain
 * </pre>
 *
 * <p>The {@code RateLimitFilter} is optional (conditionally managed via the property {@code app.rate-limit.enabled=true}).
 * We utilize an {@link ObjectProvider} to inject it as a "nullable dependency" — if the filter bean is absent,
 * the application skips its registration seamlessly and boots up normally.
 *
 * <h2>Stateless Architecture / CSRF / PasswordEncoder Decoupling</h2>
 *
 * <p><b>STATELESS:</b> Completely suppresses the generation of standard {@code HttpSession} tracking.
 * CSRF defense is safely disabled since JWT-based authentication is inherently immune to CSRF vectors when structured properly.
 *
 * <p><b>PasswordEncoder Bean:</b> The standard Spring Security {@code PasswordEncoder} bean declaration was intentionally
 * purged in the previous iteration. The core domain now mandates its own independent port interface
 * {@code core.security.PasswordEncoder}, bridged seamlessly via our infrastructure adapter implementation {@code BCryptPasswordEncoderAdapter}.
 */
@Configuration
@EnableMethodSecurity
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
                        // Public authentication endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh"
                        ).permitAll()

                        // Public system utility endpoints
                        .requestMatchers(
                                "/api/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Admin endpoints require authentication first, then method-level role checks.
                        .requestMatchers("/api/v1/admin/**").authenticated()

                        // All remaining requests must be strictly authenticated
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // Register the core JWT processing filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Rate limit filter (optional) - positioned strictly BEFORE the JWT filter within the security chain.
        // rateLimitFilterProvider.getIfAvailable(): When @ConditionalOnProperty disables the rate limiter,
        // the bean is absent from the container ⟿ getIfAvailable() returns null ⟿ registration is skipped, allowing the app to boot normally.
        RateLimitFilter rateLimitFilter = rateLimitFilterProvider.getIfAvailable();
        if (rateLimitFilter != null) {
            http.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }
}
