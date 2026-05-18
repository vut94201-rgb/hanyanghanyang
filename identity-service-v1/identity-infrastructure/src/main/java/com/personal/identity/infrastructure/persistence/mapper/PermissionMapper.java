package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.role.Permission;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapStruct mapper giữa {@link Permission} (record domain) và {@link PermissionEntity}.
 *
 * <p>{@code componentModel = "spring"} đã được set global ở parent pom
 * (compiler arg {@code -Amapstruct.defaultComponentModel=spring}), nên mapper
 * sinh ra là {@code @Component} - Spring tự inject được.
 *
 * <p>{@code unmappedTargetPolicy = ERROR} đã set global - nếu có field nào ở
 * target không được map, compile sẽ FAIL. Ép code phải explicit, không sót.
 */
@Mapper
public interface PermissionMapper {

    /**
     * Entity → Domain (record).
     * Permission là record với 3 field khớp tên với entity, MapStruct map tự động.
     */
    Permission toDomain(PermissionEntity entity);

    /**
     * Domain → Entity. Dùng cho CREATE (entity chưa có id) hoặc khi cần copy
     * value object về entity tạm.
     *
     * <p>{@code @BeanMapping} với {@code ignoreByDefault = true} → bỏ qua mọi
     * field không khai báo. Sau đó {@code @Mapping(target="...", source="...")}
     * khai báo explicit từng field.
     *
     * <p>Vì sao ignore audit fields ({@code createdAt}, {@code updatedAt}, {@code version}):
     * những field này do JPA Auditing và Hibernate tự quản lý - mapper KHÔNG nên
     * touch. Nếu set, sẽ ghi đè giá trị thật của entity đã load từ DB.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "description", source = "description")
    PermissionEntity toEntity(Permission domain);

    /**
     * Update entity đã tồn tại từ domain object. Dùng cho UPDATE (entity load
     * từ DB, sửa field nghiệp vụ, save lại).
     *
     * <p>{@link MappingTarget} bảo MapStruct: "đừng new instance, hãy ghi đè
     * vào instance này". Audit fields không touch (nhờ ignoreByDefault).
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "description", source = "description")
    void updateEntity(Permission domain, @MappingTarget PermissionEntity entity);

    // ---- Collection convenience ----

    default Set<Permission> toDomainSet(Set<PermissionEntity> entities) {
        if (entities == null) return java.util.Collections.emptySet();
        return entities.stream().map(this::toDomain).collect(Collectors.toSet());
    }

    default List<Permission> toDomainList(List<PermissionEntity> entities) {
        if (entities == null) return java.util.Collections.emptyList();
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }
}
