package com.personal.identity.core.user;

import com.personal.identity.core.shared.exception.DomainException;
import com.personal.identity.core.shared.exception.ErrorCode;

import java.io.Serial;

public class DuplicateEmailException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  protected DuplicateEmailException(String email) {
    super(
        ErrorCode.EMAIL_ALREADY_EXISTS, ErrorCode.EMAIL_ALREADY_EXISTS.getDefaultMessage() + email);
  }

  protected DuplicateEmailException(String email, Throwable cause) {
    super(
        ErrorCode.EMAIL_ALREADY_EXISTS,
        ErrorCode.EMAIL_ALREADY_EXISTS.getDefaultMessage() + email,
        cause);
  }
}
