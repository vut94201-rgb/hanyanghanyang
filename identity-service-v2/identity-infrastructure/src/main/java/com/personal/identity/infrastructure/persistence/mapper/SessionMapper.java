package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.domain.session.DeviceInfo;
import com.personal.identity.core.domain.session.GeoLocation;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.infrastructure.persistence.entity.SessionEntity;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for {@link Session} ↔ {@link SessionEntity}.
 *
 * <p><b>Specifics:</b>
 * <ul>
 * <li>The entity has 6 flattened device info columns (deviceType, deviceName, osName,...);
 * The domain has 1 {@link DeviceInfo} value object that groups these fields.</li>
 * <li>Similarly with GeoLocation and 5 location columns (country, city, lat, lng,...).</li>
 * </ul>
 *
 * <p>The mapper must manually implement the flatten/gather logic - using default methods instead of
 * declaring {@code @Mapping(target="x", source="y")} for every single field.
 *
 * <p>{@code BigDecimal ↔ Double} for lat/lng: Oracle's NUMBER(10,7) maps to
 * BigDecimal in the entity (for high precision). The domain uses Double for simplicity.
 */
@Mapper
public interface SessionMapper {

    /**
     * Entity → Domain. Must gather 6+5 flattened columns into 2 value objects.
     */
    default Session toDomain(SessionEntity entity) {
        if (entity == null) {
            return null;
        }

        return Session.rehydrate(
                entity.getId(),
                entity.getUserId(),
                toDeviceInfo(entity),
                toGeoLocation(entity),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getSessionStatus(),
                entity.getCreatedAt(),
                entity.getLastActiveAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getRevokedReason()
        );
    }

    /**
     * Domain → Entity, used for CREATE. Must flatten DeviceInfo + GeoLocation.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "sessionStatus", source = "sessionStatus")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    @Mapping(target = "expiresAt", source = "expiredAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "revokedReason", source = "revokedReason")
    // DeviceInfo flatten
    @Mapping(target = "deviceType", source = "deviceInfo.deviceType")
    @Mapping(target = "deviceName", source = "deviceInfo.deviceName")
    @Mapping(target = "osName", source = "deviceInfo.osName")
    @Mapping(target = "osVersion", source = "deviceInfo.osVersion")
    @Mapping(target = "browserName", source = "deviceInfo.browserName")
    @Mapping(target = "browserVersion", source = "deviceInfo.browserVersion")
    // GeoLocation flatten
    @Mapping(target = "countryName", source = "location.countryName")
    @Mapping(target = "countryCode", source = "location.countryCode")
    @Mapping(target = "cityName", source = "location.cityName")
    @Mapping(target = "latitude", source = "location.latitude", qualifiedByName = "doubleToBigDecimal")
    @Mapping(target = "longitude", source = "location.longitude", qualifiedByName = "doubleToBigDecimal")
    SessionEntity toEntity(Session domain);

    /**
     * Updates an entity from the domain - used upon re-saving (e.g., after a revoke).
     * Does NOT update id, userId, or createdAt - these are "immutable" session fields.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sessionStatus", source = "sessionStatus")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    @Mapping(target = "expiresAt", source = "expiredAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "revokedReason", source = "revokedReason")
    void updateEntity(Session domain, @MappingTarget SessionEntity entity);

    // ---- Helper methods ----

    /** Gathers 6 device columns into a DeviceInfo value object. */
    default DeviceInfo toDeviceInfo(SessionEntity entity) {
        if (entity == null) return null;
        return new DeviceInfo(
                entity.getDeviceType(),
                entity.getDeviceName(),
                entity.getOsName(),
                entity.getOsVersion(),
                entity.getBrowserName(),
                entity.getBrowserVersion()
        );
    }

    /** Gathers 5 location columns into a GeoLocation value object. */
    default GeoLocation toGeoLocation(SessionEntity entity) {
        if (entity == null) return null;
        return new GeoLocation(
                entity.getCountryName(),
                entity.getCountryCode(),
                entity.getCityName(),
                bigDecimalToDouble(entity.getLatitude()),
                bigDecimalToDouble(entity.getLongitude())
        );
    }

    @Named("doubleToBigDecimal")
    default BigDecimal doubleToBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    default Double bigDecimalToDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    default List<Session> toDomainList(List<SessionEntity> entities) {
        if (entities == null) return java.util.Collections.emptyList();
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }
}