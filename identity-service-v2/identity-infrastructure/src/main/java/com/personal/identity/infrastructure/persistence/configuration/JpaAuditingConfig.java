package com.personal.identity.infrastructure.persistence.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


/**
 * Enables Spring Data's JPA Auditing mechanism:
 * <ul>
 * <li>{@code @CreatedDate} -> automatically set upon INSERT.</li>
 * <li>{@code @LastModifiedDate} -> automatically updated upon UPDATE.</li>
 * </ul>
 *
 * <p>Placing this configuration within the infrastructure module (rather than the api module) ensures that
 * any Spring Boot application consuming this module will inherently have auditing enabled —
 * avoiding redundant configuration repetitions at the API layer.
 *
 * <p>If future requirements necessitate {@code @CreatedBy} / {@code @LastModifiedBy}, an additional bean
 * implementing {@code AuditorAware<String>} can be introduced to read from the SecurityContext (extracting the logged-in username).
 * For the current fresco/demo scope, managing created_at and updated_at is sufficient.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
