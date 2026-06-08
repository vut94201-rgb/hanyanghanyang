package com.personal.identity.core.domain.session;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class SessionNotFoundException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.SESSION_ACCESS_DENIED;

    public SessionNotFoundException() {
        super(ERROR_CODE);
    }

    public static SessionNotFoundException sessionNotFoundById(String sessionId) {
        return new SessionNotFoundException(sessionId);
    }

    private SessionNotFoundException(String message) {
        super(ERROR_CODE, message);
    }


}
