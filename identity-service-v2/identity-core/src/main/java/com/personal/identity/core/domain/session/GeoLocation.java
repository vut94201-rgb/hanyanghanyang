package com.personal.identity.core.domain.session;

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
