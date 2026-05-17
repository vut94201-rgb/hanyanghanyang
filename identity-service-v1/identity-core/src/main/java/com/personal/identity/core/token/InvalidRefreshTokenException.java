package com.personal.identity.core.token;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi refresh token không hợp lệ vì các lý do KHÔNG phải reuse:
 * <ul>
 *   <li>Token hash không tìm thấy (forgery hoặc đã xóa)</li>
 *   <li>Token đã REVOKED (session bị admin revoke)</li>
 *   <li>Token đã expired</li>
 * </ul>
 *
 * <p>Khác với {@link TokenReuseDetectedException}: reuse là dấu hiệu tấn công →
 * revoke toàn bộ family. Còn cases ở đây chỉ là "token không dùng được nữa",
 * không cần escalate.
 */
public class InvalidRefreshTokenException extends DomainException {

    private static final String CODE = "TOKEN.INVALID_REFRESH_TOKEN";

    public InvalidRefreshTokenException(String reason) {
        super(CODE, "Invalid refresh token: " + reason);
    }
}
