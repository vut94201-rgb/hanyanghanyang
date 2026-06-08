package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.domain.token.RefreshToken;
import com.personal.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper
public interface RefreshTokenMapper {

    default RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return RefreshToken.rehydrate(
                entity.getId(),
                entity.getSessionId(),
                entity.getTokenHash(),
                entity.getTokenStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getUsedFromIp(),
                entity.getReplacedByTokenId()
        );
    }

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
        if (entities == null) {
            return java.util.Collections.emptyList();
        }

        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}