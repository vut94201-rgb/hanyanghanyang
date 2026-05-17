package com.personal.identity.core.user;

/**
 * Trạng thái tài khoản. Tên enum khớp 1-1 với CHECK constraint trong migration V1:
 * <pre>{@code CHECK (account_status IN ('ACTIVE','DISABLED','LOCKED'))}</pre>
 *
 * <p>Phân biệt:
 * <ul>
 *   <li>{@link #ACTIVE} - hoạt động bình thường, được login</li>
 *   <li>{@link #DISABLED} - admin chủ động vô hiệu hóa, KHÔNG login được.
 *       Có thể được enable lại sau.</li>
 *   <li>{@link #LOCKED} - tự động lock do quá nhiều lần login sai, hoặc lý do
 *       bảo mật khác. Phân biệt với DISABLED ở chỗ: lock là do hệ thống,
 *       disable là do admin.</li>
 * </ul>
 *
 * <p>Soft-delete KHÔNG nằm ở đây - đó là cờ {@code is_deleted} riêng. Một user
 * vừa có thể {@code ACTIVE} vừa có thể đã bị soft-delete (nhưng query mặc định
 * sẽ lọc bỏ).
 */
public enum UserStatus {
    ACTIVE,
    DISABLED,
    LOCKED
}
