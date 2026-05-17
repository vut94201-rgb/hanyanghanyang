package com.personal.identity.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật cơ chế JPA Auditing của Spring Data:
 * <ul>
 *   <li>{@code @CreatedDate} → tự set khi INSERT.</li>
 *   <li>{@code @LastModifiedDate} → tự update khi UPDATE.</li>
 * </ul>
 *
 * <p>Đặt config ở module infrastructure (không phải api) để bất cứ Spring Boot app
 * nào dùng module này đều có auditing - không cần lặp lại config ở api.
 *
 * <p>Nếu sau này cần {@code @CreatedBy} / {@code @LastModifiedBy}, thêm 1 bean
 * {@code AuditorAware<String>} đọc từ SecurityContext (lấy username đang login).
 * Hiện tại fresher demo chỉ cần created_at/updated_at là đủ.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
