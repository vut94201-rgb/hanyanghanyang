package com.personal.identity.api.dto;

/**
 * Response cho {@code POST /api/v1/auth/logout-all}.
 *
 * <p>Trả số session bị revoke để client hiển thị toast như "Đã đăng xuất khỏi
 * 3 thiết bị". Không cần trả 204 No Content vì thông tin này có ích cho UX.
 *
 * @param revokedCount số session đã bị revoke (gồm cả session hiện tại)
 */
public record LogoutAllResponse(
        int revokedCount
) {
}