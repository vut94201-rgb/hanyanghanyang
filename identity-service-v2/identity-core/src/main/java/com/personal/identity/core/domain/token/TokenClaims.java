package com.personal.identity.core.domain.token;

import java.time.Instant;
import java.util.Set;

/**
 * Claims encapsulated within the JWT access token.
 *
 * <p>Only include fields that are absolutely necessary, NO more - the JWT token accompanies
 * EVERY single request, so the smaller it is, the better.
 *
 * @param tokenId         JTI - The unique UUID of this token. Used as the blacklist key
 *                        upon logout. NULL when building (TokenProvider auto-generates it),
 *                        populated with a value when parsing from a signed token.
 * @param userId          Subject - The user identifier.
 * @param sessionId       Session UUID - Used to verify if the session is still ACTIVE
 *                        and to update last_active_at inside the JwtAuthenticationFilter.
 * @param roleCodes       Set of role codes - Used for @PreAuthorize checks.
 * @param permissionCodes Set of permission codes - Used for @PreAuthorize("hasAuthority('user:read')").
 * @param issuedAt        Automatically set during signing.
 * @param expiresAt       Automatically set according to the TTL configuration.
 */
public record TokenClaims(
        String tokenId,
        Long userId,
        String sessionId,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        Instant issuedAt,
        Instant expiresAt
) {
}
