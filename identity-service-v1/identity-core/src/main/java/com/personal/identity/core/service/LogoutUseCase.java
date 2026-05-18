package com.personal.identity.core.service;


import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.Session;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.token.AccessTokenBlacklist;
import com.personal.identity.core.token.TokenClaims;
import com.personal.identity.core.token.TokenProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Use case: logout - vô hiệu hóa session và token.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Parse access token → lấy {@code TokenClaims} (chứa sessionId, tokenId/JTI, expiresAt)</li>
 *   <li>Nếu token verify fail → no-op (đã expire / sai signature - không cần làm gì)</li>
 *   <li>Revoke session với reason LOGOUT (idempotent - revoke 2 lần OK)</li>
 *   <li>Revoke tất cả refresh token thuộc session (bulk REVOKED)</li>
 *   <li>Blacklist access token với TTL = thời gian còn lại trước expiry</li>
 * </ol>
 *
 * <p><b>Idempotent:</b> gọi logout 2 lần liên tiếp → lần 2 no-op (session đã REVOKED,
 * token cũng đã blacklist). Không throw.
 *
 * <p><b>Vì sao silent fail khi token invalid:</b>
 * <ul>
 *   <li>Token đã expire: server-side nothing to do (filter đã reject token đó cho
 *       mọi request authenticated). Trả 200 OK.</li>
 *   <li>Token sai signature: hoặc bug client hoặc forgery - không cần leak thông tin.
 *       Trả 200 OK.</li>
 * </ul>
 * Logout là endpoint "best effort", không phải critical path. Khác với login (phải
 * verify chặt) - logout có thể nhẹ tay.
 *
 * <p><b>Vì sao không cần refresh token để logout:</b> client thường chỉ giữ access
 * token ở header. Yêu cầu gửi cả refresh token làm UX tệ. Có sessionId trong access
 * token là đủ để revoke toàn bộ chain refresh thuộc session đó.
 *
 * <p><b>Logout TỪ TẤT CẢ DEVICE:</b> use case này chỉ logout session HIỆN TẠI.
 * "Logout from all devices" là use case khác (chưa làm) - sẽ dùng
 * {@code sessionRepository.revokeAllOtherSessions()} hoặc revoke hết bao gồm cả current.
 */
public class LogoutUseCase {

    private final TokenProvider tokenProvider;
    private final SessionRepository sessionRepository;
    private final com.personal.identity.core.token.RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenBlacklist accessTokenBlacklist;

    public LogoutUseCase(
            TokenProvider tokenProvider,
            SessionRepository sessionRepository,
            com.personal.identity.core.token.RefreshTokenRepository refreshTokenRepository,
            AccessTokenBlacklist accessTokenBlacklist
    ) {
        this.tokenProvider = tokenProvider;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenBlacklist = accessTokenBlacklist;
    }

    /**
     * Logout. Idempotent, silent fail nếu token invalid.
     *
     * @param accessToken JWT từ header {@code Authorization: Bearer ...}
     */
    public void execute(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return; // No-op, không throw
        }

        Optional<TokenClaims> claimsOpt = tokenProvider.parseAndVerify(accessToken);
        if (claimsOpt.isEmpty()) {
            // Token invalid (expired/forged) - server không có gì để làm.
            // KHÔNG throw - logout là best effort.
            return;
        }

        TokenClaims claims = claimsOpt.get();

        // 1. Revoke session
        Optional<Session> sessionOpt = sessionRepository.findById(claims.sessionId());
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            // revoke() idempotent - check ACTIVE bên trong, nếu đã REVOKED thì no-op.
            session.revoke(RevokedReason.LOGOUT);
            sessionRepository.save(session);
        }

        // 2. Revoke tất cả refresh token thuộc session (cả ACTIVE và USED chuyển về REVOKED)
        refreshTokenRepository.revokeAllBySessionId(claims.sessionId());

        // 3. Blacklist access token. TTL = thời gian còn lại trước expiry.
        //    Sau khi access token expire tự nhiên, key trong Redis cũng tự xóa.
        addToBlacklist(claims);
    }

    private void addToBlacklist(TokenClaims claims) {
        if (claims.tokenId() == null) {
            // Defensive: token không có JTI (token cũ sinh trước khi thêm JTI?)
            // Không thể blacklist được - skip silent.
            return;
        }

        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            // Token đã expire - khỏi cần blacklist, filter sẽ tự reject.
            return;
        }

        accessTokenBlacklist.add(claims.tokenId(), ttl);
    }
}