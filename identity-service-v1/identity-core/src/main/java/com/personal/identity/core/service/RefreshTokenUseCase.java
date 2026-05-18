package com.personal.identity.core.service;


import com.personal.identity.core.role.Role;
import com.personal.identity.core.security.SecureRandomGenerator;
import com.personal.identity.core.session.*;
import com.personal.identity.core.token.*;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserNotFoundException;
import com.personal.identity.core.user.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: rotate refresh token → cấp access token mới + refresh token mới.
 *
 * <p><b>Flow bình thường:</b>
 * <ol>
 *   <li>Hash raw refresh token client gửi → lookup theo hash</li>
 *   <li>Nếu không tìm thấy → {@link InvalidRefreshTokenException}</li>
 *   <li>Check status:
 *     <ul>
 *       <li>{@link RefreshTokenStatus#USED} → <b>REUSE DETECTED</b>: revoke cả family + session
 *           → throw {@link TokenReuseDetectedException}</li>
 *       <li>{@link RefreshTokenStatus#REVOKED} → throw {@link InvalidRefreshTokenException}</li>
 *       <li>Expired → throw {@link InvalidRefreshTokenException}</li>
 *       <li>{@link RefreshTokenStatus#ACTIVE} → đi tiếp</li>
 *     </ul>
 *   </li>
 *   <li>Load session, check còn ACTIVE → nếu không → revoke token + throw Invalid</li>
 *   <li>Load user, check {@code canLogin()} → nếu không → revoke + throw Invalid</li>
 *   <li>Sinh refresh token mới (raw + hash), save với status ACTIVE</li>
 *   <li>Mark token cũ là USED, set {@code replacedByTokenId} trỏ tới token mới</li>
 *   <li>Cập nhật {@code session.lastActiveAt}</li>
 *   <li>Sinh JWT access token mới với claims hiện tại của user (có thể đã đổi role/permission)</li>
 *   <li>Trả {@link AuthResult}</li>
 * </ol>
 *
 * <h2>Reuse Detection - vì sao quan trọng</h2>
 *
 * <p>Scenario: attacker steal được refresh token của user (vd: XSS, malware). Pattern:
 * <pre>
 * t=0:  User login → token A (ACTIVE)
 * t=1:  Attacker steal token A
 * t=2:  User refresh → A trở thành USED, sinh token B (ACTIVE). User có B.
 * t=3:  Attacker dùng token A (đã USED!) → ta DETECT
 *       → revoke cả family (A, B, mọi token khác của session)
 *       → user phải login lại nhưng attacker không vào được nữa
 * </pre>
 *
 * <p>Cốt lõi: token chain là <b>linked list</b> qua {@code replacedByTokenId}.
 * Dùng đúng cách = mỗi token chỉ rotate 1 lần duy nhất. Bất kỳ token nào bị dùng
 * 2 lần (status = USED nhưng được gửi lại) → 1 trong 2 phía (user thật hoặc
 * attacker) đang dùng token đã rotate → có thằng "lậu".
 *
 * <p>Không cần phân biệt ai là user thật, ai là attacker. Cả 2 đều phải re-login.
 * User thật chỉ mất phiền 1 lần, attacker mất truy cập vĩnh viễn.
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

        // 1. Hash raw → lookup
        String tokenHash = TokenHasher.hash(command.rawRefreshToken());
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Token not found"));

        // 2. Kiểm tra trạng thái - reuse detection ở đây
        checkTokenStatus(currentToken);

        // 3. Load + check session
        Session session = sessionRepository.findById(currentToken.getSessionId())
                .orElseThrow(() -> new SessionNotFoundException(currentToken.getSessionId()));

        if (!session.isActive()) {
            // Session đã revoke/expire nhưng token vẫn ACTIVE (race condition hiếm).
            // Revoke token để đồng bộ trạng thái.
            currentToken.revoke();
            refreshTokenRepository.save(currentToken);
            throw new InvalidRefreshTokenException("Session is not active");
        }

        // 4. Load + check user
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> UserNotFoundException.byId(session.getUserId()));

        if (!user.canLogin()) {
            // User bị disable/lock/soft-delete sau khi đã login. Revoke session + token.
            session.revoke(RevokedReason.USER_ACTION);
            sessionRepository.save(session);
            refreshTokenRepository.revokeAllBySessionId(session.getId());
            throw new InvalidRefreshTokenException("User cannot login");
        }

        // 5. Sinh refresh token mới
        String newRawToken = secureRandomGenerator.generateToken();
        RefreshToken newToken = createNewRefreshToken(session.getId(), newRawToken);
        RefreshToken savedNew = refreshTokenRepository.save(newToken);

        // 6. Mark token cũ là USED, link tới token mới
        currentToken.markUsed(savedNew.getId(), command.context().ipAddress());
        refreshTokenRepository.save(currentToken);

        // 7. Update lastActiveAt
        session.touch();
        sessionRepository.save(session);

        // 8. Sinh JWT mới với claims hiện tại của user (role/permission có thể đã đổi)
        String accessToken = generateAccessToken(user, session.getId());

        // 9. Trả result
        Instant accessExpiresAt = Instant.now().plus(Duration.ofMinutes(15));
        return new AuthResult(accessToken, newRawToken, accessExpiresAt, session.getId());
    }

    /**
     * Kiểm tra status của token. Đây là trái tim của reuse detection.
     */
    private void checkTokenStatus(RefreshToken token) {
        RefreshTokenStatus status = token.getTokenStatus();

        if (status == RefreshTokenStatus.USED) {
            // !!! REUSE DETECTED !!!
            // Token đã rotate ra token mới, mà giờ lại được gửi lên → có kẻ steal.
            // Hành động: revoke toàn bộ family + revoke session với reason TOKEN_REUSE.
            handleReuseDetection(token);
            throw new TokenReuseDetectedException(token.getSessionId());
        }

        if (status == RefreshTokenStatus.REVOKED) {
            throw new InvalidRefreshTokenException("Token has been revoked");
        }

        // ACTIVE nhưng có thể đã expire (expiresAt < now)
        if (!token.isActive()) {
            throw new InvalidRefreshTokenException("Token has expired");
        }
    }

    /**
     * Khi detect reuse, dọn dẹp triệt để:
     * <ul>
     *   <li>Revoke session với reason TOKEN_REUSE (audit trail)</li>
     *   <li>Revoke toàn bộ refresh token thuộc session (cả ACTIVE lẫn USED chuyển hết về REVOKED)</li>
     * </ul>
     * Không phân biệt ai là user thật / attacker - cả 2 đều phải login lại.
     */
    private void handleReuseDetection(RefreshToken reuseToken) {
        String sessionId = reuseToken.getSessionId();

        // Revoke session - tìm session, mark REVOKED với reason TOKEN_REUSE
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.revoke(RevokedReason.TOKEN_REUSE);
            sessionRepository.save(session);
        });

        // Revoke tất cả refresh token thuộc session (bulk)
        refreshTokenRepository.revokeAllBySessionId(sessionId);
    }

    private RefreshToken createNewRefreshToken(String sessionId, String rawToken) {
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
        Set<String> permissionCodes = user.getEffectivePermissionCodes();
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toSet());

        TokenClaims claims = new TokenClaims(
                user.getId(),
                sessionId,
                roleCodes,
                permissionCodes,
                null,
                null
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
     * Input cho {@link #execute(RefreshCommand)}.
     *
     * @param rawRefreshToken Raw refresh token từ client (Base64 URL-safe, 43 ký tự)
     * @param context         IP + UA của request gọi refresh (lưu vào {@code usedFromIp} cho audit)
     */
    public record RefreshCommand(
            String rawRefreshToken,
            RequestContext context
    ) {
    }
}