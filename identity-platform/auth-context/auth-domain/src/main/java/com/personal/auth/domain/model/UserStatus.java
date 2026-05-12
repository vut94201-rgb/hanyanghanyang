package com.personal.auth.domain.model;
/**
 * Lifecycle status of a {@link User}.
 *
 * <ul>
 *   <li>{@link #PENDING} — just registered, email not yet verified (or admin not approved).</li>
 *   <li>{@link #ACTIVE} — verified and can sign in.</li>
 *   <li>{@link #LOCKED} — locked by admin or security policy (e.g. too many failed logins).</li>
 * </ul>
 *
 * <p>Persisted as a string column ({@code VARCHAR2(20)}) — stable identifiers,
 * not ordinals. Never reorder constants in a way that changes name semantics.
 */
public enum UserStatus {
    PENDING,
    ACTIVE,
    LOCKED
}
