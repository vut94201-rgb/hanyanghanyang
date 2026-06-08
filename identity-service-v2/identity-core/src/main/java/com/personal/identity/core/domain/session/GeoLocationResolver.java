package com.personal.identity.core.domain.session;

/**
 * <b>PORT</b>: resolves an IP address into {@link GeoLocation}.
 *
 * <p>Default implementation: {@code MaxMindGeoLocationResolver} reading the
 * {@code GeoLite2-City.mmdb} file offline.
 *
 * <p>Can be swapped out for ipinfo.io / ipapi.co (external APIs) by writing a different adapter
 * — the core domain remains unchanged.
 */
public interface GeoLocationResolver {
    /**
     * Resolves the IP address. DO NOT throw exceptions — if the IP is private/localhost/not found in the DB,
     * return {@link GeoLocation#empty()}.
     */
    GeoLocation resolve(String ipAddress);
}
