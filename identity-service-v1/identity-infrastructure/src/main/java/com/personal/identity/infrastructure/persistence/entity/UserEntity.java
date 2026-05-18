package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.user.UserStatus;
import com.personal.identity.infrastructure.persistence.entity.base.SoftDeletableAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity cho bảng {@code users}.
 *
 * <h3>Phân quyền 2 nguồn</h3>
 * <ul>
 *   <li>{@link #roles} - M2M qua {@code user_roles} (RBAC).</li>
 *   <li>{@link #directPermissions} - M2M qua {@code user_permissions} (direct grant).</li>
 * </ul>
 * Effective permissions = union 2 nguồn.
 *
 * <h3>Soft delete</h3>
 * {@code @SQLDelete} + {@code @SQLRestriction} - DELETE thực ra là UPDATE,
 * query mặc định lọc {@code is_deleted = 0}.
 *
 * <h3>Fetch strategy</h3>
 * roles + directPermissions LAZY. Repository có {@code @EntityGraph} fetch
 * on-demand qua {@code findWithAuthoritiesBy*}.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = 1, deleted_at = SYSTIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = 0")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends SoftDeletableAuditableEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq_gen")
    @SequenceGenerator(
            name = "users_seq_gen",
            sequenceName = "users_seq",
            allocationSize = 50
    )
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "email_address", nullable = false, length = 255, unique = true)
    private String emailAddress;

    /** BCrypt hash, độ dài cố định 60. Load thường. */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private UserStatus accountStatus;

    /** Roles - nguồn permission 1 (RBAC). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    /** Direct permissions - nguồn 2, ngoài role. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> directPermissions = new HashSet<>();

    // Lombok @Getter tự sinh Long getId() - thỏa mãn abstract method ở base.
}
