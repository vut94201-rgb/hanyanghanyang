package com.personal.identity.core.session;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Ném khi user cố revoke / truy cập session KHÔNG thuộc về mình.
 *
 * <p><b>Tình huống:</b> {@code DELETE /api/v1/auth/sessions/{id}} - user A login,
 * gửi sessionId của user B → server phải reject, không phải trả 404 (vì 404 sẽ
 * leak thông tin "session id này tồn tại nhưng không thuộc về bạn" vs "session
 * id này không tồn tại").
 *
 * <p><b>Vì sao tách riêng khỏi {@link SessionNotFoundException}:</b>
 * <ul>
 *   <li>Semantic khác: "không tìm thấy" vs "không có quyền".</li>
 *   <li>HTTP status khác: 404 vs 403.</li>
 *   <li>Log level có thể khác: SessionNotFound = DEBUG, AccessDenied = WARN
 *       (dấu hiệu IDOR attempt - Insecure Direct Object Reference).</li>
 * </ul>
 *
 * <p><b>Note bảo mật:</b> Một số hệ thống chọn trả 404 cho cả 2 case để tránh
 * leak "session id này có tồn tại". Đây là design choice. Ở đây dùng 403 vì
 * sessionId là UUID v4 (122 bit entropy) - attacker brute force không khả thi,
 * nên thông tin "tồn tại nhưng không thuộc về bạn" không phải vector quan trọng.
 */
public class SessionAccessDeniedException extends DomainException {

    private static final String CODE = "SESSION.ACCESS_DENIED";

    public SessionAccessDeniedException(String sessionId) {
        super(CODE, "Session not owned by user: " + sessionId);
    }
}