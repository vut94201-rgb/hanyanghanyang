package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.token.RefreshTokenStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RefreshTokenStatusConverter extends AbstractCodeEnumConverter<RefreshTokenStatus> {

  protected RefreshTokenStatusConverter() {
    super(RefreshTokenStatus.class);
  }
}
