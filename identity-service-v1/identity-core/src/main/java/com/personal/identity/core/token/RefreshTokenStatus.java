package com.personal.identity.core.token;

/**
 * Trạng thái 1 refresh token trong chain rotation.
 *
 * <p>State machine:
 * <pre>
 *   ACTIVE ──→ USED      (đã rotate ra token mới, có replacedByTokenId trỏ tới)
 *   ACTIVE ──→ REVOKED   (session bị revoke / detect reuse)
 *   USED   ──→ REVOKED   (detect reuse → mark luôn USED token là REVOKED để rõ trong audit)
 * </pre>
 *
 * <p><b>Quan trọng cho reuse detection:</b> Nếu client gửi refresh token mà
 * lookup thấy status = USED, đây là dấu hiệu bị steal token - revoke cả family.
 */
public enum RefreshTokenStatus {
    ACTIVE,
    USED,
    REVOKED
}
