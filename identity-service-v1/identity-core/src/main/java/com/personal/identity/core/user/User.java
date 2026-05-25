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
 * <p>Đây là pattern hybrid RBAC + direct-grant mà Azure AD, AWS IAM dùng.
 *
 * <h2>Đóng gói state (encapsulation)</h2>
 *
 * <p>Class này KHÔNG có {@code @Builder}, KHÔNG có public all-args constructor,
 * KHÔNG có public setter. Chỉ có 2 đường dựng instance:
 * <ul>
 *   <li>{@link #createNew(String, String, String, String)} - đăng ký user MỚI
 *       từ tầng nghiệp vụ (RegisterUseCase). Set state mặc định: chưa có id,
 *       status ACTIVE, roles rỗng, deleted false.</li>
 *   <li>{@link #rehydrate} - <b>CHỈ dành cho infrastructure mapper</b>,
 *       dùng khi load lại từ DB. Bypass mọi validation nghiệp vụ.
 *       <b>Không được gọi từ tầng service/controller.</b></li>
 * </ul>
 *
 * <p>Mọi thay đổi state sau đó đều phải qua method nghiệp vụ:
 * {@link #changePassword}, {@link #addRole}, {@link #grantPermission},
 * {@link #disable}, {@link #activate}, {@link #lock}, ...
 *
 * <h2>passwordHash</h2>
 * Có getter public (mapper cần đọc để persist) nhưng KHÔNG bao giờ được lộ qua
 * DTO response - trách nhiệm của tầng controller/mapper.
 */
@Getter
public class User {

    private Long id;
    private String username;
    private String emailAddress;

    /** BCrypt hash. KHÔNG bao giờ lộ qua DTO response. */
    private String passwordHash;

    private String fullName;
    private UserStatus accountStatus;

    /** Roles được gán cho user. KHÔNG null (worst case: rỗng). */
    private Set<Role> roles;

    /**
     * Permission gán trực tiếp cho user, NGOÀI permission từ role.
     * Effective permissions = union(roles' permissions, directPermissions).
     */
    private Set<Permission> directPermissions;

    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
    private Instant deletedAt;

    // ============================================================
    // CONSTRUCTOR - private, chỉ gọi qua factory
    // ============================================================

    private User(
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
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.directPermissions = directPermissions != null
                ? new HashSet<>(directPermissions)
                : new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    // ============================================================
    // FACTORY METHODS
    // ============================================================

    /**
     * Tạo user MỚI cho luồng đăng ký. Status mặc định ACTIVE, chưa có id
     * (DB sẽ gen), chưa có role - caller PHẢI gọi {@link #addRole(Role)}
     * sau khi tạo nếu cần gán role mặc định.
     *
     * <p>Tầng audit / soft-delete để mặc định: createdAt/updatedAt null
     * (JPA @CreationTimestamp/@UpdateTimestamp sẽ điền khi persist),
     * deleted=false.
     *
     * @throws IllegalArgumentException nếu bất kỳ tham số nào blank
     */
    public static User createNew(
            String username,
            String emailAddress,
            String passwordHash,
            String fullName
    ) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (emailAddress == null || emailAddress.isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return new User(
                null,
                username,
                emailAddress,
                passwordHash,
                fullName,
                UserStatus.ACTIVE,
                new HashSet<>(),
                new HashSet<>(),
                null,
                null,
                false,
                null
        );
    }

    /**
     * <b>CHỈ DÙNG TRONG INFRASTRUCTURE MAPPER.</b> Dùng khi load lại User
     * từ DB (UserMapper.toDomain). Bypass mọi business validation.
     *
     * <p>Việc gọi method này từ tầng service / controller / use case là
     * code smell - sẽ bị reject ở code review. Tầng nghiệp vụ phải dùng
     * {@link #createNew} hoặc load qua repository.
     *
     * <p>Nếu cần enforce cứng, có thể bổ sung ArchUnit test:
     * <pre>
     * methods().that().areDeclaredIn(User.class).and().haveName("rehydrate")
     *     .should().onlyBeCalled().byClassesThat()
     *     .resideInAPackage("..infrastructure.persistence.mapper..");
     * </pre>
     */
    public static User rehydrate(
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
        return new User(
                id,
                username,
                emailAddress,
                passwordHash,
                fullName,
                accountStatus,
                roles,
                directPermissions,
                createdAt,
                updatedAt,
                deleted,
                deletedAt
        );
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

    public void replaceRoles(Set<Role> newRoles) {
        this.roles = new HashSet<>(newRoles);
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