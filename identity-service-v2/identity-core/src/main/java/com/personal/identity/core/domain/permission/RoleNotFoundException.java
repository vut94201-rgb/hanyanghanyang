package com.personal.identity.core.domain.permission;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

public class RoleNotFoundException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.ROLE_NOT_FOUND;

    private RoleNotFoundException(String message) {
        super(ERROR_CODE, message);
    }

    public static RoleNotFoundException byId(String id) {
        return new RoleNotFoundException(ERROR_CODE.getDefaultMessage() + ": " + id);
    }

    public static RoleNotFoundException byCode(String code) {
        return new RoleNotFoundException(ERROR_CODE.getDefaultMessage() + ": " + code);
    }

    public RoleNotFoundException() {
        super(ERROR_CODE);
    }
}
