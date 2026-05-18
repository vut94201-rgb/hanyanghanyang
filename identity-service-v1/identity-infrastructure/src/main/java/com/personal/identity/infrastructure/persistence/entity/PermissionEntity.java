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
 * <p>Kế thừa {@link AuditableEntity} (ở sub-package {@code entity.base}) - có
 * {@code created_at}, {@code updated_at}, {@code version}.
 *
 * <p>KHÔNG khai báo bidirectional sang RoleEntity/UserEntity. Quan hệ many-to-many
 * được khai báo phía Role (cho role_permissions) và phía User (cho user_permissions)
 * - đều là owning side. Khi cần query "permission này thuộc role/user nào", viết
 * query thủ công - tránh phức tạp circular.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
public class PermissionEntity extends AuditableEntity {

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

    @Override
    public Object getId() {
        return id;
    }
}
