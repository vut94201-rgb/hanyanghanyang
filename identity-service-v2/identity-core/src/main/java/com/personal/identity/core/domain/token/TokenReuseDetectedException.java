package com.personal.identity.core.domain.token;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class TokenReuseDetectedException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.TOKEN_REUSE_DETECTED;

    public TokenReuseDetectedException() {
        super(ERROR_CODE);
    }

    public TokenReuseDetectedException(String message) {
        super(ERROR_CODE, message);
    }
}
