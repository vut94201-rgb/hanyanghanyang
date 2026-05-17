package com.personal.identity.core.token;

import java.util.List;
import java.util.Optional;

/**
 * <b>PORT</b> cho RefreshToken persistence.
 */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    /** Lookup theo hash - đây là cách DUY NHẤT để tìm token (không bao giờ tìm theo plain). */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findById(String id);

    /** Tất cả token thuộc 1 session - dùng khi revoke family vì reuse detection. */
    List<RefreshToken> findAllBySessionId(String sessionId);

    /**
     * Bulk revoke tất cả token ACTIVE của 1 session.
     *
     * @return số token bị revoke
     */
    int revokeAllBySessionId(String sessionId);
}
