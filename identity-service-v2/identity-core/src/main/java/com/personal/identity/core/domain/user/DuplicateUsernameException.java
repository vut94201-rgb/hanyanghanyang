package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class DuplicateUsernameException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.DUPLICATE_USERNAME;

    public DuplicateUsernameException(String message) {
        super(ERROR_CODE, message);
    }

    public DuplicateUsernameException() {
        super(ERROR_CODE);
    }

}

