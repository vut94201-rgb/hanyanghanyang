package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.token.RefreshTokenRepository;

import java.util.List;

/**
 * Use case: Revoke ALL active sessions belonging to a user (including the current session instance).
 *
 * <p><b>How this differs from {@code ChangePasswordUseCase}:</b>
 * <ul>
 * <li>{@code ChangePasswordUseCase} revokes OTHER sessions while PRESERVING the current session
 * (delivering a smooth UX — ensuring the user is not abruptly kicked out immediately after updating their password).</li>
 * <li>{@code LogoutAllUseCase} forcefully terminates ALL sessions across the board (the intent is to
 * "kick every device out" — typically triggered when a user suspects their account has been compromised).</li>
 * </ul>
 *
 * <p><b>Target Scenario:</b> A user loses their laptop or detects unauthorized account activity,
 * demands an immediate blanket logout across all devices, and subsequently updates their credentials.
 *
 * <p><b>Why we load session entities instead of executing a single bulk SQL UPDATE query:</b>
 * <ul>
 * <li>To invoke the {@code session.revoke(RevokedReason)} domain method for every individual instance.</li>
 * <li>Executing a direct native bulk SQL UPDATE (via {@code sessionRepository.revokeAllByUserId})
 * bypasses core domain model behavior and side effects. For example, if future requirements mandate
 * publishing a "session revoked" domain event, a direct database-level bulk query would completely skip event publishing.</li>
 * </ul>
 *
 * <p><b>Performance Trade-off Analysis:</b> At the current scope, {@code revoke()} solely modifies
 * an in-memory field state without triggering immediate infrastructure side effects; hence, loading a managed bulk update
 * is perfectly acceptable. Assuming an average user maintains ~10 concurrent active sessions,
 * loading 10 lightweight entities into memory followed by 10 fast update statements incurs minimal latency.
 * <i>Core Architectural Axiom:</i> Prioritize domain model integrity over micro-optimizations. If a high-scale
 * performance bottleneck surfaces later due to extensive event publishing overhead, this can be refactored.
 */
public class LogoutAllUseCase {


    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutAllUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revokes all active sessions belonging to the specified user.
     *
     * @param userId The ID of the target user.
     * @return The total count of sessions successfully invalidated (allowing the frontend to display
     * a responsive notification toast like "Successfully logged out of N devices").
     */
    public int execute(Long userId) {

        // 1. Fetch the collection of ACTIVE session PRIOR to executing the database-side bulk revocation
        //      (required to track their IDs for cascading   the refresh token   invalidation afterward)
        List<Session> activeSessions = sessionRepository.findActiveByUserId(userId);

        // 2. Execute  a database-level bulk via a single optimized query
        int revokeCount = sessionRepository.revokeAllByUserId(
                userId,
                RevokedReason.USER_ACTION
        );
        // 3. Cascade the invalidation to revoke the refresh token chain of each respective session.
        //    Note: We iterate via a loop here because the RefreshTokenRepository currently lacks
        //    a dedicated "revokeByUserId" batch mechanism. This layer can be optimized
        //    subsequently by introducing a native port method.
        for (Session session : activeSessions) {
            refreshTokenRepository.revokeAllBySessionId(session.getId());
        }
        return revokeCount;
    }

}
