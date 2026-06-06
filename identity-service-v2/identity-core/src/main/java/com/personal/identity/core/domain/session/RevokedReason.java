package com.personal.identity.core.domain.session;
/**
 * Reasons for session revocation. Important for  audit logs and attack detection.
 *
 * <p>Special {@link #TOKEN_REUSE}: when a used refresh token is detected
 * this is a  very strong indicator of an attack - revoke the entire token family
 * and optionally send an email alert to the user
 */
public enum RevokedReason {
    /**  Admin revoke session/user */
    ADMIN_REVOKED,
    /**  The user logged out */
    LOGOUT,
    /**  The user manually logs out of another device from sessions list */
    USER_ACTION,
    /** The token has expired naturally */
    EXPIRED,
    /** Token reuse detected → revoke all family. */
    TOKEN_REUSE
}
