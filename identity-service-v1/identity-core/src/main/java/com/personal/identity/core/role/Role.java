package com.personal.identity.core.role;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Aggregate Role. Có behavior (gán / bỏ permission) nên dùng class thay vì record.
 *
 * <p>Quan hệ:
 * <ul>
 *   <li>Many-to-many với {@code User} (qua bảng nối {@code user_roles}).</li>
 *   <li>Many-to-many với {@link Permission} (qua bảng nối {@code role_permissions}).</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
public class Role {

    @Setter(AccessLevel.PACKAGE)
    private Long id;

    /** Mã định danh: ADMIN, USER, MODERATOR. UNIQUE trong DB. */
    private String roleCode;

    /** Tên hiển thị: "Administrator", "Normal User"... */
    private String roleName;

    private String description;

    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Setter(AccessLevel.PACKAGE)
    private Instant createdAt;

    @Setter(AccessLevel.PACKAGE)
    private Instant updatedAt;

    public Role(
            Long id,
            String roleCode,
            String roleName,
            String description,
            Set<Permission> permissions,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.description = description;
        this.permissions = permissions != null ? permissions : new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void addPermission(Permission permission) {
        if (permission == null) throw new IllegalArgumentException("Permission must not be null");
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    /** Tiện kiểm tra trong service: role này có permission code X không? */
    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
                .anyMatch(p -> p.permissionCode().equals(permissionCode));
    }
}
