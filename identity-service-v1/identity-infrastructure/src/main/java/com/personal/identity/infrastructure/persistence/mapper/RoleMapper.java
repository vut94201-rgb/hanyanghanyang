package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.role.Permission;
import com.personal.identity.core.role.Role;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper cho {@link Role} ↔ {@link RoleEntity}.
 *
 * <p>{@code uses = PermissionMapper.class}: khi map field {@code permissions}
 * (Set<PermissionEntity> ↔ Set<Permission>), MapStruct sẽ gọi {@link PermissionMapper}
 * thay vì tự generate code map - tránh duplicate logic.
 *
 * <p><b>Lưu ý LAZY:</b> {@code roleEntity.getPermissions()} là LAZY collection.
 * Mapper PHẢI được gọi trong transaction (hoặc sau khi đã fetch qua @EntityGraph).
 * Nếu không, sẽ throw LazyInitializationException khi access permissions.
 */
@Mapper(uses = PermissionMapper.class)
public interface RoleMapper {

    default Role toDomain(RoleEntity entity) {
        if (entity == null) {
            return null;
        }

        return Role.rehydrate(
                entity.getId(),
                entity.getRoleCode(),
                entity.getRoleName(),
                entity.getDescription(),
                mapPermissions(entity.getPermissions()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    Set<Permission> mapPermissions(Set<PermissionEntity> entities);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "roleName", source = "roleName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "permissions", source = "permissions")
    RoleEntity toEntity(Role domain);

    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "roleName", source = "roleName")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "permissions", source = "permissions")
    void updateEntity(Role domain, @MappingTarget RoleEntity entity);
}