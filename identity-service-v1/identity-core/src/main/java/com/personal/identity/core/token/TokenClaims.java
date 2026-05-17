package com.personal.identity.core.token;

import java.time.Instant;
import java.util.Set;

/**
 * Claims đóng gói trong JWT access token.
 *
 * <p>Trường gì cần có thì chứa, KHÔNG nhiều hơn - token JWT đi cùng MỌI request,
 * càng nhỏ càng tốt.
 *
 * @param userId          Subject - định danh user
 * @param sessionId       UUID session - dùng để check session có còn ACTIVE
 *                        và update last_active_at trong JwtAuthenticationFilter
 * @param roleCodes       Set role codes - để @PreAuthorize check
 * @param permissionCodes Set permission codes - để @PreAuthorize("hasAuthority('user:read')")
 * @param issuedAt        Tự set khi sign
 * @param expiresAt       Tự set theo TTL config
 */
public record TokenClaims(
        Long userId,
        String sessionId,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Instant issuedAt,
        Instant expiresAt
) {
}
