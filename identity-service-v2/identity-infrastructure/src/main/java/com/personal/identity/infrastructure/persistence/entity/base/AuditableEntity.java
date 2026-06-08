package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
/**
 * Extends {@link BaseEntity} to add 2 timestamp columns automatically managed via Spring Data
 * JPA Auditing:
 * <ul>
 * <li>{@code created_at} - set once upon INSERT.</li>
 * <li>{@code updated_at} - updated upon every UPDATE.</li>
 * </ul>
 *
 * <p>Subclasses must inherit with a specific type parameter, e.g.:
 * {@code RoleEntity extends AuditableEntity<Long>}.
 *
 * <p>Requirement: the application must have {@code @EnableJpaAuditing} (already present in {@code JpaAuditingConfig}).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity<ID extends Serializable> extends BaseEntity<ID> {
  @CreatedDate
  @Column(nullable = false, updatable = false, name = "created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
