package com.personal.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for JPA entities that need audit columns
 * (created_by, updated_by, created_at, updated_at), optimistic locking (version),
 * and soft-delete/active flags.
 *
 * <p>Requires Spring Data JPA auditing to be enabled at the application level
 * via {@code @EnableJpaAuditing} and an {@code AuditorAware<Long>} bean
 * (configured in identity-bootstrap).
 *
 * <p>Boolean columns use Oracle's {@code NUMBER(1,0)} convention.
 */
@MappedSuperclass
@Setter
@Getter
@EntityListeners(AuditingEntityListener.class)
public class BaseJpaAuditEntity {
    @Version
    @Column(name = "version")
    private Long version;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "active", nullable = false, columnDefinition = "NUMBER(1,0) DEFAULT 1")
    private Boolean active = true;

    @Column(name = "deleted", nullable = false, columnDefinition = "NUMBER(1,0) DEFAULT 0")
    private Boolean deleted = false;
}
