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
 * Mở rộng {@link BaseEntity} thêm 2 cột timestamp tự quản lý qua Spring Data
 * JPA Auditing:
 * <ul>
 *   <li>{@code created_at} - set 1 lần khi INSERT.</li>
 *   <li>{@code updated_at} - update mỗi lần UPDATE.</li>
 * </ul>
 *
 * <p>Class con phải kế thừa với type parameter cụ thể, vd:
 * {@code RoleEntity extends AuditableEntity<Long>}.
 *
 * <p>Yêu cầu: application có {@code @EnableJpaAuditing} (đã có ở {@code JpaAuditingConfig}).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity<ID extends Serializable> extends BaseEntity<ID> {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
