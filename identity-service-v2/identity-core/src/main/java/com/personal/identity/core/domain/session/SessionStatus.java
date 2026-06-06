package com.personal.identity.core.domain.session;
/**
 * Session status. Matchets {@code CHECK (session_status IN ('ACTIVE','REVOKED','EXPIRED'))}.
 *
 * <p>State machine:
 * <pre>
 *     ACTIVE → REVOKED  (logout / token reuse / admin action)
 *     ACTIVE → EXPIRED  (qua expires_at, cleanup job đánh dấu)
 * </pre>
 * No reverse transitions: a REVOKED/EXPIRED cannot return to ACTIVE.
 */
public enum SessionStatus {
    ACTIVE,REVOKED,EXPIRED
}
