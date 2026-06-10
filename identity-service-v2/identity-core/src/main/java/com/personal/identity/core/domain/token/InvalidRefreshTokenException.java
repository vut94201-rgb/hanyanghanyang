package com.personal.identity.core.domain.token;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

/**
 * Thrown when a refresh token is invalid for reasons OTHER than reuse:
 * <ul>
 * <li>Token hash not found (forgery or already deleted)</li>
 * <li>Token is REVOKED (session was revoked by an admin)</li>
 * <li>Token has expired</li>
 * </ul>
 *
 * <p>Unlike {@link TokenReuseDetectedException}: reuse indicates an attack ->
 * revoke the entire token family. The cases here are simply "token is no longer usable",
 * no escalation required.
 */
public class InvalidRefreshTokenException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.TOKEN_INVALID_REFRESH_TOKEN;

    public InvalidRefreshTokenException() {
        super(ERROR_CODE);
    }

    public InvalidRefreshTokenException(String message) {
        super(
                ERROR_CODE,
                "Invalid refresh token: " + message
        );
    }
}
