package com.personal.identity.core.domain.user;

/**
 * Account status. The enum name matches exactly with the CHECK constraint in migration V1:
 * <pre>{@code CHECK (account_status IN ('ACTIVE','DISABLED','LOCKED'))}</pre>
 *
 * <p>Distinctions:
 * <ul>
 * <li>{@link #ACTIVE} - operating normally, login allowed.</li>
 * <li>{@link #DISABLED} - manually disabled by an admin, login NOT allowed.
 * Can be re-enabled later.</li>
 * <li>{@link #LOCKED} - automatically locked due to too many failed login attempts, or other
 * security reasons. Distinction from DISABLED: lock is triggered by the system,
 * disable is triggered by an admin.</li>
 * </ul>
 *
 * <p>Soft-delete is NOT handled here - that uses a separate {@code is_deleted} flag. A user
 * can be both {@code ACTIVE} and soft-deleted (but default queries
 * will filter them out).
 */
public enum UserStatus {
    ACTIVE, LOCKED, DISABLED
}
