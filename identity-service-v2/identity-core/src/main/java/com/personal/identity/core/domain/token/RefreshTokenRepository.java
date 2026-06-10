package com.personal.identity.core.domain.token;

import java.util.List;
import java.util.Optional;

/**
 * <b>PORT</b> for RefreshToken persistence.
 */
public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);

    /**
     * Lookup by hash - this is the ONLY way to find a token (never lookup by plain text).
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findById(String tokenId);

    /**
     * All tokens belonging to a session - used when revoking a token family due to reuse detection.
     */
    List<RefreshToken> findAllBySessionId(String sessionId);

    /**
     * Bulk revokes all ACTIVE tokens of a session.
     *
     * @return the number of revoked tokens
     */
    int revokeAllBySessionId(String sessionId);
}
