package com.personal.identity.core.role;

import com.personal.identity.core.shared.exception.DomainException;

public class RoleNotFoundException extends DomainException {

    private static final String CODE = "ROLE.NOT_FOUND";

    public static RoleNotFoundException byId(Long id) {
        return new RoleNotFoundException("Role not found with id=" + id);
    }

    public static RoleNotFoundException byCode(String code) {
        return new RoleNotFoundException("Role not found with code=" + code);
    }

    private RoleNotFoundException(String message) {
        super(CODE, message);
    }
}
