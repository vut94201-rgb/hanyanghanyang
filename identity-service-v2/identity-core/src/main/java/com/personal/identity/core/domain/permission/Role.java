package com.personal.identity.core.domain.permission;

import lombok.Getter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Role Aggregate. Since it contains behaviors (assigning / removing permissions),
 * a class should be used instead of a record.
 *
 * <p>Relationships:
 *
 * <ul>
 * <li>Many-to-many with {@code User} (via the join table {@code user_roles}).
 * <li>Many-to-many with {@link Permission} (via the join table {@code role_permissions}).
 * </ul>
 */
@Getter
public class Role {
    private Long id;
    private String roleCode;
    private String description;
    private Set<Permission> permissions;
    private Instant createdAt;
    private Instant updatedAt;
    private String roleName;

    private Role(
            Long id,
            String roleCode,
            String roleName,
            String description,
            Set<Permission> permissions,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.description = description;
        this.permissions = permissions != null ? new HashSet<>(permissions) : new HashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Role createNew(String roleCode, String roleName, String description) {
        if (Objects.isNull(roleCode) || roleCode.isBlank()) {
            throw new IllegalArgumentException("roleCode must not be blank");
        }

        if (Objects.isNull(roleName) || roleName.isBlank()) {
            throw new IllegalArgumentException("roleName must not be blank");
        }
        return new Role(null, roleCode, roleCode, description, new HashSet<>(), null, null);
    }

    public static Role rehydrate(Long id,
                                 String roleCode,
                                 String roleName,
                                 String description,
                                 Set<Permission> permissions,
                                 Instant createdAt,
                                 Instant updatedAt) {
        return new Role(id, roleCode, roleName, description, permissions, createdAt, updatedAt);
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    public void addPermission(Permission permission) {
        if (Objects.isNull(permission)) {
            throw new IllegalArgumentException("permission must not be null");
        }
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        if (Objects.isNull(permission)) {
            throw new IllegalArgumentException("permission must not be null");
        }
        this.permissions.remove(permission);
    }

    public boolean hasPermission(String permissionCode) {
        if (Objects.isNull(permissionCode) || permissionCode.isBlank()) {
            return false;
        }
        return this.permissions.stream().anyMatch(permission -> permission.permissionCode().equals(permissionCode));

    }


    public void rename(String newRoleName) {
        if (newRoleName == null || newRoleName.isBlank()) {
            throw new IllegalArgumentException("roleName must not be blank");
        }
        this.roleName = newRoleName;
    }

    public void changeDescription(String newDescription) {
        if (Objects.isNull(newDescription) || newDescription.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        this.description = newDescription;
    }
}
