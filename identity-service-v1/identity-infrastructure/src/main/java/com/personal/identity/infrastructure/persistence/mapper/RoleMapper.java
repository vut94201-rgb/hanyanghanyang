package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.role.Role;
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

    Role toDomain(RoleEntity entity);

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

    default Set<Role> toDomainSet(Set<RoleEntity> entities) {
        if (entities == null) return java.util.Collections.emptySet();
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    default List<Role> toDomainList(List<RoleEntity> entities) {
        if (entities == null) return java.util.Collections.emptyList();
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }
}
