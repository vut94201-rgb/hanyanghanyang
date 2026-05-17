package com.personal.identity.core.session;

/**
 * <b>PORT</b>: resolve IP address thành {@link GeoLocation}.
 *
 * <p>Implementation mặc định: {@code MaxMindGeoLocationResolver} đọc file
 * {@code GeoLite2-City.mmdb} offline.
 *
 * <p>Có thể swap sang ipinfo.io / ipapi.co (API ngoài) bằng cách viết adapter
 * khác - core không thay đổi.
 */
public interface GeoLocationResolver {

    /**
     * Resolve IP. Không throw - nếu IP private/localhost/không trong DB,
     * trả về {@link GeoLocation#empty()}.
     */
    GeoLocation resolve(String ipAddress);
}
