package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.AuthResult;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.application.port.out.TokenHasher;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.application.security.SecureRandomGenerator;
import com.personal.identity.core.domain.permission.Role;
import com.personal.identity.core.domain.session.RequestContext;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.session.SessionNotFoundException;
import com.personal.identity.core.domain.token.*;
import com.personal.identity.core.domain.user.User;
import com.personal.identity.core.domain.user.UserNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: Rotate the refresh token ⟿ issue a new access token and a new refresh token.
 *
 * <p><b>Standard Execution Flow:</b>
 * <ul>
 * <li>Step 1: Hash the raw refresh token submitted by the client ⟿ perform a lookup by hash.</li>
 * <li>Step 2: If not found ⟿ throw {@link InvalidRefreshTokenException}.</li>
 * <li>Step 3: Evaluate token status. For {@link RefreshTokenStatus#USED} ⟿ REUSE DETECTED (revoke the entire family and session, then throw {@link TokenReuseDetectedException}). For {@link RefreshTokenStatus#REVOKED} or Expired ⟿ throw {@link InvalidRefreshTokenException}. For {@link RefreshTokenStatus#ACTIVE} ⟿ proceed.</li>
 * <li>Step 4: Load the session and verify it remains ACTIVE ⟿ if not, revoke the token and throw an exception.</li>
 * <li>Step 5: Load the user and verify {@code canLogin()} ⟿ if not, revoke the session/token and throw an exception.</li>
 * <li>Step 6: Generate a new refresh token (raw + hash) and persist it with an ACTIVE status.</li>
 * <li>Step 7: Mark the previous token as USED, setting {@code replacedByTokenId} to point to the newly generated token.</li>
 * <li>Step 8: Update the session's {@code lastActiveAt} timestamp.</li>
 * <li>Step 9: Generate a new JWT access token utilizing the user's current claims (accommodating potential role/permission modifications).</li>
 * <li>Step 10: Return the {@link AuthResult}.</li>
 * </ul>
 *
 * <h2>Reuse Detection - Why It Is Critical</h2>
 *
 * <p>Scenario: An attacker steals a user's refresh token (e.g., via XSS or malware). Pattern:
 * <pre>
 * t=0: User logs in ⟿ token A is issued (ACTIVE).
 * t=1: Attacker steals token A.
 * t=2: User performs a refresh ⟿ token A transitions to USED, issuing token B (ACTIVE). User now holds B.
 * t=3: Attacker attempts to use token A (which is already USED!) ⟿ the system DETECTS this.
 * ⟿ The system revokes the entire token family (A, B, and all other tokens within the session).
 * ⟿ The legitimate user is forced to re-login, but the attacker loses access permanently.
 * </pre>
 *
 * <p>Core concept: The token chain operates as a linked list via {@code replacedByTokenId}.
 * Proper usage dictates that each token is rotated exactly once. If any token is utilized twice
 * (status = USED but submitted again), it implies that one of the two parties (the legitimate user
 * or the attacker) is employing a compromised, rotated token ⟿ indicating a hijacked state.
 *
 * <p>The system does not need to distinguish between the legitimate user and the attacker. Both are forced to re-authenticate.
 * The legitimate user suffers a minor inconvenience (one extra login), whereas the attacker loses their unauthorized access permanently.
 */
public class RefreshTokenUseCase {

    private static final Duration SESSION_TTL = Duration.ofDays(30);

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SecureRandomGenerator secureRandomGenerator;
    private final TokenProvider tokenProvider;

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            SessionRepository sessionRepository,
            UserRepository userRepository,
            SecureRandomGenerator secureRandomGenerator,
            TokenProvider tokenProvider
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.secureRandomGenerator = secureRandomGenerator;
        this.tokenProvider = tokenProvider;
    }


    public AuthResult execute(RefreshCommand command) {
        validateInput(command);

        // 1. Hash the raw token and perform a lookup
        String tokenHash = TokenHasher.hash(command.rawRefreshToken());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Token not found"));

        // 2. Evaluate token status — reuse detection triggers here
        checkTokenStatus(currentToken);

        // 3. Load and validate the associated session
        Session session = sessionRepository.findById(currentToken.getSessionId())
                .orElseThrow(() -> SessionNotFoundException.sessionNotFoundById(currentToken.getSessionId()));

        if (!session.isActive()) {
            // The session has been revoked or expired, but the token remains ACTIVE (a rare race condition).
            // Revoke the token to synchronize states.
            currentToken.revoke();
            refreshTokenRepository.save(currentToken);
            throw new InvalidRefreshTokenException("Session is not active");
        }

        // 4. Load and validate the user
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> UserNotFoundException.byId(session.getUserId()));

        if (!user.canLogin()) {
            // The user was disabled, locked, or soft-deleted after their initial login. Revoke both the session and the token chain.
            session.revoke(RevokedReason.USER_ACTION);
            sessionRepository.save(session);
            refreshTokenRepository.revokeAllBySessionId(session.getId());
            throw new InvalidRefreshTokenException("User cannot login");
        }

        // 5. Generate a new refresh token
        String newRawToken = secureRandomGenerator.generateToken();
        RefreshToken newToken = createNewRefreshToken(
                session.getId(),
                newRawToken
        );
        RefreshToken savedNew = refreshTokenRepository.save(newToken);

        // 6. Mark the previous token as USED and establish a link to the new token
        currentToken.markUsed(
                savedNew.getId(),
                command.context().ipAddress()
        );
        refreshTokenRepository.save(currentToken);

        // 7. Update the session's lastActiveAt timestamp
        session.touch();
        sessionRepository.save(session);

        // 8. Generate a new JWT utilizing the user's current claims (roles/permissions might have mutated)
        String accessToken = generateAccessToken(
                user,
                session.getId()
        );

        // 9. Return the payload result
        Instant accessExpiresAt = Instant.now().plus(Duration.ofMinutes(15));
        return new AuthResult(
                accessToken,
                newRawToken,
                accessExpiresAt,
                session.getId()
        );
    }

    /**
     * Evaluates the token's status. This serves as the core engine for reuse detection.
     */
    private void checkTokenStatus(RefreshToken token) {
        RefreshTokenStatus status = token.getTokenStatus();

        if (status == RefreshTokenStatus.USED) {
            // !!! REUSE DETECTED !!!
            // The token was previously rotated to yield a new token, yet it is being submitted again ⟿ indicating a theft.
            // Action: Revoke the entire token family and the associated session with the reason TOKEN_REUSE.
            handleReuseDetection(token);
            throw new TokenReuseDetectedException(token.getSessionId());
        }

        if (status == RefreshTokenStatus.REVOKED) {
            throw new InvalidRefreshTokenException("Token has been revoked");
        }

        // Token is ACTIVE but might have expired (expiresAt < now)
        if (!token.isActive()) {
            throw new InvalidRefreshTokenException("Token has expired");
        }
    }

    /**
     * When a reuse is detected, perform a rigorous cleanup:
     * Revoke the session with the reason TOKEN_REUSE (for the audit trail).
     * Revoke all refresh tokens bound to the session (transitioning both ACTIVE and USED tokens to REVOKED).
     * The system does not differentiate between the legitimate user and the attacker; both must re-authenticate.
     */
    private void handleReuseDetection(RefreshToken reuseToken) {
        String sessionId = reuseToken.getSessionId();

        // Revoke the session - locate the session and mark it as REVOKED with the reason TOKEN_REUSE
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(RevokedReason.TOKEN_REUSE);
            sessionRepository.save(session);
        });

        // Revoke all refresh tokens tied to the session (bulk operation)
        refreshTokenRepository.revokeAllBySessionId(sessionId);
    }

    private RefreshToken createNewRefreshToken(
            String sessionId,
            String rawToken
    ) {
        Instant now = Instant.now();

        return RefreshToken.createNew(UUID.randomUUID().toString(), sessionId, TokenHasher.hash(rawToken), now.plus(SESSION_TTL));
    }

    private String generateAccessToken(
            User user,
            String sessionId
    ) {
        // Effective permissions = direct permissions + (derived from roles). The User entity provides a helper for this.
        Set<String> permissionCodes = user.getEffectivePermissionCodes();
        // Extract role codes directly from user.roles
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        TokenClaims claims = new TokenClaims(
                null,  // tokenId - the adapter auto-generates the JTI
                user.getId(),
                sessionId,
                roleCodes,
                permissionCodes,
                null,  // TokenProvider automatically sets 'iat'
                null   // TokenProvider automatically sets 'exp'
        );
        return tokenProvider.generateAccessToken(claims);
    }

    private void validateInput(RefreshCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("RefreshCommand must not be null");
        }
        if (command.rawRefreshToken() == null || command.rawRefreshToken().isBlank()) {
            throw new IllegalArgumentException("rawRefreshToken must not be blank");
        }
        if (command.context() == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    /**
     * Input payload for {@link #execute(RefreshCommand)}.
     *
     * @param rawRefreshToken The raw refresh token from the client (Base64 URL-safe, 43 characters).
     * @param context         The IP address and User-Agent of the request (persisted into {@code usedFromIp} for the audit trail).
     */
    public record RefreshCommand(
            String rawRefreshToken,
            RequestContext context
    ) {
    }
}
