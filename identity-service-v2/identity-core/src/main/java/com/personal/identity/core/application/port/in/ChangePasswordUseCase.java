package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.application.security.PasswordEncoder;
import com.personal.identity.core.domain.token.RefreshTokenRepository;
import com.personal.identity.core.domain.user.InvalidCredentialsException;
import com.personal.identity.core.domain.user.PasswordSameAsCurrentException;
import com.personal.identity.core.domain.user.UserNotFoundException;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Use case: User changes their password.
 *
 * <p><b>Execution Flow:</b>
 * <ol>
 * <li>Load the user by ID (already authenticated via the filter; userId is present in the context).</li>
 * <li>Verify the current password — if incorrect, throw an {@link InvalidCredentialsException}.</li>
 * <li>Check that the new password differs from the current one (preventing redundant operations).</li>
 * <li>Hash the new password, invoke {@code user.changePassword(newHash)}, and persist.</li>
 * <li>Revoke ALL OTHER sessions belonging to the user (excluding the current session) — forcing a re-login
 * on all other devices. This is a security best practice.</li>
 * </ol>
 *
 * <h2>Why revoke other sessions?</h2>
 *
 * <p>A password change is typically a reaction to one of two scenarios:
 * <ol>
 * <li><b>Routine update:</b> The user decides "I've used this password for 6 months, time to change it for safety."
 * No signs of compromise.</li>
 * <li><b>Anomaly detected:</b> The user notices an unfamiliar session or login from an unknown device ⟿
 * changes the password to sever the attacker's access.</li>
 * </ol>
 *
 * <p>Scenario (2) is critical. If we only change the password and DO NOT revoke active sessions:
 * The attacker retains an ACTIVE session ⟿ they can continue utilizing their access token and refresh token freely.
 * Changing the password only prevents SUBSEQUENT logins; it does not disrupt an ongoing session.
 * <b>Revoking other sessions is the ONLY way to terminate active, ongoing access.</b>
 *
 * <p>This pattern is an industry standard: Google, GitHub, and Facebook all force re-authentication on
 * other devices following a password change.
 *
 * <p><b>Why PRESERVE the current session?</b> The user is currently operating on the web interface to change
 * their password — once changed, there is no logical reason to force them to re-login on the very tab
 * they are actively using. That would result in poor UX.
 *
 * <p><b>Why we do NOT blacklist access tokens of revoked sessions here:</b> Access tokens possess a short
 * TTL (e.g., 15 minutes). Once the session is marked as REVOKED ⟿ the global security filter will verify the session
 * status and instantly reject any requests originating from a REVOKED session, even if the access token itself
 * remains cryptographically valid. In the worst-case scenario (within 15 minutes), these stale access tokens
 * will naturally expire.
 */
public class ChangePasswordUseCase {


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public ChangePasswordUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    public void execute(ChangePasswordCommand command) {
        validateInput(command);

        // 1. Load User
        var currentUser = userRepository.findById(command.userId()).orElseThrow(() -> UserNotFoundException.byId(command.userId()));

        // 2. Verify current password
        //    DO NOT apply the dummy hash  timing-attack mitigation trick here because  the user is already authenticated
        //    (processing a valid JWT) - User enumeration is not concern here. An incorrect password is simply an incorrect password
        if (!passwordEncoder.matches(
                command.currentPassword,
                currentUser.getPasswordHash()
        )) {
            throw new InvalidCredentialsException();
        }

        // 3. Ensure  the new password  differs  from the current one. This is not strictly mandatory(BCrypt will
        //    generate a distinct  hash anyway due to a different salt), but it prevents the users from accidentally re-summiting
        //    their old password
        if (passwordEncoder.matches(
                command.newPassword,
                currentUser.getPasswordHash()
        )) {
            throw new PasswordSameAsCurrentException();
        }
    }

    private void validateInput(ChangePasswordCommand changePasswordCommand) {
        if (Objects.isNull(changePasswordCommand)) {
            throw new IllegalArgumentException("changePasswordCommand must  not be null");
        }
        if (Objects.isNull(changePasswordCommand.userId)) {
            throw new IllegalArgumentException("userId must  not be null");
        }
        if (Objects.isNull(changePasswordCommand.currentSessionId) || StringUtils.hasText(changePasswordCommand.currentSessionId)) {
            throw new IllegalArgumentException("currentSessionId must  not be null");
        }
        if (Objects.isNull(changePasswordCommand.currentPassword) || StringUtils.hasText(changePasswordCommand.currentPassword)) {
            throw new IllegalArgumentException("currentPassword must  not be null");
        }
        if (Objects.isNull(changePasswordCommand.newPassword) || StringUtils.hasText(changePasswordCommand.newPassword)) {
            throw new IllegalArgumentException("newPassword must  not be null");
        }
    }

    /**
     * Input command for {@link #execute(ChangePasswordCommand)}.
     *
     * @param userId           The user ID (extracted from the JWT claims in the filter).
     * @param currentSessionId The current session ID (extracted from the JWT claims) - this specific session
     *                         will be PRESERVED, while all other sessions are revoked.
     * @param currentPassword  The current password, used to verify account ownership.
     * @param newPassword      The new password (password strength validation is handled at the DTO layer).
     */
    public record ChangePasswordCommand(
            Long userId,
            String currentSessionId,
            String currentPassword,
            String newPassword
    ) {
    }
}
