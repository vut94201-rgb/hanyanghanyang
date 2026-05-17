package com.personal.identity.core.token;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi phát hiện refresh token đã USED bị gửi lại - đây là DẤU HIỆU TẤN CÔNG.
 *
 * <p>Khi exception này throw, service phải:
 * <ol>
 *   <li>Revoke toàn bộ session family</li>
 *   <li>Revoke tất cả refresh token thuộc session đó</li>
 *   <li>(Optional) Gửi email cảnh báo cho user</li>
 * </ol>
 */
public class TokenReuseDetectedException extends DomainException {

    private static final String CODE = "TOKEN.REUSE_DETECTED";

    public TokenReuseDetectedException(String sessionId) {
        super(CODE, "Refresh token reuse detected for session: " + sessionId);
    }
}
