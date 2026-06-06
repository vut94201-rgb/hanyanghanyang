package com.personal.identity.core.domain.shared.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
  EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "Email already exists"),
  INVALID_PASSWORD("INVALID_PASSWORD", "Invalid password"),

  TOKEN_EXPIRED("TOKEN_EXPIRED", "Token expired"),
  TOKEN_INVALID("TOKEN_INVALID", "Token is invalid"),

  ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found"),
  PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied");

  private final String code;
  private final String defaultMessage;

  ErrorCode(String code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }


}
