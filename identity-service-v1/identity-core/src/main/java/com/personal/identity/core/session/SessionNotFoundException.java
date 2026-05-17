package com.personal.identity.core.session;

import com.personal.identity.core.shared.exception.DomainException;

public class SessionNotFoundException extends DomainException {

    private static final String CODE = "SESSION.NOT_FOUND";

    public SessionNotFoundException(String sessionId) {
        super(CODE, "Session not found: " + sessionId);
    }
}
