package com.personal.identity.infrastructure.persistence.mapper;


import com.personal.identity.core.role.Permission;
import com.personal.identity.core.role.Role;
import com.personal.identity.core.user.User;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import com.personal.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.*;

import java.util.Set;

/**
 * MapStruct mapper cho {@link User} ↔ {@link UserEntity}.
 *
 * <p><b>2 nguồn permission:</b>
 * <ul>
 *   <li>{@code roles} (qua bảng user_roles)</li>
 *   <li>{@code directPermissions} (qua bảng user_permissions)</li>
 * </ul>
 *
 * <p>Mapper PHẢI gọi trong transaction sau khi entity được load với
 * {@code @EntityGraph(attributePaths = {"roles", "roles.permissions", "directPermissions"})}.
 * Nếu không, LazyInitializationException.
 *
 * <h2>Vì sao {@link #toDomain} viết tay (default method)</h2>
 *
 * <p>{@code User} domain bỏ {@code @Builder} + public all-args constructor để
 * đóng gói state. Chỉ dựng được qua {@code User.rehydrate(...)}. MapStruct không
 * tự generate được pattern này, nên {@code toDomain} dùng {@code default} method
 * gọi trực tiếp {@code User.rehydrate}.
 *
 * <p>Hai method {@link #mapRoles} và {@link #mapDirectPermissions} để MapStruct
 * tự generate impl, sử dụng {@link RoleMapper} + {@link PermissionMapper} qua
 * {@code uses = {...}}.
 *
 * <p><b>passwordHash:</b> được map 2 chiều. KHÔNG bao giờ xuất hiện trong DTO
 * response - trách nhiệm tầng controller.
 */
@Mapper(uses = {RoleMapper.class, PermissionMapper.class})
public interface UserMapper {

    /**
     * Entity → Domain. Dùng {@link User#rehydrate} - bypass business validation
     * vì state đã được DB đảm bảo (constraint + flyway migration).
     */
    default User toDomain(UserEntity entity) {
        if (entity == null) return null;

        return User.rehydrate(
                entity.getId(),
                entity.getUsername(),
                entity.getEmailAddress(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getAccountStatus(),
                mapRoles(entity.getRoles()),
                mapDirectPermissions(entity.getDirectPermissions()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isDeleted(),
                entity.getDeletedAt()
        );
    }

    /**
     * MapStruct tự generate, dùng {@link RoleMapper#toDomain(RoleEntity)} cho từng phần tử.
     * Trả về Set rỗng nếu input null (do MapStruct convention).
     */
    Set<Role> mapRoles(Set<RoleEntity> entities);

    /** Tương tự {@link #mapRoles}, dùng {@link PermissionMapper}. */
    Set<Permission> mapDirectPermissions(Set<PermissionEntity> entities);

    /**
     * Domain → Entity, dùng cho CREATE.
     * KHÔNG map audit fields, KHÔNG map deleted/deletedAt - những thứ này
     * JPA/Hibernate tự quản lý.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "emailAddress", source = "emailAddress")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "accountStatus", source = "accountStatus")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "directPermissions", source = "directPermissions")
    UserEntity toEntity(User domain);

    /**
     * Update entity đã tồn tại. KHÔNG update id (PK không đổi).
     *
     * <p><b>Vì sao KHÔNG map roles + directPermissions ở đây:</b> mapper sẽ REPLACE
     * cả collection bằng entities mới build từ domain (qua RoleMapper/PermissionMapper).
     * Các entity mới này là DETACHED (không có @Version từ DB) - khi Hibernate auto-flush
     * sẽ ném {@code PropertyValueException: uninitialized version value}.
     * <p>Giải pháp: giữ nguyên managed collection ở {@code @MappingTarget entity}.
     * Nếu use case cần đổi roles thì viết method riêng (vd: {@code updateRoles})
     * dùng {@code entity.getRoles().clear()} rồi {@code addAll(reattachedRoles)}.
     * <p>Hiện tại các use case sửa user (change-password) KHÔNG đổi roles, nên bỏ map
     * là an toàn nhất.
     */
    @BeanMapping(ignoreByDefault = true, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "username", source = "username")
    @Mapping(target = "emailAddress", source = "emailAddress")
    @Mapping(target = "passwordHash", source = "passwordHash")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "accountStatus", source = "accountStatus")
    void updateEntity(User domain, @MappingTarget UserEntity entity);
}