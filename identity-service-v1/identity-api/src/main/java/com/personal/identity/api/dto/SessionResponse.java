package com.personal.identity.api.dto;


import com.personal.identity.core.session.DeviceType;
import com.personal.identity.core.session.Session;

import java.time.Instant;

/**
 * Response cho endpoint {@code GET /api/v1/auth/sessions} (list active sessions
 * của user hiện tại) và metadata cho từng session.
 *
 * <p><b>Vì sao KHÔNG có {@code userId}:</b> client gọi endpoint này đã biết
 * mình là ai (đã authenticated). Trả userId chỉ tạo noise. Nếu cần admin API
 * xem session của user khác sẽ làm DTO riêng.
 *
 * <p><b>{@code current}:</b> đánh dấu session nào là session đang gửi request -
 * giúp UI hiển thị "This device" thay vì cho user nhầm revoke session mình
 * đang dùng. So sánh {@code session.id} với {@code AuthenticatedUser.sessionId()}
 * ở tầng controller.
 *
 * <p><b>Trường device gộp:</b> {@code deviceName} đã là "Chrome 120 on macOS"
 * (do Yauaa build), không cần expose riêng từng trường browser/os để UI gọn.
 * Nếu client cần raw để custom display thì thêm endpoint riêng.
 *
 * @param id           UUID session
 * @param deviceType   DESKTOP / MOBILE / TABLET / UNKNOWN
 * @param deviceName   "Chrome 120 on macOS"
 * @param ipAddress    IP lúc login (chưa update lúc activity sau)
 * @param countryName  null nếu GeoIP không resolve được
 * @param cityName     null nếu GeoIP không resolve được
 * @param createdAt    Lúc login lần đầu
 * @param lastActiveAt Update mỗi request authenticated qua filter
 * @param current      true nếu là session đang gửi request
 */
public record SessionResponse(
        String id,
        DeviceType deviceType,
        String deviceName,
        String ipAddress,
        String countryName,
        String cityName,
        Instant createdAt,
        Instant lastActiveAt,
        boolean current
) {

    public static SessionResponse from(Session session, String currentSessionId) {
        var device = session.getDeviceInfo();
        var loc = session.getLocation();

        return new SessionResponse(
                session.getId(),
                device != null ? device.deviceType() : DeviceType.UNKNOWN,
                device != null ? device.deviceName() : null,
                session.getIpAddress(),
                loc != null ? loc.countryName() : null,
                loc != null ? loc.cityName() : null,
                session.getCreatedAt(),
                session.getLastActiveAt(),
                session.getId().equals(currentSessionId)
        );
    }
}