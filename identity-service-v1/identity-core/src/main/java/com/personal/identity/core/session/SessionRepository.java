package com.personal.identity.core.session;

import java.util.List;
import java.util.Optional;

/**
 * <b>PORT</b> cho Session persistence.
 */
public interface SessionRepository {

    Session save(Session session);

    Optional<Session> findById(String id);

    /** List session ACTIVE của 1 user - cho API "list devices". */
    List<Session> findActiveByUserId(Long userId);

    /** Tất cả session của user, bất kể status - cho admin xem lịch sử. */
    List<Session> findAllByUserId(Long userId);

    /**
     * Revoke tất cả session ACTIVE của user, TRỪ session hiện tại.
     * Dùng cho "logout from all other devices".
     *
     * @return số session bị revoke
     */
    int revokeAllOtherSessions(Long userId, String currentSessionId, RevokedReason reason);
}
