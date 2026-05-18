package com.personal.identity.core.user;

import com.personal.identity.core.role.Permission;
import com.personal.identity.core.role.Role;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregate root cho User.
 *
 * <h2>Mô hình phân quyền</h2>
 * User có 2 nguồn permission:
 * <ol>
 *   <li><b>Qua role</b> ({@link #roles}) - RBAC thuần. Role gom permission theo
 *       nhóm logic (ADMIN, USER, MODERATOR...).</li>
 *   <li><b>Direct grant</b> ({@link #directPermissions}) - gán trực tiếp ngoài role.
 *       Linh hoạt cho case 1 user duy nhất cần 1 quyền đặc biệt mà không cần tạo
 *       role rác.</li>
 * </ol>
 *
 * <p><b>Effective permissions = union(2 nguồn)</b> - chỉ additive, không deny.
 * Xem {@link #getEffectivePermissions()}.
 *
 * <p>Đây là pattern hybrid RBAC + ABAC mà Azure AD, AWS IAM dùng.
 *
 * <h2>Đặc điểm class</h2>
 * <ul>
 *   <li>Class có behavior thay vì record - vì có lifecycle: tạo → đổi password →
 *       gán role / direct permission → disable → soft delete.</li>
 *   <li>Setter của id, createdAt, updatedAt, deleted{,At} là package-private -
 *       chỉ mapper trong cùng package set được; code khác không thể.</li>
 *   <li>{@code passwordHash} có getter public nhưng KHÔNG bao giờ lộ qua DTO -
 *       trách nhiệm của tầng controller/mapper.</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
public class User {

    @Setter(AccessLevel.PACKAGE)
    private Long id;

    private String username;
    private String emailAddress;

    /** BCrypt hash. KHÔNG bao giờ lộ qua DTO response. */
    private String passwordHash;

    private String fullName;
    private UserStatus accountStatus;

    /** Roles được gán cho user. KHÔNG null (worst case: rỗng). */
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Permission gán trực tiếp cho user, NGOÀI permission từ role.
     * Effective permissions = union(roles' permissions, directPermissions).
     */
    @Builder.Default
    private Set<Permission> directPermissions = new HashSet<>();

    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    @Setter(AccessLevel.PACKAGE)
    private boolean deleted;

    @Setter(AccessLevel.PACKAGE)
    private Instant deletedAt;

    // ---- All-args constructor cho @Builder (Lombok yêu cầu khi có Setter custom) ----
    public User(
            Long id,
            String username,
            String emailAddress,
            String passwordHash,
            String fullName,
            UserStatus accountStatus,
            Set<Role> roles,
            Set<Permission> directPermissions,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted,
            Instant deletedAt
    ) {
        this.id = id;
        this.username = username;
        this.emailAddress = emailAddress;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.accountStatus = accountStatus;
        this.roles = roles != null ? roles : new HashSet<>();
        this.directPermissions = directPermissions != null ? directPermissions : new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    // ============================================================
    // DOMAIN BEHAVIOR
    // ============================================================

    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("New password hash must not be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    /** Gán role. Idempotent (gán 2 lần cùng role = 1 lần). */
    public void addRole(Role role) {
        if (role == null) throw new IllegalArgumentException("Role must not be null");
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /**
     * Gán direct permission ngoài role. Idempotent.
     *
     * <p>Lưu ý: KHÔNG check trùng với permission từ role - nếu user đã có
     * {@code user:read} qua role, gán direct lại cũng OK, getEffectivePermissions
     * sẽ dedupe bằng Set.
     */
    public void grantPermission(Permission permission) {
        if (permission == null) throw new IllegalArgumentException("Permission must not be null");
        this.directPermissions.add(permission);
    }

    /**
     * Bỏ direct permission. CHỈ ảnh hưởng đến direct - nếu user vẫn có permission
     * này qua role, vẫn còn trong effectivePermissions.
     */
    public void revokePermission(Permission permission) {
        this.directPermissions.remove(permission);
    }

    public void disable() { this.accountStatus = UserStatus.DISABLED; }
    public void activate() { this.accountStatus = UserStatus.ACTIVE; }
    public void lock() { this.accountStatus = UserStatus.LOCKED; }

    public boolean canLogin() {
        return accountStatus == UserStatus.ACTIVE && !deleted;
    }

    /**
     * Tổng hợp tất cả permission user thực sự có.
     *
     * @return Set permission đã dedupe, KHÔNG null (worst case: rỗng).
     */
    public Set<Permission> getEffectivePermissions() {
        Set<Permission> result = new HashSet<>(directPermissions);
        for (Role role : roles) {
            result.addAll(role.getPermissions());
        }
        return result;
    }

    /**
     * Helper cho service: lấy effective permissions dưới dạng Set string code.
     * Dùng khi build JWT claims hoặc check {@code hasAuthority(...)}.
     */
    public Set<String> getEffectivePermissionCodes() {
        return getEffectivePermissions().stream()
                .map(Permission::permissionCode)
                .collect(Collectors.toSet());
    }
}
