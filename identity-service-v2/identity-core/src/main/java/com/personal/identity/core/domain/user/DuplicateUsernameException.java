package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class DuplicateUsernameException extends DomainException {


    public DuplicateUsernameException(String message) {
        super(ErrorCode.DUPLICATE_USERNAME, message);
    }

    public DuplicateUsernameException() {
        super(ErrorCode.DUPLICATE_USERNAME);
    }

}

