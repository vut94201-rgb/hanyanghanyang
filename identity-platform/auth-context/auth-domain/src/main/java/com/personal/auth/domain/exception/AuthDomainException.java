package com.personal.auth.domain.exception;

import com.personal.shared.exception.BusinessException;
/**
 * Exception thrown by Auth domain code when a business rule is violated.
 *
 * <p>Carries an {@link AuthErrorCode} so the API exception handler can map
 * the failure to a stable HTTP response.
 *
 * <p>Domain code should throw this — and only this — for business-rule
 * violations. Infrastructure failures (DB down, network) should propagate
 * as runtime exceptions and be handled at the boundary separately.
 */
public class AuthDomainException extends BusinessException {
    public AuthDomainException(AuthErrorCode authErrorCode) {
        super(authErrorCode);
    }

    public AuthDomainException(AuthErrorCode authErrorCode, String detailMessage) {
        super(authErrorCode, detailMessage);
    }

    public AuthDomainException(AuthErrorCode authErrorCode, Throwable cause) {
        super(authErrorCode, cause);
    }
}
