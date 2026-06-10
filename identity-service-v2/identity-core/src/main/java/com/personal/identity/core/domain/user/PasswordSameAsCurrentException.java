package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class PasswordSameAsCurrentException extends DomainException {
    public PasswordSameAsCurrentException(String defaultMessage) {
        super(
                ErrorCode.PASSWORD_SAME_AS_CURRENT,
                defaultMessage
        );
    }

    public PasswordSameAsCurrentException() {
        super(ErrorCode.PASSWORD_SAME_AS_CURRENT);
    }


}
