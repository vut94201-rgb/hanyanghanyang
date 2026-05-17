package com.personal.identity.core.session;

/**
 * Vị trí địa lý resolve từ IP qua MaxMind GeoLite2-City.
 *
 * <p>Tạo bởi adapter {@code MaxMindGeoLocationResolver} (implements
 * {@code GeoLocationResolver} port). Mọi field nullable - khi IP là private
 * (192.168.x, 10.x), localhost, hoặc database không cover IP đó, resolver
 * sẽ trả {@link #empty()}.
 *
 * @param countryName   "Vietnam", "United States"...
 * @param countryCode   ISO 3166-1 alpha-2: "VN", "US"
 * @param cityName      "Hanoi", "San Francisco"
 * @param latitude      Vĩ độ, có thể null
 * @param longitude     Kinh độ
 */
public record GeoLocation(
        String countryName,
        String countryCode,
        String cityName,
        Double latitude,
        Double longitude
) {
    public static GeoLocation empty() {
        return new GeoLocation(null, null, null, null, null);
    }

    public boolean isEmpty() {
        return countryCode == null && cityName == null;
    }
}
