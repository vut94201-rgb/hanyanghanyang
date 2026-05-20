package com.personal.identity.core.audit;

/**
 * Liệt kê các loại action admin được track. Lưu dưới dạng String trong DB
 * (xem migration V5) → enum này define source of truth, đổi tên enum = đổi
 * cả script migration backfill nếu cần.
 *
 * <p><b>Tại sao không gộp với HTTP action verb (GET/POST):</b> action ở đây
 * là business-level, không phải HTTP-level. Một POST có thể là DISABLE_USER
 * hoặc LOCK_USER tùy endpoint. Audit log đọc bởi compliance/security team -
 * họ không quan tâm HTTP method.
 *
 * <p><b>Đặt tên:</b> {@code VERB_OBJECT} - dễ đọc khi grep log.
 */
public enum AdminAction {

    /** Admin liệt kê user. KHÔNG log để tránh noise (đọc-only). */
    // LIST_USERS — intentionally not tracked

    /** Disable: chuyển status sang DISABLED + revoke session. */
    DISABLE_USER,

    /** Lock: chuyển status sang LOCKED (thường do hệ thống auto, admin chỉ trigger manually). */
    LOCK_USER,

    /** Activate: chuyển status DISABLED/LOCKED → ACTIVE. */
    ACTIVATE_USER,

    /** Gán/bỏ role. Payload chứa diff (added, removed). */
    UPDATE_USER_ROLES,

    /** Admin xem chi tiết user (chỉ log khi user là sensitive — placeholder). */
    // VIEW_USER_DETAIL — TODO khi có user "sensitive" cần log read access

    /** Xem audit log. KHÔNG log để tránh meta-noise. */
    // VIEW_AUDIT_LOG — intentionally not tracked
}
