package com.personal.identity.core.service;


import com.personal.identity.core.session.*;
import com.personal.identity.core.token.RefreshTokenRepository;

/**
 * Use case: user revoke 1 session cụ thể của chính mình (qua endpoint
 * {@code DELETE /api/v1/auth/sessions/{id}}).
 *
 * <p><b>Tình huống dùng:</b> User mở trang "Devices signed in" (như Google,
 * GitHub) và bấm "Sign out" cho 1 thiết bị cụ thể.
 *
 * <p><b>Quan trọng - ownership check:</b> phải đảm bảo user A không revoke
 * session của user B. So sánh {@code session.getUserId()} với userId từ JWT.
 * Nếu mismatch → ném {@link SessionAccessDeniedException} (HTTP 403).
 *
 * <p><b>Vì sao KHÔNG dùng {@code LogoutUseCase} ở đây:</b> LogoutUseCase nhận
 * raw JWT access token để parse JTI + blacklist Redis. Ở đây ta KHÔNG có access
 * token của session bị revoke (chỉ có sessionId). Hệ quả: JTI của session đó
 * sẽ KHÔNG vào blacklist - nhưng filter check session ACTIVE mỗi request, nên
 * mọi request từ session đã revoke vẫn fail 403. Real-time logout vẫn hoạt động,
 * chỉ thiếu lớp Redis. Trade-off chấp nhận được - JTI sẽ tự "rớt" khỏi blacklist
 * sau 15 phút (token TTL) qua expired.
 */
public class RevokeSessionUseCase {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RevokeSessionUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revoke 1 session by id, ownership check bằng userId.
     *
     * @param targetSessionId session cần revoke
     * @param requestingUserId userId từ JWT của request hiện tại
     * @throws SessionNotFoundException sessionId không tồn tại
     * @throws SessionAccessDeniedException session không thuộc về user này
     */
    public void execute(String targetSessionId, Long requestingUserId) {
        Session session = sessionRepository.findById(targetSessionId)
                .orElseThrow(() -> new SessionNotFoundException(targetSessionId));

        // Ownership check - quan trọng nhất, ngăn IDOR attack
        if (!session.getUserId().equals(requestingUserId)) {
            throw new SessionAccessDeniedException(targetSessionId);
        }

        // Idempotent: nếu đã REVOKED rồi thì revoke() no-op, không throw.
        // Vẫn revoke refresh tokens phòng trường hợp inconsistent state.
        session.revoke(RevokedReason.USER_ACTION);
        sessionRepository.save(session);

        refreshTokenRepository.revokeAllBySessionId(targetSessionId);
    }
}