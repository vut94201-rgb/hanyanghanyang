package com.personal.identity.core.user;

import com.personal.identity.core.role.Role;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate root cho User.
 *
 * <p><b>Vì sao là class có behavior, KHÔNG phải record?</b>
 * <ul>
 *   <li>User có lifecycle: tạo → đổi password → gán role → disable → soft delete.
 *       Behavior này nên đặt ngay trên domain object thay vì rải rác trong service.</li>
 *   <li>Set roles là mutable collection - record không phù hợp.</li>
 * </ul>
 *
 * <p><b>Lưu ý setter visibility:</b>
 * {@code id}, {@code createdAt}, {@code updatedAt} chỉ có {@code @Setter} ở mức
 * <i>package-private</i> (chỉ mapper trong cùng package mới set được). Code application
 * thông thường KHÔNG được set thủ công các field này - chúng do persistence layer
 * quản lý (id từ sequence, timestamp từ JPA Auditing).
 *
 * <p><b>passwordHash không có getter public:</b>
 * Chỉ {@code AuthService} cần đọc để verify - và service đó được phép. Hash không
 * bao giờ lộ ra ngoài (vd: không xuất hiện trong DTO response). Để cấp quyền đọc
 * cho service mà không leak ra controller, ta dùng method {@link #getPasswordHash()}
 * có Javadoc cảnh báo.
 */
@Getter
@Builder
@NoArgsConstructor
public class User {

    /**
     * PK - auto-generated từ Oracle SEQUENCE {@code users_seq}.
     * Mapper từ entity set giá trị này; code khác KHÔNG set thủ công.
     */
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

    /** Audit field - JPA tự set. */
    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    /** Soft-delete fields. {@code deleted=true} → bị filter khỏi query mặc định. */
    @Setter(AccessLevel.PACKAGE)
    private boolean deleted;

    @Setter(AccessLevel.PACKAGE)
    private Instant deletedAt;

    // ---- Cần tạo ALL-ARGS constructor MANUAL vì @Builder cộng với @Setter(PACKAGE)
    // ---- không tự sinh ra constructor đầy đủ. Lombok @AllArgsConstructor cũng được
    // ---- nhưng để rõ ràng cho fresher đọc code, tôi viết tay:
    public User(
            Long id,
            String username,
            String emailAddress,
            String passwordHash,
            String fullName,
            UserStatus accountStatus,
            Set<Role> roles,
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
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    // ============================================================
    // DOMAIN BEHAVIOR - đặt ở đây thay vì rải rác trong service
    // ============================================================

    /**
     * Đổi mật khẩu. Service gọi {@code PasswordEncoder.encode(plainText)} trước
     * rồi truyền hash vào đây - User domain KHÔNG biết về encoder cụ thể.
     */
    public void changePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("New password hash must not be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    /** Gán thêm role. Idempotent (gán 2 lần cùng role không có hiệu ứng phụ). */
    public void addRole(Role role) {
        if (role == null) throw new IllegalArgumentException("Role must not be null");
        this.roles.add(role);
    }

    /** Bỏ role. */
    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    /** Admin disable user. */
    public void disable() {
        this.accountStatus = UserStatus.DISABLED;
    }

    /** Mở khóa lại - dùng cho cả DISABLED và LOCKED. */
    public void activate() {
        this.accountStatus = UserStatus.ACTIVE;
    }

    /** Auto-lock sau quá nhiều lần login sai. */
    public void lock() {
        this.accountStatus = UserStatus.LOCKED;
    }

    /** Trạng thái có cho phép login không. */
    public boolean canLogin() {
        return accountStatus == UserStatus.ACTIVE && !deleted;
    }
}
