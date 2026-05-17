package com.personal.identity.core.session;

/**
 * Thông tin thiết bị parse từ User-Agent. Value object thuần (record).
 *
 * <p>Tạo bởi adapter {@code YauaaUserAgentParser} (implements {@code UserAgentParser}
 * port). Mọi field đều có thể null nếu yauaa không parse được - không throw, để
 * field null thay vì block luồng login.
 *
 * @param deviceType      DESKTOP/MOBILE/TABLET/UNKNOWN
 * @param deviceName      Friendly name: "Chrome 120 on macOS"
 * @param osName          "macOS", "Windows", "Android"...
 * @param osVersion       "14.2", "11", ...
 * @param browserName     "Chrome", "Safari"...
 * @param browserVersion  "120.0.6099.62"
 */
public record DeviceInfo(
        DeviceType deviceType,
        String deviceName,
        String osName,
        String osVersion,
        String browserName,
        String browserVersion
) {
    /**
     * Fallback khi không parse được. Tránh null check rải rác.
     */
    public static DeviceInfo unknown() {
        return new DeviceInfo(DeviceType.UNKNOWN, null, null, null, null, null);
    }
}
