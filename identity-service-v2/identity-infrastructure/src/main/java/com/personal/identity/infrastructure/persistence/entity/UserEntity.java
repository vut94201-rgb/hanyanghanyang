package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.domain.user.UserStatus;
import com.personal.identity.infrastructure.persistence.entity.base.SoftDeletableAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;


/**
 * JPA entity for the {@code users} table.
 *
 * <h3>Two Permission Sources</h3>
 * <ul>
 * <li>{@link #roles} - M2M via {@code user_roles} (RBAC).</li>
 * <li>{@link #directPermissions} - M2M via {@code user_permissions} (direct grants).</li>
 * </ul>
 * Effective permissions = the union of both sources.
 *
 * <h3>Soft delete</h3>
 * {@code @SQLDelete} + {@code @SQLRestriction} - DELETE is actually an UPDATE,
 * default queries filter where {@code is_deleted = 0}.
 *
 * <h3>Fetch strategy</h3>
 * roles + directPermissions are LAZY. The repository uses an {@code @EntityGraph} to fetch
 * on-demand via {@code findWithAuthoritiesBy*}.
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

    /** BCrypt hash, fixed length of 60. Eagerly loaded. */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;


    @Column(name = "account_status", nullable = false)
    private UserStatus accountStatus;

    /** Roles - permission source 1 (RBAC). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    /** Direct permissions - permission source 2, independent of roles. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> directPermissions = new HashSet<>();

    // Lombok's @Getter auto-generates Long getId() - fulfilling the abstract method in the base class.
}