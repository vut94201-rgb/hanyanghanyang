package com.personal.identity.api.security.config;

import com.personal.identity.core.application.port.in.*;
import com.personal.identity.core.application.port.out.RoleRepository;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.application.security.PasswordEncoder;
import com.personal.identity.core.application.security.SecureRandomGenerator;
import com.personal.identity.core.domain.audit.AuditLogRepository;
import com.personal.identity.core.domain.session.GeoLocationResolver;
import com.personal.identity.core.domain.session.UserAgentParser;
import com.personal.identity.core.domain.token.AccessTokenBlacklist;
import com.personal.identity.core.domain.token.RefreshTokenRepository;
import com.personal.identity.core.domain.token.TokenProvider;
import org.springframework.context.annotation.Bean;

/**
 * Wires the core domain use cases (pure Java) into managed Spring beans.
 *
 * <h2>Why separate this configuration instead of declaring {@code @Service} directly on use cases?</h2>
 * Hexagonal Architectural Axiom — the core domain must remain 100% framework-free. Core use cases
 * should be written as pure Java classes that only depend on core outbound interfaces (ports). If we injected
 * Spring-specific annotations like {@code @Service} or {@code @Autowired} into the core layer, the domain would become
 * "polluted" by infrastructure framework dependencies.
 *
 * <h3>Key Benefits:</h3>
 * <ul>
 * <li>Core components can be thoroughly unit-tested using plain JUnit and Mockito, completely eliminating
 * the performance overhead of loading a heavy {@code @SpringBootTest} context.</li>
 * <li>The core layer can run seamlessly in any non-Spring Java runtime environment (e.g., plain CLI applications,
 * Quarkus, Micronaut).</li>
 * <li>If the underlying framework changes in the future (e.g., migrating to Micronaut), we only need to rewrite
 * this infrastructure configuration class — the entire core domain logic remains completely untouched.</li>
 * </ul>
 *
 * <h2>How Spring resolves dependencies:</h2>
 * Spring automatically injects beans into the {@code @Bean} method parameters by matching their exact TYPE.
 * For instance, the {@code UserRepository userRepository} parameter will be resolved to the concrete
 * {@code UserRepositoryAdapter} instance bean that implements that port interface. This approach eliminates
 * ambiguity since each inbound port maps strictly to exactly one infrastructure adapter.
 *
 * <h2>Important Note on {@link PasswordEncoder}:</h2>
 * The core domain port interface shares the identical simple name with Spring Security's native crypt interface
 * {@code org.springframework.security.crypto.password.PasswordEncoder}. The parameter declared here is explicitly typed
 * to our core port: {@code com.personal.identity.core.security.PasswordEncoder} ⟿ Spring will correctly inject
 * our custom {@code BCryptPasswordEncoderAdapter} bean (which serves as the sole implementation of our core port).
 * Meanwhile, Spring Security's internal {@code PasswordEncoder} bean defined inside the {@code SecurityConfig} class belongs
 * to a distinct package namespace, ensuring the two beans never collide or cause bean mixing bugs.
 */
public class UseCaseConfig {


    @Bean
    public RegisterUseCase registerUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        return new RegisterUseCase(
                userRepository,
                roleRepository,
                passwordEncoder
        );
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

                refreshTokenRepository,
                accessTokenBlacklist,
                tokenProvider,
                sessionRepository
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
        return new RevokeSessionUseCase(
                sessionRepository,
                refreshTokenRepository
        );
    }

    @Bean
    public LogoutAllUseCase logoutAllUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        return new LogoutAllUseCase(
                sessionRepository,
                refreshTokenRepository
        );
    }

    @Bean
    public AdminUserUseCase adminUserUseCase(
            UserRepository userRepository,
          RoleRepository roleRepository,
            SessionRepository sessionRepository,
           AuditLogRepository auditLogRepository
    ) {
        return new AdminUserUseCase(
                userRepository,
                roleRepository,
                sessionRepository,
                auditLogRepository
        );
    }
}
