package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

/**
 * Thrown when login fails because the username does NOT exist OR the password is INCORRECT.
 *
 * <p><b>Why combine both cases into a single exception without differentiation:</b>
 * Security best practice — if we differentiated between {@link UserNotFoundException} (user does not exist)
 * and "wrong password" (user exists but password is incorrect), an attacker could enumerate usernames
 * by trying various usernames and observing the different responses. Returning the EXACT SAME message
 * and taking the SAME amount of processing time (BCrypt verification executes even if the user does not exist — see
 * {@code LoginUseCase}) is the standard way to prevent basic user enumeration.
 *
 * <p><b>Convention:</b> the message is always "Invalid username or password" —
 * NEVER "user not found" or "wrong password". The frontend must also
 * display this message, making no distinction between the two cases.
 */
public class InvalidCredentialsException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.AUTH_INVALID_CREDENTIALS;

    public InvalidCredentialsException(String message) {
        super(ERROR_CODE, message);
    }

    public InvalidCredentialsException() {
        super(ERROR_CODE);
    }
}
