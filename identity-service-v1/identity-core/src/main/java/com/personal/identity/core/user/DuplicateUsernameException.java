package com.personal.identity.core.user;

import com.personal.identity.core.shared.exception.DomainException;

/**
 * Throw khi tạo user mới với username đã tồn tại.
 *
 * <p>Service NÊN check tồn tại trước khi gọi {@code save()} để throw exception
 * này sớm với message thân thiện. Nếu không check, JPA sẽ throw
 * {@code DataIntegrityViolationException} với message khó hiểu cho user.
 */
public class DuplicateUsernameException extends DomainException {

    private static final String CODE = "USER.DUPLICATE_USERNAME";

    public DuplicateUsernameException(String username) {
        super(CODE, "Username already exists: " + username);
    }
}
