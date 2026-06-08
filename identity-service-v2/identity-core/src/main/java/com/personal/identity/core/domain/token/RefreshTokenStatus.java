package com.personal.identity.core.domain.token;

/**
 * Status of a refresh token within the rotation chain.
 *
 * <p>State machine:
 * <pre>
 * ACTIVE ——> USED     (already rotated into a new token, pointed to by replacedByTokenId)
 * ACTIVE ——> REVOKED  (session revoked / reuse detected)
 * USED   ——> REVOKED  (reuse detected -> immediately mark the USED token as REVOKED for audit clarity)
 * </pre>
 *
 * <p><b>Important for reuse detection:</b> If a client sends a refresh token and
 * the lookup reveals its status is USED, this is an indicator of a stolen token — revoke the entire token family.
 */
public enum RefreshTokenStatus {
    ACTIVE, REVOKED, USED
}
