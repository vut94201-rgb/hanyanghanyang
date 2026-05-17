package com.personal.identity.core.user;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi tạo user mới với email đã tồn tại.
 */
public class DuplicateEmailException extends DomainException {

    private static final String CODE = "USER.DUPLICATE_EMAIL";

    public DuplicateEmailException(String email) {
        super(CODE, "Email already exists: " + email);
    }
}
