package com.personal.identity.core.domain.session;

public record DeviceInfo(
    DeviceType deviceType,
    String deviceName,
    String osName,
    String osVersion,
    String browserName,
    String browserVersion) {

  public static DeviceInfo unknown() {
    return new DeviceInfo(DeviceType.UNKNOWN, null, null, null, null, null);
  }
}
