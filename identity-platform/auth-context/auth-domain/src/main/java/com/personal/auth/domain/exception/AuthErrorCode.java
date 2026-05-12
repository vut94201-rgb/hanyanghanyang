package com.personal.auth.domain.exception;

import com.personal.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 * Error codes for the Auth bounded context.
 *
 * <p>Convention: {@code AUTH-{number}} where the number range hints at
 * subdomain:
 * <ul>
 *   <li>{@code 1xx} — registration / account lifecycle</li>
 *   <li>{@code 2xx} — credentials / login (used later)</li>
 *   <li>{@code 9xx} — generic / unexpected (used later)</li>
 * </ul>
 *
 * <p>Codes are STABLE identifiers — once published to clients, never
 * change their meaning. Add new ones, don't repurpose old ones.
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS("AUTH-101", "Email already exists"),
    USERNAME_ALREADY_EXISTS("AUTH-102", "Username already exists"),
    INVALID_EMAIL_FORMAT("AUTH-103", "Invalid email format"),
    WEAK_PASSWORD("AUTH-104", "Password does not meet complexity requirements"),
    USER_NOT_FOUND("AUTH-105", "User not found"),
    USER_LOCKED("AUTH-106", "User account is locked");
    private final String code;
    private final String message;
}
