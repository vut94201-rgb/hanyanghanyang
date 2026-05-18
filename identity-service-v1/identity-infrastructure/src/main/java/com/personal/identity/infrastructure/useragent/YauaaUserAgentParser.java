package com.personal.identity.infrastructure.useragent;


import com.personal.identity.core.session.DeviceInfo;
import com.personal.identity.core.session.DeviceType;
import com.personal.identity.core.session.UserAgentParser;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;

/**
 * Adapter implements {@link UserAgentParser} (port của core) dùng yauaa.
 *
 * <p><b>Triết lý:</b> KHÔNG bao giờ throw. User-Agent là input không trusted từ
 * client - có thể rỗng, null, hoặc cố tình ghi sai. Adapter này luôn trả về
 * 1 {@link DeviceInfo} hợp lệ (có thể là {@link DeviceInfo#unknown()}). Không
 * để parse fail block luồng login.
 *
 * <p><b>Bẫy yauaa:</b> khi không xác định được giá trị, yauaa trả về STRING
 * {@code "Unknown"} (literal text 7 ký tự) chứ KHÔNG trả null. Phải convert
 * thành null Java ở method {@link #nullIfUnknown(String)} để DB lưu null đúng
 * nghĩa (thay vì literal "Unknown" tràn lan trong column).
 *
 * <p><b>Mapping DeviceClass → DeviceType:</b>
 * <ul>
 *   <li>{@code "Desktop"} → {@code DESKTOP}</li>
 *   <li>{@code "Phone"}, {@code "Mobile"} → {@code MOBILE}
 *       (Phone = smartphone xác định brand; Mobile = chỉ biết là mobile.
 *       Cả hai gộp về MOBILE vì DB không phân biệt sâu hơn.)</li>
 *   <li>{@code "Tablet"} → {@code TABLET}</li>
 *   <li>Tất cả còn lại (Watch, TV, Robot, eReader, Game Console, Unknown, ...)
 *       → {@code UNKNOWN}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class YauaaUserAgentParser implements UserAgentParser {

    /**
     * yauaa string trả về khi không xác định được. Phải convert sang null Java.
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
            // Bất kỳ lỗi nào từ yauaa - trả unknown thay vì throw lên service.
            // Không log ERROR - User-Agent rác là chuyện thường ngày.
            return DeviceInfo.unknown();
        }
    }

    /**
     * yauaa trả literal "Unknown" khi không xác định được. Quy chuẩn thành null
     * để tránh literal "Unknown" đi vào DB.
     */
    private String nullIfUnknown(String value) {
        if (value == null || value.isBlank() || YAUAA_UNKNOWN.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * Map string DeviceClass của yauaa sang enum {@link DeviceType} của core.
     * Bất cứ value lạ nào → UNKNOWN, không bao giờ throw.
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
     * Tạo friendly device name: "Chrome 120 on macOS". Nếu thiếu info, trả null
     * thay vì chuỗi nửa vời như "null on null".
     */
    private String buildDeviceName(String browserName, String browserVersion, String osName) {
        if (browserName == null && osName == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (browserName != null) {
            sb.append(browserName);
            if (browserVersion != null) {
                // Chỉ lấy major version cho gọn: "120.0.6099.62" → "120"
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