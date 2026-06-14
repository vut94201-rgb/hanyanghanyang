package com.personal.identity.infrastructure.persistence.entity.enums;

import com.personal.identity.core.domain.session.DeviceType;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DeviceTypeConverter extends AbstractCodeEnumConverter<DeviceType> {

  protected DeviceTypeConverter() {
    super(DeviceType.class);
  }
}
