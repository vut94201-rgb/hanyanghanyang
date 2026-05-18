package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.infrastructure.persistence.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity cho bảng {@code permissions}.
 *
 * <p>Kế thừa {@code AuditableEntity<Long>} - có {@code created_at}, {@code updated_at},
 * {@code version}, kèm getter {@code Long getId()} type-safe (không phải {@code Object}).
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class PermissionEntity extends AuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permissions_seq_gen")
    @SequenceGenerator(
            name = "permissions_seq_gen",
            sequenceName = "permissions_seq",
            allocationSize = 50
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "permission_code", nullable = false, length = 100, unique = true)
    private String permissionCode;

    @Column(name = "description", length = 500)
    private String description;

    // Note: KHÔNG cần override getId() - Lombok @Getter tự sinh public Long getId()
    // từ field 'id', đã thỏa mãn abstract method ở BaseEntity<Long>.
}
