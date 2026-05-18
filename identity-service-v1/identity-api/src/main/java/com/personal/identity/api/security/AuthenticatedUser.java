package com.personal.identity.api.security;

/**
 * Principal được set vào {@code SecurityContext} sau khi JWT verify pass.
 *
 * <p>Controller dùng để lấy thông tin user authenticated:
 * <pre>
 * &#64;PostMapping("/me")
 * public X me(&#64;AuthenticationPrincipal AuthenticatedUser user) {
 *     return userService.find(user.userId());
 * }
 * </pre>
 *
 * <p><b>Vì sao record:</b> immutable, gọn, không cần getter/equals manual.
 *
 * <p><b>Vì sao có {@code tokenId} (JTI):</b> cần để logout - blacklist JTI vào Redis.
 * Không có JTI thì logout phải lưu cả raw token (dài, tốn RAM Redis).
 *
 * <p><b>Vì sao có {@code sessionId}:</b> cần cho change-password (revoke other sessions
 * trừ session hiện tại), và cho future use case "list my devices".
 *
 * @param userId    ID user lấy từ JWT subject claim
 * @param sessionId UUID session lấy từ JWT {@code sid} claim
 * @param tokenId   JTI lấy từ JWT {@code jti} claim
 */
public record AuthenticatedUser(
        Long userId,
        String sessionId,
        String tokenId
) {
}