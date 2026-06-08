package com.personal.identity.core.domain.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email already exists"),
    INVALID_PASSWORD("INVALID_PASSWORD", "Invalid password"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", "Invalid credentials"),
    DUPLICATE_USERNAME("DUPLICATE_USERNAME", "Duplicate username"),

    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token expired"),
    TOKEN_INVALID("TOKEN_INVALID", "Token is invalid"),
    TOKEN_REUSE_DETECTED("TOKEN_REUSE_DETECTED", "Token reuse detected"),
    TOKEN_INVALID_REFRESH_TOKEN("TOKEN_INVALID_REFRESH_TOKEN", "Token invalid refresh token"),

    SESSION_ACCESS_DENIED("SESSION_ACCESS_DENIED", "Session access denied"),
    SESSION_NOT_FOUND("SESSION_NOT_FOUND", "Session not found"),

    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
    PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied");


    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }


}
