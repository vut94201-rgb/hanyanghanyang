package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

/**
 * Thrown when a user is not found by id / username / email.
 *
 * <p>Distinguish from {@code InvalidCredentialsException}: when LOGIN fails due to an incorrect password,
 * we use {@code InvalidCredentialsException} (DO NOT use this exception),
 * to prevent leaking the information that "the username exists but the password is incorrect".
 */
public class UserNotFoundException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.USER_NOT_FOUND;

    private UserNotFoundException(String message) {

        super(ERROR_CODE, message);
    }

    public static UserNotFoundException byId(long id) {
        return new UserNotFoundException(String.format("%s: id=%d", ERROR_CODE.getDefaultMessage(), id));
    }

    public static UserNotFoundException byEmail(String email) {
        return new UserNotFoundException(String.format("%s: email=%s", ERROR_CODE.getDefaultMessage(), email));
    }

    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException(String.format("%s: username=%s", ERROR_CODE.getDefaultMessage(), username));
    }
}
