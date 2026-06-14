package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.audit.AdminAction;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AdminActionConverter extends AbstractCodeEnumConverter<AdminAction> {
  protected AdminActionConverter() {
    super(AdminAction.class);
  }
}
