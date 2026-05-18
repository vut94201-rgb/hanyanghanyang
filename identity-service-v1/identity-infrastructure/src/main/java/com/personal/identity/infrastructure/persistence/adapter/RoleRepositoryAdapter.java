package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.role.Role;
import com.personal.identity.core.role.RoleRepository;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import com.personal.identity.infrastructure.persistence.jpa.PermissionJpaRepository;
import com.personal.identity.infrastructure.persistence.jpa.RoleJpaRepository;
import com.personal.identity.infrastructure.persistence.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter implements {@link RoleRepository}.
 *
 * <p><b>Chú ý quan trọng cho save:</b>
 * Khi domain Role có permissions, mỗi Permission đã có id (đã tồn tại trong DB).
 * Nếu mapper convert sang PermissionEntity new instance, Hibernate sẽ coi đó là
 * "transient" và cố INSERT lại → vi phạm UNIQUE constraint trên permission_code.
 *
 * <p>Giải pháp: trước khi save Role, load lại các PermissionEntity từ DB theo id
 * (qua {@link PermissionJpaRepository#findAllById}). Như vậy Hibernate biết chúng
 * là "managed" entity, chỉ INSERT vào bảng nối role_permissions, không touch
 * bảng permissions.
 *
 * <p>Đây là pattern "reattach managed entities before persist" - rất phổ biến
 * khi làm việc với many-to-many không cascade.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final RoleMapper mapper;

    @Override
    @Transactional
    public Role save(Role role) {
        RoleEntity entity;
        if (role.getId() == null) {
            entity = mapper.toEntity(role);
            // Sau khi mapper toEntity, permissions là Set<PermissionEntity> KHÔNG managed.
            // Reattach để Hibernate không cố INSERT lại bảng permissions.
            entity.setPermissions(reattachPermissions(entity.getPermissions()));
        } else {
            entity = jpaRepository.findById(role.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Role not found for update, id=" + role.getId()));
            mapper.updateEntity(role, entity);
            entity.setPermissions(reattachPermissions(entity.getPermissions()));
        }
        RoleEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    /**
     * Load lại các PermissionEntity từ DB theo id để Hibernate xem là managed.
     * Bỏ qua permission có id null (shouldn't happen, nhưng an toàn).
     */
    private Set<PermissionEntity> reattachPermissions(Set<PermissionEntity> input) {
        if (input == null || input.isEmpty()) return new HashSet<>();
        Set<Long> ids = input.stream()
                .map(PermissionEntity::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(permissionJpaRepository.findAllById(ids));
    }

    @Override
    public Optional<Role> findById(Long id) {
        // Eager load permissions để mapper không lazy-init exception
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * Tìm theo code, eager fetch permissions để mapper không bị LAZY.
     * (Dùng findWithPermissionsByRoleCode thay vì findByRoleCode)
     */
    @Override
    public Optional<Role> findByRoleCode(String roleCode) {
        return jpaRepository.findWithPermissionsByRoleCode(roleCode).map(mapper::toDomain);
    }

    @Override
    public List<Role> findAllByRoleCodeIn(Set<String> roleCodes) {
        // Note: findAllByRoleCodeIn KHÔNG có @EntityGraph - nếu service cần permissions,
        // phải dùng findWithPermissionsByRoleCode từng cái hoặc viết custom query.
        // Hiện tại bulk lookup này dùng cho việc "gán role cho user" - chỉ cần id và code.
        return mapper.toDomainList(jpaRepository.findAllByRoleCodeIn(roleCodes));
    }

    @Override
    public List<Role> findAll() {
        return mapper.toDomainList(jpaRepository.findAllWithPermissionsBy());
    }

    @Override
    public boolean existsByRoleCode(String roleCode) {
        return jpaRepository.existsByRoleCode(roleCode);
    }
}
