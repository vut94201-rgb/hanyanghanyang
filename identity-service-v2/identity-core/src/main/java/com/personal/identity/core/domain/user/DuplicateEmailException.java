package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

import java.io.Serial;

public class DuplicateEmailException extends DomainException {
    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateEmailException(String email) {
        super(
                ErrorCode.EMAIL_ALREADY_EXISTS, ErrorCode.EMAIL_ALREADY_EXISTS.getDefaultMessage() + ": " + email);
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                ErrorCode.EMAIL_ALREADY_EXISTS.getDefaultMessage() + email,
                cause);
    }
}
