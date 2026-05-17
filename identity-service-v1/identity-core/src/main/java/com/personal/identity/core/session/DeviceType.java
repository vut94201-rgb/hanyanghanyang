package com.personal.identity.core.session;

/**
 * Loại thiết bị parse từ User-Agent qua yauaa.
 * Khớp với {@code CHECK (device_type IN ('DESKTOP','MOBILE','TABLET','UNKNOWN'))}.
 */
public enum DeviceType {
    DESKTOP,
    MOBILE,
    TABLET,
    UNKNOWN
}
