package com.personal.identity.core.service;

import com.personal.identity.core.role.Role;
import com.personal.identity.core.security.PasswordEncoder;
import com.personal.identity.core.security.SecureRandomGenerator;
import com.personal.identity.core.session.*;
import com.personal.identity.core.token.*;
import com.personal.identity.core.user.InvalidCredentialsException;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: đăng nhập bằng username + password.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Lookup user theo username</li>
 *   <li>Verify password - dùng dummy hash để chống timing attack khi user
 *       không tồn tại (xem chi tiết bên dưới)</li>
 *   <li>Check {@code canLogin()}: ACTIVE + chưa soft-delete</li>
 *   <li>Parse User-Agent → DeviceInfo</li>
 *   <li>Resolve IP → GeoLocation</li>
 *   <li>Build Session ACTIVE với UUID, save</li>
 *   <li>Sinh raw refresh token (32 byte random) - hash SHA-256 - save RefreshToken ACTIVE</li>
 *   <li>Build TokenClaims từ user roles + permissions, sign JWT access token</li>
 *   <li>Trả {@link AuthResult}: access JWT + raw refresh + sessionId + expiresAt</li>
 * </ol>
 *
 * <p><b>Security: chống user enumeration qua timing attack.</b> Nếu user không
 * tồn tại, ta CŨNG verify password với 1 hash dummy. BCrypt mất ~100ms cho dù
 * verify thành công hay thất bại. Nếu skip verify khi user không có:
 * <ul>
 *   <li>Username sai → response ~5ms</li>
 *   <li>Username đúng, password sai → response ~105ms</li>
 * </ul>
 * Attacker đo response time để enumerate username hợp lệ. Dummy hash verify giữ
 * thời gian xử lý gần như nhau cho cả 2 case.
 *
 * <p><b>Dummy hash là hash thật của 1 string vô nghĩa</b> ("dummy_password_for_timing_attack"),
 * pre-compute với BCrypt strength=10. {@code matches()} sẽ luôn false (vì raw password
 * không bao giờ là string này) nhưng vẫn tốn 100ms như verify thật.
 *
 * <p><b>Session TTL = refresh token TTL.</b> Khi refresh token expire, session
 * cũng nên expire - cùng lifecycle. Hard-code 30 ngày ở đây cho đơn giản; nếu
 * cần config, thêm vào constructor (nhưng coupling sẽ rộng hơn).
 */
public class LoginUseCase {

    /**
     * BCrypt hash của 1 password vô nghĩa, pre-compute với strength=10.
     * Dùng để verify khi user không tồn tại → tốn cùng thời gian như verify thật.
     * KHÔNG bao giờ trùng với bất kỳ password user thật nào.
     *
     * <p>Sinh bằng: {@code new BCryptPasswordEncoder(10).encode("dummy_password_for_timing_attack")}
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    /**
     * Session TTL = refresh token TTL. Hard-code 30 ngày để khớp với
     * {@code app.jwt.refresh-token-ttl-days} mặc định.
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

        // 1+2. Lookup user + verify password với constant time
        User user = authenticate(command.username(), command.rawPassword());

        // 3. Check user trạng thái OK để login
        if (!user.canLogin()) {
            // Trả CÙNG exception với "wrong password" để không leak status
            // (account locked / disabled).
            throw new InvalidCredentialsException();
        }

        // 4-5. Parse UA + resolve geo. Cả 2 đều graceful fallback - không throw,
        //      worst case trả unknown/empty.
        DeviceInfo device = userAgentParser.parse(command.context().rawUserAgent());
        GeoLocation geo = geoLocationResolver.resolve(command.context().ipAddress());

        // 6. Tạo session
        Session session = createSession(user, command.context(), device, geo);
        Session savedSession = sessionRepository.save(session);

        // 7. Sinh raw refresh token, hash, save
        String rawRefreshToken = secureRandomGenerator.generateToken();
        RefreshToken refreshToken = createRefreshToken(savedSession.getId(), rawRefreshToken);
        refreshTokenRepository.save(refreshToken);

        // 8. Sinh access JWT
        String accessToken = generateAccessToken(user, savedSession.getId());

        // 9. Trả result. accessTokenExpiresAt: parse từ token cho chính xác,
        //    nhưng đơn giản hơn là tính lại từ giờ + TTL. Ở đây dùng cách thứ 2
        //    để khỏi parse lại JWT vừa sign.
        Instant accessExpiresAt = Instant.now().plus(Duration.ofMinutes(15));
        return new AuthResult(accessToken, rawRefreshToken, accessExpiresAt, savedSession.getId());
    }

    /**
     * Authenticate user. Luôn tốn ~100ms dù user có tồn tại hay không
     * (chống timing attack).
     */
    private User authenticate(String username, String rawPassword) {
        User user = userRepository.findByUsername(username).orElse(null);

        // Verify password DÙ user có tồn tại hay không.
        // - User tồn tại: verify với hash thật → có thể đúng hoặc sai
        // - User không tồn tại: verify với dummy hash → luôn sai, nhưng tốn cùng thời gian
        String hashToVerify = (user != null) ? user.getPasswordHash() : DUMMY_PASSWORD_HASH;
        boolean passwordMatches = passwordEncoder.matches(rawPassword, hashToVerify);

        if (user == null || !passwordMatches) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    private Session createSession(User user, RequestContext context, DeviceInfo device, GeoLocation geo) {
        Instant now = Instant.now();
        return Session.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .deviceInfo(device)
                .location(geo)
                .ipAddress(context.ipAddress())
                .userAgent(context.rawUserAgent())
                .sessionStatus(SessionStatus.ACTIVE)
                .createdAt(now)
                .lastActiveAt(now)
                .expiresAt(now.plus(SESSION_TTL))
                .build();
    }

    private RefreshToken createRefreshToken(String sessionId, String rawToken) {
        Instant now = Instant.now();
        return RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .tokenHash(TokenHasher.hash(rawToken))
                .tokenStatus(RefreshTokenStatus.ACTIVE)
                .createdAt(now)
                .expiresAt(now.plus(SESSION_TTL))
                .build();
    }

    private String generateAccessToken(User user, String sessionId) {
        // Effective permissions = direct + (qua roles). User entity đã có helper.
        Set<String> permissionCodes = user.getEffectivePermissionCodes();

        // Role codes lấy trực tiếp từ user.roles
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        TokenClaims claims = new TokenClaims(
                null,  // tokenId - adapter tự sinh JTI
                user.getId(),
                sessionId,
                roleCodes,
                permissionCodes,
                null,  // TokenProvider tự set iat
                null   // TokenProvider tự set exp
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
     * Input cho {@link #execute(LoginCommand)}.
     *
     * @param username     Username, đã trim/lowercase ở DTO layer
     * @param rawPassword  Plain password, sẽ verify bằng BCrypt
     * @param context      IP + User-Agent từ HTTP request
     */
    public record LoginCommand(
            String username,
            String rawPassword,
            RequestContext context
    ) {
    }
}