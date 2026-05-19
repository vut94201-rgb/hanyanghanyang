package com.personal.identity.core.service;


import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.Session;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.token.RefreshTokenRepository;

import java.util.List;

/**
 * Use case: revoke TẤT CẢ session của user (kể cả session hiện tại).
 *
 * <p><b>Khác biệt với {@code ChangePasswordUseCase}:</b>
 * <ul>
 *   <li>change-password revoke session KHÁC, GIỮ session hiện tại (UX tốt -
 *       user không phải login lại trên chính thiết bị họ vừa thao tác).</li>
 *   <li>logout-all revoke TẤT CẢ, kể cả session hiện tại (intent là "kick mọi
 *       device kể cả tôi - tôi nghi ngờ bị compromise").</li>
 * </ul>
 *
 * <p><b>Tình huống dùng:</b> User mất laptop, nghi ngờ ai đó đang dùng tài khoản,
 * muốn log out toàn bộ device → sau đó đổi password.
 *
 * <p><b>Vì sao revoke từng session qua loop thay vì bulk SQL UPDATE?</b>
 * Để cho mỗi session đi qua {@code Session.revoke(RevokedReason)} domain method.
 * Việc bulk SQL UPDATE trực tiếp ({@code sessionRepository.revokeAllByUserId})
 * có lợi performance nhưng bỏ qua domain logic (nếu sau này có side effect như
 * publish event "session revoked", bulk SQL sẽ miss).
 *
 * <p>Tuy nhiên ở thời điểm hiện tại {@code revoke()} chỉ set field, KHÔNG có
 * side effect, nên dùng bulk SQL ở đây OK. Chọn bulk vì performance: user có
 * 10 device là bình thường, không nên load 10 entity + 10 UPDATE statement.
 * Trade-off chốt: ưu tiên perf → bulk. Nếu sau này thêm event publishing thì
 * refactor.
 */
public class LogoutAllUseCase {

    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutAllUseCase(
            SessionRepository sessionRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Revoke tất cả session ACTIVE của user.
     *
     * @return số session bị revoke (để client có thể hiện toast "Đã đăng xuất khỏi N thiết bị")
     */
    public int execute(Long userId) {
        // 1. Lấy danh sách session ACTIVE TRƯỚC khi revoke (để revoke refresh tokens sau)
        List<Session> activeSessions = sessionRepository.findActiveByUserId(userId);

        // 2. Bulk UPDATE - 1 query duy nhất
        int revokedCount = sessionRepository.revokeAllByUserId(userId, RevokedReason.USER_ACTION);

        // 3. Revoke refresh tokens của từng session.
        // Vẫn loop ở đây vì RefreshTokenRepository chưa có method "revoke by user_id".
        // Có thể optimize sau bằng cách thêm port method.
        for (Session session : activeSessions) {
            refreshTokenRepository.revokeAllBySessionId(session.getId());
        }

        return revokedCount;
    }
}