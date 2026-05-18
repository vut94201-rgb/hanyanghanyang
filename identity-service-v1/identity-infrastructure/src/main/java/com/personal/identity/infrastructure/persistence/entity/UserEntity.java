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
 *   <li>{@link #roles} - many-to-many sang {@link RoleEntity} qua bảng {@code user_roles}.
 *       Permissions từ role là RBAC thuần.</li>
 *   <li>{@link #directPermissions} - many-to-many sang {@link PermissionEntity} qua bảng
 *       {@code user_permissions}. Direct grant ngoài role.</li>
 * </ul>
 * Effective permissions = union 2 nguồn (xem {@code User#getEffectivePermissions()} ở domain).
 *
 * <h3>Soft delete</h3>
 * <ul>
 *   <li>{@code @SQLDelete}: gọi {@code repository.delete(...)} sẽ chạy UPDATE thay vì DELETE.
 *       UPDATE tự tăng version để khớp optimistic locking.</li>
 *   <li>{@code @SQLRestriction}: thêm WHERE {@code is_deleted = 0} vào MỌI query →
 *       user soft-deleted bị filter mặc định.</li>
 * </ul>
 *
 * <h3>Fetch strategy</h3>
 * Cả roles và directPermissions đều LAZY. Repository có {@code @EntityGraph} để
 * fetch on-demand, tránh N+1.
 */
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = 1, deleted_at = SYSTIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("is_deleted = 0")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity extends SoftDeletableAuditableEntity {

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

    /** BCrypt hash, độ dài cố định 60. Load thường (không lazy basic). */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private UserStatus accountStatus;

    /**
     * Roles - RBAC nguồn 1. LAZY mặc định.
     * Repository fetch on-demand qua {@code @EntityGraph}.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    /**
     * Direct permissions - nguồn 2 ngoài role.
     *
     * <p>Note: bảng nối {@code user_permissions} có thêm cột audit
     * ({@code granted_at, granted_by, grant_reason}) - JPA không map những cột
     * này khi dùng {@code @ManyToMany} đơn thuần (chỉ map 2 cột FK). Để track
     * audit metadata, sau này có thể tách ra entity nối riêng
     * {@code UserPermissionGrantEntity} với {@code @ManyToOne x2}.
     *
     * <p>Hiện tại fresher demo OK với @ManyToMany - các cột audit vẫn được insert
     * (default SYSTIMESTAMP cho granted_at; granted_by/grant_reason set qua native query).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> directPermissions = new HashSet<>();

    @Override
    public Object getId() {
        return id;
    }
}
