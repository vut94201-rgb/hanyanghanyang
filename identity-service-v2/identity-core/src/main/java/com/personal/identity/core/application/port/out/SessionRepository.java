package com.personal.identity.core.application.port.out;

import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;

import java.util.List;
import java.util.Optional;

/**
 * <b>PORT</b> For Session persistence.
 */
public interface SessionRepository {


    Session save(Session session);

    Optional<Session> findById(String id);

    /** Lists ACTIVE sessions of a user - used for the "list devices" API. */
    List<Session> findActiveByUserId(Long userId);

    /** All sessions of a user, regardless of status - used for admin history view. */
    List<Session> findAllByUserId(Long userId);

    /**
     * Revokes all ACTIVE sessions of a user, EXCEPT the current session.
     * Used for "logout from all other devices" in the change-password flow.
     *
     * @return the number of revoked sessions
     */
    int revokeAllOtherSessions(Long userId, String currentSessionId, RevokedReason reason);

    /**
     * Revokes ALL ACTIVE sessions of a user (including the current session).
     * Used for the {@code POST /logout-all} endpoint - the user actively kicks out every device,
     * including the one sending the request. After this API, the user must log in again.
     *
     * <p><b>Unlike {@link #revokeAllOtherSessions}:</b> does not have a
     * {@code currentSessionId} parameter for exclusion. This is the core difference between
     * "change password" (keeps the current session) and "logout all" (revokes everything).
     *
     * @return the number of revoked sessions
     */
    int revokeAllByUserId(Long userId, RevokedReason reason);
}
