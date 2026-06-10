package com.personal.identity.infrastructure.useragent;

import com.personal.identity.core.domain.session.DeviceInfo;
import com.personal.identity.core.domain.session.DeviceType;
import com.personal.identity.core.domain.session.UserAgentParser;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;


import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link UserAgentParser} (a core port) using yauaa.
 *
 * <p><b>Philosophy:</b> NEVER throw exceptions. The User-Agent is untrusted client input — it might
 * be empty, null, or deliberately spoofed. This adapter consistently returns a valid {@link DeviceInfo}
 * (which may simply be {@link DeviceInfo#unknown()}). Parsing failures must never block the login execution flow.
 *
 * <p><b>Yauaa Pitfall:</b> When unable to determine a value, yauaa returns the STRING
 * {@code "Unknown"} (a 7-character literal) rather than returning null. This must be converted
 * to a proper Java null via {@link #nullIfUnknown(String)} to ensure the database accurately stores
 * nulls (preventing the literal "Unknown" from polluting the columns).
 *
 * <p><b>Mapping DeviceClass -> DeviceType:</b>
 * <ul>
 * <li>{@code "Desktop"} -> {@link DeviceType#DESKTOP}</li>
 * <li>{@code "Phone"}, {@code "Mobile"} -> {@link DeviceType#MOBILE}
 * (Phone = smartphone with identified brand; Mobile = generic mobile device. Both are consolidated
 * into MOBILE as the DB does not require deeper granularity.)</li>
 * <li>{@code "Tablet"} -> {@link DeviceType#TABLET}</li>
 * <li>Everything else (Watch, TV, Robot, eReader, Game Console, Unknown, ...)
 * -> {@link DeviceType#UNKNOWN}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class YauaaUserAgentParser implements UserAgentParser {

    /**
     * The literal string yauaa returns for undetermined values. Must be converted to a Java null.
     */
    private static final String YAUAA_UNKNOWN = "Unknown";

    private final UserAgentAnalyzer analyzer;

    @Override
    public DeviceInfo parse(String rawUserAgent) {
        if (rawUserAgent == null || rawUserAgent.isBlank()) {
            return DeviceInfo.unknown();
        }

        try {
            UserAgent ua = analyzer.parse(rawUserAgent);

            String deviceClass = ua.getValue(UserAgent.DEVICE_CLASS);
            String osName = nullIfUnknown(ua.getValue(UserAgent.OPERATING_SYSTEM_NAME));
            String osVersion = nullIfUnknown(ua.getValue(UserAgent.OPERATING_SYSTEM_VERSION));
            String browserName = nullIfUnknown(ua.getValue(UserAgent.AGENT_NAME));
            String browserVersion = nullIfUnknown(ua.getValue(UserAgent.AGENT_VERSION));

            DeviceType deviceType = mapDeviceType(deviceClass);
            String deviceName = buildDeviceName(browserName, browserVersion, osName);

            return new DeviceInfo(
                    deviceType,
                    deviceName,
                    osName,
                    osVersion,
                    browserName,
                    browserVersion
            );
        } catch (RuntimeException e) {
            // Catches any yauaa parsing errors - returns unknown rather than propagating the exception to the service layer.
            // DO NOT log at ERROR level - garbage User-Agent strings are an everyday occurrence.
            return DeviceInfo.unknown();
        }
    }

    /**
     * Yauaa returns the literal "Unknown" when undetermined. Standardizes this to null
     * to prevent the literal "Unknown" from bleeding into the DB.
     */
    private String nullIfUnknown(String value) {
        if (value == null || value.isBlank() || YAUAA_UNKNOWN.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * Maps yauaa's DeviceClass string to the core's {@link DeviceType} enum.
     * Any unrecognized value defaults to UNKNOWN, ensuring no exceptions are thrown.
     */
    private DeviceType mapDeviceType(String deviceClass) {
        if (deviceClass == null) {
            return DeviceType.UNKNOWN;
        }
        return switch (deviceClass) {
            case "Desktop" -> DeviceType.DESKTOP;
            case "Phone", "Mobile" -> DeviceType.MOBILE;
            case "Tablet" -> DeviceType.TABLET;
            default -> DeviceType.UNKNOWN;
        };
    }

    /**
     * Constructs a friendly device name: "Chrome 120 on macOS". Returns null if essential
     * info is missing, avoiding half-baked strings like "null on null".
     */
    private String buildDeviceName(String browserName, String browserVersion, String osName) {
        if (browserName == null && osName == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (browserName != null) {
            sb.append(browserName);
            if (browserVersion != null) {
                // Extracts only the major version for brevity: "120.0.6099.62" -> "120"
                int dotIndex = browserVersion.indexOf('.');
                sb.append(' ').append(dotIndex > 0 ? browserVersion.substring(0, dotIndex) : browserVersion);
            }
        }
        if (osName != null) {
            if (!sb.isEmpty()) {
                sb.append(" on ");
            }
            sb.append(osName);
        }
        return sb.toString();
    }
}