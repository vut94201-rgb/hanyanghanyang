package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.domain.permission.Permission;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper between {@link Permission} (domain record) and {@link PermissionEntity}.
 *
 * <p>{@code componentModel = "spring"} has been globally configured in the parent POM
 * (via the compiler argument {@code -Amapstruct.defaultComponentModel=spring}), meaning the generated
 * mapper implementation is automatically annotated with {@code @Component} and can be seamlessly injected by Spring.
 *
 * <p>{@code unmappedTargetPolicy = ERROR} has also been globally set — if any field in the
 * target is missing a mapping configuration, compilation will FAIL. This forces code to be explicit
 * and prevents accidental omissions.
 */
@Mapper
public interface PermissionMapper {

    /**
     * Entity -> Domain (record).
     * * <p>Permission is a record with 3 fields matching the names in the entity;
     * MapStruct handles the mapping automatically.
     */
    Permission toDomain(PermissionEntity entity);

    /**
     * Domain -> Entity. Used for CREATE operations (where the entity does not have an ID yet)
     * or when copying a value object into a temporary entity.
     *
     * <p>{@code @BeanMapping(ignoreByDefault = true)} -> ignores all unconfigured fields.
     * Afterwards, {@code @Mapping(target = "...", source = "...")} is used to explicitly declare each field.
     *
     * <p><b>Why audit fields (createdAt, updatedAt, version) are ignored:</b>
     * These fields are entirely managed by JPA Auditing and Hibernate; the mapper SHOULD NOT
     * touch them. If mapped, it would overwrite the authentic values of the entity loaded from the DB.
     */
    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", source = "id")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "description", source = "description")
    PermissionEntity toEntity(Permission domain);

    /**
     * Updates an existing entity loaded from the database using a domain object. Used for UPDATE
     * operations (loads the entity from DB, modifies business fields, and saves it back).
     *
     * <p>{@code @MappingTarget} instructs MapStruct: "modify the existing instance, do not instantiate
     * a new one". Audit fields remain untouched thanks to {@code ignoreByDefault = true}.
     */
    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "description", source = "description")
    void updateEntity(Permission domain, @MappingTarget PermissionEntity entity);

    // =========================================================================
    // Collection convenience
    // =========================================================================

    default Set<Permission> toDomainSet(Set<PermissionEntity> entities) {
        if (entities == null) return Collections.emptySet();
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    default List<Permission> toDomainList(List<PermissionEntity> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }
}