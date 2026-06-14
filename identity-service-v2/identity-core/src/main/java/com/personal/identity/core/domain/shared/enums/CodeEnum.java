package com.personal.identity.core.domain.shared.enums;

public interface CodeEnum <T>{
  T getCode();

  default boolean matchCode(String code) {
    return getCode().toString().equalsIgnoreCase(code);
  }
}
