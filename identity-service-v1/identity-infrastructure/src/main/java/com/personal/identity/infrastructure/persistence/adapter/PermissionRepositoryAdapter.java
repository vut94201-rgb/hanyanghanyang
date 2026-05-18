package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.role.Permission;
import com.personal.identity.core.role.PermissionRepository;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.jpa.PermissionJpaRepository;
import com.personal.identity.infrastructure.persistence.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter implements {@link PermissionRepository} port của core.
 *
 * <p><b>Cách hoạt động:</b>
 * <ol>
 *   <li>Service tầng core gọi method của {@code PermissionRepository} (interface).</li>
 *   <li>Spring tự inject implementation này (vì {@code @Repository}).</li>
 *   <li>Adapter delegate xuống {@link PermissionJpaRepository} (Spring Data),
 *       rồi dùng {@link PermissionMapper} convert Entity ↔ Domain.</li>
 * </ol>
 *
 * <p>Đây chính là chỗ "đảo ngược dependency": core khai báo interface, adapter
 * implement. Core không biết JPA tồn tại.
 *
 * <p>{@code @Transactional(readOnly = true)} ở class-level - mọi method mặc định
 * là read-only. Method ghi (save, delete) phải override bằng {@code @Transactional}
 * (không readOnly).
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final PermissionJpaRepository jpaRepository;
    private final PermissionMapper mapper;

    @Override
    @Transactional
    public Permission save(Permission permission) {
        PermissionEntity entity;
        if (permission.id() == null) {
            // CREATE: new entity từ domain
            entity = mapper.toEntity(permission);
        } else {
            // UPDATE: load entity cũ rồi update (để giữ audit fields, version)
            entity = jpaRepository.findById(permission.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Permission not found for update, id=" + permission.id()));
            mapper.updateEntity(permission, entity);
        }
        PermissionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Permission> findByPermissionCode(String permissionCode) {
        return jpaRepository.findByPermissionCode(permissionCode).map(mapper::toDomain);
    }

    @Override
    public List<Permission> findAllByPermissionCodeIn(Set<String> codes) {
        return mapper.toDomainList(jpaRepository.findAllByPermissionCodeIn(codes));
    }

    @Override
    public List<Permission> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
}
