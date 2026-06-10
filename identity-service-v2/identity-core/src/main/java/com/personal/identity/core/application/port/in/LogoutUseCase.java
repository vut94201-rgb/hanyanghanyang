package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.token.AccessTokenBlacklist;
import com.personal.identity.core.domain.token.RefreshTokenRepository;
import com.personal.identity.core.domain.token.TokenClaims;
import com.personal.identity.core.domain.token.TokenProvider;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: Logout — invalidates the current session and associated tokens.
 *
 * <p><b>Execution Flow:</b>
 * <ol>
 * <li>Parse the access token ⟿ retrieve {@code TokenClaims} (containing sessionId, tokenId/JTI, and expiresAt).</li>
 * <li>If token verification fails ⟿ execute a no-op (token is either already expired or has an invalid signature — no further action required).</li>
 * <li>Revoke the session with the reason {@code LOGOUT} (idempotent operation — revoking an already revoked session is safe).</li>
 * <li>Revoke all refresh tokens bound to this session (bulk {@code REVOKED} transition).</li>
 * <li>Blacklist the access token with a {@code TTL = remaining time before natural token expiry}.</li>
 * </ol>
 *
 * <p><b>Idempotency:</b> Invoking the logout sequence multiple times consecutively results in a no-op on subsequent
 * calls (as the session is already {@code REVOKED} and the token JTI is already blacklisted). No exceptions will be thrown.
 *
 * <p><b>Why we silently fail when a token is invalid:</b>
 * <ul>
 * <li>Token already expired: Nothing left to do on the server side (the global JWT security filter already rejects this token
 * for any authenticated endpoints). Returns a clean {@code 200 OK}.</li>
 * <li>Invalid signature: Indicates either a client-side bug or a malicious forgery attempt — we intentionally avoid leaking system state details.
 * Returns a clean {@code 200 OK}.</li>
 * </ul>
 * Logout behaves as a "best effort" endpoint, completely decoupled from the critical path. Unlike login (which enforces rigorous, non-forgiving
 * verification barriers), logout can afford a more lenient, permissive processing policy.
 *
 * <p><b>Why the refresh token is not required for a logout request:</b> Client applications usually only retain the access token
 * within the HTTP authorization headers. Mandating the client to submit the refresh token as well yields poor UX.
 * Embedded within the access token claims is the {@code sessionId}, which provides sufficient scope to cascade the revocation across the entire
 * refresh token chain tied to that session instance.
 *
 * <p><b>Global Logout (Logout from all devices):</b> This specific use case ONLY terminates the CURRENT session.
 * "Logout from all devices" is a separate, dedicated use case (not implemented here) — which would invoke
 * {@code sessionRepository.revokeAllOtherSessions()} or tear down all active sessions including the current one.
 */
public class LogoutUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenProvider tokenProvider;
    private final SessionRepository sessionRepository;

    public LogoutUseCase(
            RefreshTokenRepository refreshTokenRepository,
            AccessTokenBlacklist accessTokenBlacklist,
            TokenProvider tokenProvider,
            SessionRepository sessionRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenBlacklist = accessTokenBlacklist;
        this.tokenProvider = tokenProvider;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Executes the logout routine. Fully idempotent, failing silently if the token is invalid.
     *
     * @param accessToken The incoming raw JWT extracted from the {@code Authorization: Bearer ...} header.
     */
    public void execute(String accessToken) {
        if (Objects.isNull(accessToken) || StringUtils.hasText(accessToken)) {
            return;
        }
        Optional<TokenClaims> claimsOptional = tokenProvider.parseAndVerify(accessToken);
        if (claimsOptional.isEmpty()) {
            // Token is invalid (expired/forged) — the server has no actionable state changes to perform.
            // DO NOT throw an exception — logout is treated as a best-effort transaction.
            return;
        }
        TokenClaims tokenClaims = claimsOptional.get();

        // 1. Revoke Session
        Optional<Session> sessionOptional = sessionRepository.findById(tokenClaims.sessionId());
        if (sessionOptional.isPresent()) {
            Session session = sessionOptional.get();
            // revoke() is idempotent — encapsulates internal state checks; if already REVOKED, it acts as a no-op.
            session.revoke(RevokedReason.LOGOUT);
            sessionRepository.save(session);
        }

        // 2. Revoke all refresh token bound to this session(cascades both ACTIVE and USED states to REVOKED)
        refreshTokenRepository.revokeAllBySessionId(tokenClaims.sessionId());

        // 3. Blacklist access token JTI. TTL is calculated as the remaining duration before natural token expiration
        //    One the access token naturally expries, Redis automatically evicts the blacklist key namespace
        addToBlacklist(tokenClaims);

    }


    private void addToBlacklist(TokenClaims tokenClaims) {


        if (Objects.isNull(tokenClaims) || Objects.isNull(tokenClaims.tokenId())) {
            // Defensive guard: Token is missing JTI claim(legacy token generated before JTI enforcement)
            // Cannot blacklist without a unique ID -> Skip silently
            return;
        }
        Duration timeToLive = Duration.between(
                Instant.now(),
                tokenClaims.expiresAt()
        );
        if (timeToLive.isNegative() || timeToLive.isZero()) {
            // Token has already expired - no need  to populate the blacklist;
            return;
        }
        accessTokenBlacklist.add(
                tokenClaims.tokenId(),
                timeToLive
        );


    }
}
