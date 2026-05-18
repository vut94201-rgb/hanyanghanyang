package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.infrastructure.persistence.entity.base.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity cho bảng {@code roles}.
 *
 * <p>Là owning side của quan hệ many-to-many với {@link PermissionEntity}.
 * Bảng nối {@code role_permissions} đã có trong migration V1.
 *
 * <p><b>Fetch strategy:</b> {@code permissions} là {@code LAZY}.
 * Repository {@code RoleJpaRepository} có method {@code findWithPermissions*}
 * dùng {@code @EntityGraph} để fetch có chọn lọc, tránh N+1.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class RoleEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "roles_seq_gen")
    @SequenceGenerator(
            name = "roles_seq_gen",
            sequenceName = "roles_seq",
            allocationSize = 50
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "role_code", nullable = false, length = 50, unique = true)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "description", length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    @Override
    public Object getId() {
        return id;
    }
}
