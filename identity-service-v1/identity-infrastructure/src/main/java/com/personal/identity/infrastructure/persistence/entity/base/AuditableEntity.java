package com.personal.identity.infrastructure.persistence.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Mở rộng {@link BaseEntity} thêm 2 cột timestamp tự quản lý:
 * <ul>
 *   <li>{@code created_at} - set 1 lần khi INSERT, KHÔNG đổi sau đó.</li>
 *   <li>{@code updated_at} - update mỗi lần UPDATE.</li>
 * </ul>
 *
 * <p>Cơ chế: Spring Data JPA Auditing thông qua {@link AuditingEntityListener}.
 * Để hoạt động, class application phải có {@code @EnableJpaAuditing}.
 *
 * <p><b>Vì sao dùng {@link Instant}?</b>
 * <ul>
 *   <li>{@code Instant} = thời điểm tuyệt đối trên trục thời gian (UTC), không phụ thuộc timezone server.</li>
 *   <li>Tránh được bug khi server chuyển timezone hoặc deploy nhiều region.</li>
 *   <li>Map sang Oracle {@code TIMESTAMP} sạch sẽ (Hibernate tự convert UTC).</li>
 * </ul>
 *
 * <p>Dùng cho các entity là master data hoặc lookup (Role, Permission) - cần audit
 * khi nào tạo / sửa, KHÔNG cần soft delete.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {

    /**
     * Thời điểm tạo (UTC). Set tự động qua {@code @CreatedDate}, không cho update.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm sửa gần nhất (UTC). Update tự động qua {@code @LastModifiedDate}.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
