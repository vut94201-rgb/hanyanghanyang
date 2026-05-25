package com.personal.identity.core.shared.exception;

import lombok.Getter;

import java.io.Serial;

@Getter
public abstract class DomainException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 1L;

  private final ErrorCode errorCode;

  protected DomainException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }
}
