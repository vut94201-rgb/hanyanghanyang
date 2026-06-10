package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.AuthResult;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.application.port.out.TokenHasher;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.application.security.PasswordEncoder;
import com.personal.identity.core.application.security.SecureRandomGenerator;
import com.personal.identity.core.domain.permission.Role;
import com.personal.identity.core.domain.session.*;
import com.personal.identity.core.domain.token.*;
import com.personal.identity.core.domain.user.InvalidCredentialsException;
import com.personal.identity.core.domain.user.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: Login using username and password.
 *
 * <p><b>Execution Flow:</b>
 * <ol>
 * <li>Lookup user by username.</li>
 * <li>Verify password - utilizing a dummy hash to mitigate timing attacks when the user does not exist (see details below).</li>
 * <li>Check {@code canLogin()}: Ensure the user is ACTIVE and has not been soft-deleted.</li>
 * <li>Parse User-Agent into a {@code DeviceInfo} object.</li>
 * <li>Resolve IP address into a {@code GeoLocation} object.</li>
 * <li>Build and persist an ACTIVE Session with a new UUID.</li>
 * <li>Generate a raw refresh token (32 random bytes), hash it using SHA-256, and persist it as an ACTIVE RefreshToken.</li>
 * <li>Build TokenClaims using the user's roles and permissions, then sign the JWT access token.</li>
 * <li>Return the {@link AuthResult} containing the access JWT, raw refresh token, sessionId, and expiration time.</li>
 * </ol>
 *
 * <p><b>Security: Mitigating user enumeration via timing attacks.</b> If the user does not
 * exist, we STILL verify the password against a dummy hash. BCrypt consumes ~100ms regardless of
 * whether the verification succeeds or fails. If we bypassed verification for non-existent users:
 * <ul>
 * <li>Incorrect username → response takes ~5ms.</li>
 * <li>Correct username, incorrect password → response takes ~105ms.</li>
 * </ul>
 * An attacker could measure this response time discrepancy to enumerate valid usernames.
 * The dummy hash verification ensures the processing time remains nearly identical across both scenarios.
 *
 * <p><b>The dummy hash is a valid BCrypt hash of a meaningless string</b> ("dummy_password_for_timing_attack"),
 * pre-computed with strength=10. The {@code matches()} method will always evaluate to false (since the
 * raw password provided will never match this exact string), but it will still consume the same ~100ms
 * as a legitimate verification.
 *
 * <p><b>Session TTL matches the refresh token TTL.</b> When the refresh token expires, the session
 * should also expire — they share the same lifecycle. Hard-coded to 30 days here for simplicity;
 * if configuration is needed, it can be injected via the constructor (though this increases coupling).
 */
public class LoginUseCase {

    /**
     * BCrypt hash of a meaningless password, pre-computed with strength=10.
     * Used to perform verification when the user does not exist → consuming the exact same time as a real verification.
     * WILL NEVER match any actual user's password.
     *
     * <p>Generated via: {@code new BCryptPasswordEncoder(10).encode("dummy_password_for_timing_attack")}
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /**
     * Session TTL = refresh token TTL. Hard-coded to 30 days to align with the
     * default {@code app.jwt.refresh-token-ttl-days} property.
     */
    private static final Duration SESSION_TTL = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandomGenerator secureRandomGenerator;
    private final UserAgentParser userAgentParser;
    private final GeoLocationResolver geoLocationResolver;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    public LoginUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SecureRandomGenerator secureRandomGenerator,
            UserAgentParser userAgentParser,
            GeoLocationResolver geoLocationResolver,
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository,
            TokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandomGenerator = secureRandomGenerator;
        this.userAgentParser = userAgentParser;
        this.geoLocationResolver = geoLocationResolver;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    public AuthResult execute(LoginCommand command) {
        validateInput(command);

        // 1+2. Lookup user and verify password in near-constant time.
        User user = authenticate(
                command.username(),
                command.rawPassword()
        );

        // 3. Verify user status is viable for login.
        if (!user.canLogin()) {
            // Return the EXACT SAME exception as "wrong password" to prevent leaking
            // the account status (e.g., locked or disabled).
            throw new InvalidCredentialsException();
        }

        // 4-5. Parse User-Agent and resolve geolocation. Both implement graceful fallback —
        //      no exceptions are thrown; in the worst case, they return unknown/empty representations.
        DeviceInfo device = userAgentParser.parse(command.context().rawUserAgent());
        GeoLocation geo = geoLocationResolver.resolve(command.context().ipAddress());

        // 6. Create and persist the session.
        Session session = createSession(
                user,
                command.context(),
                device,
                geo
        );
        Session savedSession = sessionRepository.save(session);

        // 7. Generate raw refresh token, hash it, and persist.
        String rawRefreshToken = secureRandomGenerator.generateToken();
        RefreshToken refreshToken = createRefreshToken(
                savedSession.getId(),
                rawRefreshToken
        );
        refreshTokenRepository.save(refreshToken);

        // 8. Generate access JWT.
        String accessToken = generateAccessToken(
                user,
                savedSession.getId()
        );

        // 9. Return the result. For accessTokenExpiresAt: parsing it directly from the token
        //    is the most accurate, but calculating it from 'now + TTL' is simpler. We use
        //    the second approach here to avoid re-parsing the newly signed JWT.
        Instant accessExpiresAt = Instant.now().plus(Duration.ofMinutes(15));
        return new AuthResult(
                accessToken,
                rawRefreshToken,
                accessExpiresAt,
                savedSession.getId()
        );
    }

