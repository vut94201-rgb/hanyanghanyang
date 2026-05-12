package com.personal.identity.boostrap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
/**
 * Configuration for Spring Data JPA auditing.
 *
 * <p>Provides the {@code AuditorAware<Long>} bean that
 * {@code AuditingEntityListener} consults to fill {@code @CreatedBy} and
 * {@code @LastModifiedBy} fields on every save.
 *
 * <p><b>Current implementation is a placeholder</b> — it always returns
 * a fixed system user ID ({@code 0L}). This is acceptable during initial
 * development (Hướng A: register flow, no security yet).
 *
 * <p>When Spring Security is wired in (future iteration), replace the
 * implementation with one that reads the authenticated user ID from
 * {@code SecurityContextHolder}, e.g.:
 * <pre>{@code
 * return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
 *         .filter(Authentication::isAuthenticated)
 *         .map(Authentication::getPrincipal)
 *         .filter(p -> p instanceof CustomUserPrincipal)
 *         .map(p -> ((CustomUserPrincipal) p).getUserId());
 * }</pre>
 */
@Configuration
public class JpaAuditingConfig {
    /**
     * Placeholder system user ID used when no authenticated user is available.
     */
    private static final Long SYSTEM_USER_ID = 0L;

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> Optional.of(SYSTEM_USER_ID);
    }
}
