package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.domain.permission.Permission;
import com.personal.identity.core.domain.permission.Role;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.*;

import java.util.Set;
/**
 * MapStruct mapper for {@link Role} ⟷ {@link RoleEntity}.
 *
 * <p>{@code uses = PermissionMapper.class}: When mapping the {@code permissions} field
 * ({@code Set<PermissionEntity>} ⟷ {@code Set<Permission>}), MapStruct will invoke
 * {@link PermissionMapper} instead of auto-generating custom mapping code — avoiding duplicate logic.
 *
 * <p><b>Note on LAZY fetching:</b> {@code roleEntity.getPermissions()} returns a LAZY collection.
 * The mapper MUST be invoked within a transaction (or after the collection has been eagerly fetched via
 * an {@code @EntityGraph}). Otherwise, it will throw a {@code LazyInitializationException} upon accessing the permissions.
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