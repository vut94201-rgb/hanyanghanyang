package com.personal.identity.core.session;

/**
 * Lý do session bị revoke. Quan trọng cho audit log và detect tấn công.
 *
 * <p>Đặc biệt {@link #TOKEN_REUSE}: khi phát hiện refresh token đã dùng được
 * gửi lại, đây là dấu hiệu rất mạnh của tấn công - revoke toàn bộ family
 * và có thể gửi cảnh báo email cho user.
 */
public enum RevokedReason {
    /** User chủ động logout. */
    LOGOUT,
    /** Token reuse detected → revoke cả family. */
    TOKEN_REUSE,
    /** User chủ động logout 1 device khác từ session list. */
    USER_ACTION,
    /** Session quá hạn tự nhiên. */
    EXPIRED
}
