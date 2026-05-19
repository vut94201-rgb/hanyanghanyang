package com.personal.identity.core.session;

import java.util.List;
import java.util.Optional;


/**
 * <b>PORT</b> cho Session persistence.
 */
public interface SessionRepository {

    Session save(Session session);

    Optional<Session> findById(String id);

    /** List session ACTIVE của 1 user - cho API "list devices". */
    List<Session> findActiveByUserId(Long userId);

    /** Tất cả session của user, bất kể status - cho admin xem lịch sử. */
    List<Session> findAllByUserId(Long userId);

    /**
     * Revoke tất cả session ACTIVE của user, TRỪ session hiện tại.
     * Dùng cho "logout from all other devices" trong change-password flow.
     *
     * @return số session bị revoke
     */
    int revokeAllOtherSessions(Long userId, String currentSessionId, RevokedReason reason);

    /**
     * Revoke TẤT CẢ session ACTIVE của user (kể cả session hiện tại).
     * Dùng cho endpoint {@code POST /logout-all} - user chủ động kick mọi device,
     * bao gồm chính thiết bị đang gửi request. Sau API này user phải login lại.
     *
     * <p><b>Khác với {@link #revokeAllOtherSessions}:</b> không có tham số
     * {@code currentSessionId} để loại trừ. Đây là điểm khác biệt cốt lõi giữa
     * "đổi password" (giữ session hiện tại) và "logout all" (revoke hết).
     *
     * @return số session bị revoke
     */
    int revokeAllByUserId(Long userId, RevokedReason reason);
}