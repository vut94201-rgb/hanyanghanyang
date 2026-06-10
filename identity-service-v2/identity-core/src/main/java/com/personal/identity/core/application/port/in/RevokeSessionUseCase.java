package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.session.SessionAccessDeniedException;
import com.personal.identity.core.domain.session.SessionNotFoundException;
import com.personal.identity.core.domain.token.RefreshTokenRepository;

/**
 * Use case: A user explicitly revokes a specific session belonging to themselves (via a targeted endpoint).
 * <p>{@code DELETE /api/v1/auth/sessions/{id}}.
 *
 * <p><b>Target Scenario:</b> A user navigates to their "Devices signed in" profile page (similar to Google or GitHub)
 * and clicks "Sign out" to terminate access for a single, specific device.
 *
 * <p><b>Critical Dependency - Ownership Check:</b> We must strictly guarantee that User A cannot revoke a session
 * that belongs to User B. The system compares the {@code session.getUserId()} of the requested resource against the authenticated
 * {@code userId} extracted from the incoming JWT. If a mismatch is detected, the use case throws a
 * {@link SessionAccessDeniedException} (yielding an HTTP 403 Forbidden status).
 *
 * <p><b>Architectural Rationale - Why we do NOT employ {@code LogoutUseCase} or blacklisting here:</b>
 * {@code LogoutUseCase} accepts the raw JWT access token of the current active request to parse its JTI and append it to
 * the Redis blacklist. In this scenario, however, the server does not possess the access token of the targeted session
 * being revoked (we only know its generic {@code sessionId}). Consequently, the JTI of that external session's access token
 * cannot be added to the blacklist.
 * <p>Instead, we rely on the global security filter executing on every incoming request: it interceptively cross-references
 * the session identifier against the database to confirm it remains ACTIVE. If the session has been flipped to {@code REVOKED},
 * any subsequent API calls bound to that token will immediately fail with a 403 response. Real-time active logout remains fully functional.
 * <p>The minor trade-off is accepting a slight database/cache verification load instead of a pure Redis blacklist check.
 * Once the short-lived access token of that remote session reaches its natural 15-minute expiration window (token TTL),
 * it drops off entirely.
 */
public class RevokeSessionUseCase {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RevokeSessionUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revokes a single session by its unique ID, enforcing an identity ownership check against the executing user.
     *
     * @param targetSessionId  The unique string identifier of the session targeted for destruction.
     * @param requestingUserId The authenticated user ID extracted from the current active JWT context.
     * @throws SessionNotFoundException     if the targeted sessionId does not exist in the database.
     * @throws SessionAccessDeniedException if the target session does not belong to the requesting user.
     */
    public void execute(
            String targetSessionId,
            Long requestingUserId
    ) {
        Session session = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> SessionNotFoundException.sessionNotFoundById(targetSessionId));

        // Ownership check — highly critical constraint to block IDOR (Insecure Direct Object Reference) vectors.
        if (!session.getUserId().equals(requestingUserId)) {
            throw new SessionAccessDeniedException(targetSessionId);
        }

        // Idempotency guarantee: if the state is already flipped to REVOKED, execute() operates as a no-op without throwing exceptions.
        // We proactively revoke all associated refresh tokens to mitigate potential inconsistent state anomalies.
        session.revoke(RevokedReason.USER_ACTION);
        sessionRepository.save(session);

        refreshTokenRepository.revokeAllBySessionId(targetSessionId);
    }
}
