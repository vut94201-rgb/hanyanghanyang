package com.personal.identityservicealmav1.shared_config.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    CANNOT_SEND_EMAIL(1008, "Cannot send email", HttpStatus.BAD_REQUEST),
    TOKEN_ALREADY_LOGGED_OUT(1010, "Token already logged out", HttpStatus.UNAUTHORIZED),
    DUPLICATE_USERNAME(1011, "Username already exists", HttpStatus.BAD_REQUEST),
    DUPLICATE_EMAIL(1012, "Email already exists", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1013, "Invalid email format", HttpStatus.BAD_REQUEST),
    DUPLICATE_PHONE_NUMBER(1014, "Phone number already exists", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER(1015, "Invalid phone number format", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1016, "User not found", HttpStatus.NOT_FOUND),
    INVALID_OLD_PASSWORD(1017, "Old password is incorrect", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1018, "Password is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_IN_USE(1019, "Email is already in use", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_IN_USE(1019, "Phone number is already in use", HttpStatus.BAD_REQUEST),
    USERNAME_ALREADY_IN_USE(1019, "Username is already in use", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1020, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(1021, "Permission not found", HttpStatus.NOT_FOUND),
    ROLE_NAME_ALREADY_EXISTS(1022, "Role name already exists", HttpStatus.BAD_REQUEST),
    PERMISSION_NAME_ALREADY_EXISTS(1023, "Permission name already exists", HttpStatus.BAD_REQUEST),
    SESSION_LIMIT_REACHED(
            1024, "User has reached the maximum number of login sessions", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER(1025, "Missing required parameter", HttpStatus.BAD_REQUEST),
    MISSING_PATH_VARIABLE(1026, "Missing required path variable", HttpStatus.BAD_REQUEST),
    ACCOUNT_LOCKED(1027, "Account is locked. Please try again later.", HttpStatus.FORBIDDEN),
    INVALID_USERNAME_OR_PASSWORD(1028, "Invalid username or password", HttpStatus.BAD_REQUEST);





    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}