    /**
     * Authenticates the user. Consumes ~100ms regardless of whether the user exists or not
     * (mitigating timing attacks).
     */
    private User authenticate(
            String username,
            String rawPassword
    ) {
        User user = userRepository.findByUsername(username).orElse(null);

        // Verify the password REGARDLESS of user existence.
        // - User exists: verify against their actual hash → evaluates to true or false.
        // - User does not exist: verify against the dummy hash → always evaluates to false,
        //   but consumes the same computation time.
        String hashToVerify = (user != null) ? user.getPasswordHash() : DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(
                rawPassword,
                hashToVerify
        );

        if (user == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    private Session createSession(
            User user,
            RequestContext context,
            DeviceInfo device,
            GeoLocation geo
    ) {
        Instant now = Instant.now();
        return Session.createNew(
                UUID.randomUUID().toString(),
                user.getId(),
                device,
                geo,
                context.ipAddress(),
                context.rawUserAgent(),
                now.plus(SESSION_TTL)

        );

    }

    private RefreshToken createRefreshToken(
            String sessionId,
            String rawToken
    ) {
        Instant now = Instant.now();
        return RefreshToken.createNew(
                UUID.randomUUID().toString(),
                sessionId,
                TokenHasher.hash(rawToken),
                now.plus(SESSION_TTL)
        );
    }

    private String generateAccessToken(
            User user,
            String sessionId
    ) {
        // Effective permissions = direct permissions + (permissions derived from roles).
        // The User entity already provides a helper method for this.
        Set<String> permissionCodes = user.getEffectivePermissionCodes();

        // Extract role codes directly from the user's roles.
        Set<String> roleCodes = user.getRoles().stream().map(Role::getRoleCode).collect(Collectors.toSet());

        TokenClaims claims = new TokenClaims(
                null,  // tokenId - the adapter auto-generates the JTI
                user.getId(),
                sessionId,
                roleCodes,
                permissionCodes,
                null,  // TokenProvider automatically sets the 'iat' (issued at) claim
                null   // TokenProvider automatically sets the 'exp' (expiration) claim
        );

        return tokenProvider.generateAccessToken(claims);
    }

    private void validateInput(LoginCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("LoginCommand must not be null");
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }
        if (command.context() == null) {
            throw new IllegalArgumentException("context must not be null");
        }
    }

    /**
     * Input command for {@link #execute(LoginCommand)}.
     *
     * @param username    Username, already trimmed and lowercased at the DTO layer.
     * @param rawPassword Plain text password, to be verified using BCrypt.
     * @param context     IP address and User-Agent extracted from the HTTP request.
     */
    public record LoginCommand(
            String username,
            String rawPassword,
            RequestContext context
    ) {
    }
}
