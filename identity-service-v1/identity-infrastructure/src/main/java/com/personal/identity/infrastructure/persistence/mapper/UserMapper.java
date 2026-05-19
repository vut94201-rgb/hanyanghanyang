package com.personal.identity.infrastructure.persistence.mapper;

import com.personal.identity.core.user.User;
import com.personal.identity.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

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
 * <p><b>passwordHash mapping:</b> được map giữa 2 chiều. KHÔNG bao giờ xuất hiện
 * trong DTO response - trách nhiệm tầng controller.
 *
 * <p><b>Audit + soft-delete fields:</b>
 * Map từ entity → domain để service biết khi nào tạo / sửa / xóa. Nhưng KHÔNG
 * map ngược domain → entity (vì JPA tự quản lý) - các field này không xuất hiện
 * trong toEntity/updateEntity.
 */
@Mapper(uses = {RoleMapper.class, PermissionMapper.class})
public interface UserMapper {

    /**
     * Entity → Domain. Map đầy đủ kể cả audit / soft-delete fields để service
     * có context khi cần.
     *
     * <p>Lưu ý {@code deleted} và {@code deletedAt} kế thừa từ
     * {@code SoftDeletableAuditableEntity} - MapStruct tự tìm getter ở
     * superclass nên không cần khai báo nguồn.
     */
    User toDomain(UserEntity entity);

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