package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.token.RefreshToken;
import com.personal.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MapStruct mapper cho {@link RefreshToken} ↔ {@link RefreshTokenEntity}.
 * Structural mapping đơn thuần - field-to-field cùng tên.
 */
@Mapper
public interface RefreshTokenMapper {

    RefreshToken toDomain(RefreshTokenEntity entity);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sessionId", source = "sessionId")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "tokenStatus", source = "tokenStatus")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "usedAt", source = "usedAt")
    @Mapping(target = "usedFromIp", source = "usedFromIp")
    @Mapping(target = "replacedByTokenId", source = "replacedByTokenId")
    RefreshTokenEntity toEntity(RefreshToken domain);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tokenStatus", source = "tokenStatus")
    @Mapping(target = "usedAt", source = "usedAt")
    @Mapping(target = "usedFromIp", source = "usedFromIp")
    @Mapping(target = "replacedByTokenId", source = "replacedByTokenId")
    void updateEntity(RefreshToken domain, @MappingTarget RefreshTokenEntity entity);

    default List<RefreshToken> toDomainList(List<RefreshTokenEntity> entities) {
        if (entities == null) return java.util.Collections.emptyList();
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }
}
