package com.personal.identity.api.config;


import com.personal.identity.core.role.RoleRepository;
import com.personal.identity.core.security.PasswordEncoder;
import com.personal.identity.core.security.SecureRandomGenerator;
import com.personal.identity.core.service.*;
import com.personal.identity.core.session.GeoLocationResolver;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.session.UserAgentParser;
import com.personal.identity.core.token.AccessTokenBlacklist;
import com.personal.identity.core.token.RefreshTokenRepository;
import com.personal.identity.core.token.TokenProvider;
import com.personal.identity.core.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wire các use case của core (pure Java) thành Spring bean.
 *
 * <p><b>Vì sao tách config này thay vì gán {@code @Service} trực tiếp lên use case:</b>
 * Triết lý Hexagonal - core 100% framework-free. Use case là pure Java class chỉ
 * depend interface (port). Spring annotation thuộc về tầng api - đặt {@code @Service}
 * vào core sẽ "nhiễm" framework vào domain.
 *
 * <p><b>Lợi ích:</b>
 * <ul>
 *   <li>Core có thể test bằng plain JUnit + Mockito, không cần {@code @SpringBootTest}.</li>
 *   <li>Core có thể chạy trong môi trường không có Spring (CLI, Quarkus...) nếu cần.</li>
 *   <li>Khi đổi framework (vd: sang Micronaut), chỉ phải viết lại config này, core giữ nguyên.</li>
 * </ul>
 *
 * <p><b>Cách Spring resolve dependency:</b> Spring inject bean theo TYPE qua tham
 * số method {@code @Bean}. Vd: tham số {@code UserRepository} sẽ resolve về
 * {@code UserRepositoryAdapter} (chỉ adapter này implement port). Không có nhập
 * nhằng vì mỗi port chỉ có 1 adapter.
 *
 * <p><b>Note về {@code PasswordEncoder}:</b> port core trùng tên với
 * {@code o.s.s.crypto.password.PasswordEncoder}. Ở đây tham số kiểu
 * {@code com.personal.identity.core.security.PasswordEncoder} - Spring sẽ inject
 * {@code BCryptPasswordEncoderAdapter} (bean duy nhất implement core port).
 * Bean Spring Security {@code PasswordEncoder} ở {@code SecurityConfig} là class
 * khác - sẽ không bị mix.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public RegisterUseCase registerUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        return new RegisterUseCase(userRepository, roleRepository, passwordEncoder);
    }

    @Bean
    public LoginUseCase loginUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecureRandomGenerator secureRandomGenerator,
            UserAgentParser userAgentParser,
            GeoLocationResolver geoLocationResolver,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenProvider tokenProvider
    ) {
        return new LoginUseCase(
                userRepository,
                passwordEncoder,
                secureRandomGenerator,
                userAgentParser,
                geoLocationResolver,
                sessionRepository,
                refreshTokenRepository,
                tokenProvider
        );
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SecureRandomGenerator secureRandomGenerator,
            TokenProvider tokenProvider
    ) {
        return new RefreshTokenUseCase(
                refreshTokenRepository,
                sessionRepository,
                userRepository,
                secureRandomGenerator,
                tokenProvider
        );
    }

    @Bean
    public LogoutUseCase logoutUseCase(
            TokenProvider tokenProvider,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            AccessTokenBlacklist accessTokenBlacklist
    ) {
        return new LogoutUseCase(
                tokenProvider,
                sessionRepository,
                refreshTokenRepository,
                accessTokenBlacklist
        );
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new ChangePasswordUseCase(
                userRepository,
                passwordEncoder,
                sessionRepository,
                refreshTokenRepository
        );
    }

    @Bean
    public RevokeSessionUseCase revokeSessionUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new RevokeSessionUseCase(sessionRepository, refreshTokenRepository);
    }

    @Bean
    public LogoutAllUseCase logoutAllUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new LogoutAllUseCase(sessionRepository, refreshTokenRepository);
    }

    @Bean
    public com.personal.identity.core.service.AdminUserUseCase adminUserUseCase(
            UserRepository userRepository,
            com.personal.identity.core.role.RoleRepository roleRepository,
            SessionRepository sessionRepository,
            com.personal.identity.core.audit.AuditLogRepository auditLogRepository
    ) {
        return new com.personal.identity.core.service.AdminUserUseCase(
                userRepository, roleRepository, sessionRepository, auditLogRepository);
    }
}
