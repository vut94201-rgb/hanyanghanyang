package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserRepository;
import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import com.personal.identity.infrastructure.persistence.entity.UserEntity;
import com.personal.identity.infrastructure.persistence.jpa.PermissionJpaRepository;
import com.personal.identity.infrastructure.persistence.jpa.RoleJpaRepository;
import com.personal.identity.infrastructure.persistence.jpa.UserJpaRepository;
import com.personal.identity.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter implements {@link UserRepository}.
 *
 * <p><b>findBy* methods:</b> dùng phiên bản "WithAuthorities" của JPA repository
 * (có @EntityGraph) để eager load roles + permissions + directPermissions trong
 * 1 query. Mapper sau đó access các collection không bị LazyInitializationException.
 *
 * <p><b>save:</b> phải reattach RoleEntity và PermissionEntity (cho directPermissions)
 * để Hibernate xem chúng là managed - tránh duplicate insert.
 *
 * <p><b>softDelete:</b> dùng @SQLDelete của entity - gọi {@code jpaRepository.delete}
 * thực ra chạy UPDATE chứ không phải DELETE thật. {@code @SQLRestriction} đảm bảo
 * user đã soft-delete sẽ KHÔNG xuất hiện trong query mặc định.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final RoleJpaRepository roleJpaRepository;
    private final PermissionJpaRepository permissionJpaRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity;
        if (user.getId() == null) {
            // INSERT path: build entity mới từ domain, reattach roles/perms
            // để Hibernate xem chúng là managed (tránh duplicate insert role/perm).
            entity = mapper.toEntity(user);
            entity.setRoles(reattachRoles(entity.getRoles()));
            entity.setDirectPermissions(reattachPermissions(entity.getDirectPermissions()));
        } else {
            // UPDATE path: load managed entity, mapper update field scalar
            // (username, password, ...) - KHÔNG động vào roles/directPermissions
            // (xem UserMapper.updateEntity javadoc). Giữ nguyên managed collection
            // để tránh "Detached entity ... uninitialized version" do auto-flush.
            entity = jpaRepository.findWithAuthoritiesById(user.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "User not found for update, id=" + user.getId()));
            mapper.updateEntity(user, entity);
        }

        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findWithAuthoritiesById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findWithAuthoritiesByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailAddress(String emailAddress) {
        return jpaRepository.findByEmailAddress(emailAddress).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmailAddress(String emailAddress) {
        return jpaRepository.existsByEmailAddress(emailAddress);
    }

    @Override
    @Transactional
    public void softDelete(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("Cannot soft-delete user without id");
        }
        // Load entity trước - @SQLDelete cần entity được tracked
        UserEntity entity = jpaRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "User not found for soft-delete, id=" + user.getId()));
        // delete() trigger @SQLDelete → UPDATE is_deleted=1, deleted_at=SYSTIMESTAMP
        jpaRepository.delete(entity);
    }

    // ---- Reattach helpers ----

    private Set<RoleEntity> reattachRoles(Set<RoleEntity> input) {
        if (Objects.isNull(input)  || input.isEmpty()) return new HashSet<>();
        Set<Long> ids = input.stream()
                .map(RoleEntity::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(roleJpaRepository.findAllById(ids));
    }

    private Set<PermissionEntity> reattachPermissions(Set<PermissionEntity> input) {
        if (Objects.isNull(input) || input.isEmpty()) return new HashSet<>();
        Set<Long> ids = input.stream()
                .map(PermissionEntity::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return new HashSet<>();
        return new HashSet<>(permissionJpaRepository.findAllById(ids));
    }
}