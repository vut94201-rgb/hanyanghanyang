package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.permission.Permission;
import com.personal.identity.core.domain.permission.Role;
import lombok.Getter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregate root for User.
 *
 * <h2>Authorization Model</h2>
 * A User has 2 sources of permissions:
 * <ol>
 * <li><b>Via role</b> ({@link #roles}) - Pure RBAC. Roles group permissions by
 * logical groups (ADMIN, USER, MODERATOR...).</li>
 * <li><b>Direct grant</b> ({@link #directPermissions}) - assigned directly outside of roles.
 * Flexible for cases where a single user needs a specific permission without creating
 * a junk role.</li>
 * </ol>
 *
 * <p><b>Effective permissions = union(2 sources)</b> - additive only, no deny.
 * See {@link #getEffectivePermissions()}.
 *
 * <p>This is the hybrid RBAC + direct-grant pattern used by Azure AD and AWS IAM.
 *
 * <h2>State Encapsulation</h2>
 *
 * <p>This class does NOT have {@code @Builder}, NO public all-args constructor,
 * NO public setter. There are only 2 ways to construct an instance:
 * <ul>
 * <li>{@link #createNew(String, String, String, String)} - register a NEW user
 * from the business layer (RegisterUseCase). Sets default state: no id,
 * ACTIVE status, empty roles, deleted false.</li>
 * <li>{@link #rehydrate} - <b>ONLY for infrastructure mapper</b>,
 * used when reloading from the DB. Bypasses all business validations.
 * <b>Must not be called from the service/controller layer.</b></li>
 * </ul>
 *
 * <p>All subsequent state changes must go through business methods:
 * {@link #changePassword}, {@link #addRole}, {@link #grantPermission},
 * {@link #disable}, {@link #activate}, {@link #lock}, ...
 *
 * <h2>passwordHash</h2>
 * Has a public getter (mapper needs to read it to persist) but MUST NEVER be exposed via
 * DTO response - this is the responsibility of the controller/mapper layer.
 */
@Getter
public class User {

    private Long id;
    private String username;
    private String emailAddress;

    /** BCrypt hash. NEVER expose via DTO response. */
    private String passwordHash;

    private String fullName;
    private UserStatus accountStatus;

    /** Roles assigned to the user. NOT null (worst case: empty). */
    private Set<Role> roles;

    /**
     * Permissions granted directly to the user, APART FROM permissions from roles.
     * Effective permissions = union(roles' permissions, directPermissions).
     */
    private Set<Permission> directPermissions;

    private Instant createdAt;
    private Instant updatedAt;
    private boolean deleted;
    private Instant deletedAt;

    // ============================================================
    // CONSTRUCTOR - private, only called via factory
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
     * Create a NEW user for the registration flow. Default status ACTIVE, no id
     * (DB will generate), no role - caller MUST call {@link #addRole(Role)}
     * after creation if a default role needs to be assigned.
     *
     * <p>Audit / soft-delete layer left as default: createdAt/updatedAt null
     * (JPA @CreationTimestamp/@UpdateTimestamp will fill upon persist),
     * deleted=false.
     *
     * @throws IllegalArgumentException if any parameter is blank
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
     * <b>ONLY USE IN INFRASTRUCTURE MAPPER.</b> Used when reloading User
     * from DB (UserMapper.toDomain). Bypasses all business validation.
     *
     * <p>Calling this method from the service / controller / use case layer is a
     * code smell - it will be rejected in code review. The business layer must use
     * {@link #createNew} or load via repository.
     *
     * <p>If hard enforcement is needed, an ArchUnit test can be added:
     * <pre>
     * methods().that().areDeclaredIn(User.class).and().haveName("rehydrate")
     * .should().onlyBeCalled().byClassesThat()
     * .resideInAPackage("..infrastructure.persistence.mapper..");
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

    /** Assign role. Idempotent (assigning the same role twice = once). */
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
     * Grant direct permission outside a role. Idempotent.
     *
     * <p>Note: Does NOT check for duplication with role permissions - if a user already has
     * {@code user:read} via a role, granting it directly again is OK; getEffectivePermissions
     * will deduplicate using Set.
     */
    public void grantPermission(Permission permission) {
        if (permission == null) throw new IllegalArgumentException("Permission must not be null");
        this.directPermissions.add(permission);
    }

    /**
     * Revoke direct permission. ONLY affects direct grants - if the user still has this
     * permission via a role, it will remain in effectivePermissions.
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
     * Aggregate all permissions the user actually has.
     *
     * @return Deduplicated Set of permissions, NOT null (worst case: empty).
     */
    public Set<Permission> getEffectivePermissions() {
        Set<Permission> result = new HashSet<>(directPermissions);
        for (Role role : roles) {
            result.addAll(role.getPermissions());
        }
        return result;
    }

    /**
     * Helper for service: retrieve effective permissions as a Set of string codes.
     * Used when building JWT claims or checking {@code hasAuthority(...)}.
     */
    public Set<String> getEffectivePermissionCodes() {
        return getEffectivePermissions().stream()
                .map(Permission::permissionCode)
                .collect(Collectors.toSet());
    }
}