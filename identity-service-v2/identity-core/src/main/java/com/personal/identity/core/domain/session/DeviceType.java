package com.personal.identity.core.domain.session;

import com.personal.identity.core.domain.shared.enums.CodeEnum;

public enum DeviceType implements CodeEnum<String> {
  DESKTOP("D"),
  MOBILE("M"),
  TABLET("T"),
  UNKNOWN("U");
  private final String code;

  private DeviceType(String code) {
    this.code = code;
  }

  @Override
  public String getCode() {
    return code;
  }

  public static DeviceType fromCode(String code) {
    for (DeviceType type : DeviceType.values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    throw new IllegalArgumentException("invalid device type code");
  }
}
