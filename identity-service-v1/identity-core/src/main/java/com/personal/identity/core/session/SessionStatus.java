package com.personal.identity.core.session;

/**
 * Trạng thái session. Khớp với {@code CHECK (session_status IN ('ACTIVE','REVOKED','EXPIRED'))}.
 *
 * <p>State machine:
 * <pre>
 *     ACTIVE → REVOKED  (logout / token reuse / admin action)
 *     ACTIVE → EXPIRED  (qua expires_at, cleanup job đánh dấu)
 * </pre>
 * Không có chuyển ngược: 1 session đã REVOKED/EXPIRED không thể về ACTIVE.
 */
public enum SessionStatus {
    ACTIVE,
    REVOKED,
    EXPIRED
}
