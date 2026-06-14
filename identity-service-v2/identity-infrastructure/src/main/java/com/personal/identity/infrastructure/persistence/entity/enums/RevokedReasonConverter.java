package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.session.RevokedReason;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RevokedReasonConverter extends AbstractCodeEnumConverter<RevokedReason> {

  protected RevokedReasonConverter() {
      super(RevokedReason.class);
  }
}
