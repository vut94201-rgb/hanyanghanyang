package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.session.DeviceInfo;
import com.personal.identity.core.session.GeoLocation;
import com.personal.identity.core.session.Session;
import com.personal.identity.infrastructure.persistence.entity.SessionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper cho {@link Session} ↔ {@link SessionEntity}.
 *
 * <p><b>Đặc thù:</b>
 * <ul>
 *   <li>Entity có 6 cột device info phẳng (deviceType, deviceName, osName,...);
 *       Domain có 1 value object {@link DeviceInfo} gom các field này.</li>
 *   <li>Tương tự với GeoLocation và 5 cột location (country, city, lat, lng,...).</li>
 * </ul>
 *
 * <p>Mapper phải tự viết logic flatten/gather - dùng default method thay vì
 * khai báo dòng @Mapping(target="x", source="y") cho từng field.
 *
 * <p>{@code BigDecimal ↔ Double} cho lat/lng: Oracle NUMBER(10,7) map sang
 * BigDecimal trong entity (chính xác cao). Domain dùng Double cho gọn.
 */
@Mapper
public interface SessionMapper {

    /**
     * Entity → Domain. Phải gom 6+5 cột phẳng thành 2 value object.
     */
    @Mapping(target = "deviceInfo", expression = "java(toDeviceInfo(entity))")
    @Mapping(target = "location", expression = "java(toGeoLocation(entity))")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    Session toDomain(SessionEntity entity);

    /**
     * Domain → Entity, dùng cho CREATE. Phải flatten DeviceInfo + GeoLocation.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "ipAddress", source = "ipAddress")
    @Mapping(target = "userAgent", source = "userAgent")
    @Mapping(target = "sessionStatus", source = "sessionStatus")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
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
     * Update entity từ domain - dùng khi save lại (vd: sau khi revoke).
     * KHÔNG update id, userId, createdAt - những field "immutable" của session.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "sessionStatus", source = "sessionStatus")
    @Mapping(target = "lastActiveAt", source = "lastActiveAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "revokedAt", source = "revokedAt")
    @Mapping(target = "revokedReason", source = "revokedReason")
    void updateEntity(Session domain, @MappingTarget SessionEntity entity);

    // ---- Helper methods ----

    /** Gom 6 cột device thành DeviceInfo value object. */
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

    /** Gom 5 cột location thành GeoLocation value object. */
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
