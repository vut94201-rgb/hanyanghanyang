package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByPermissionCode(String permissionCode);

    /** Bulk lookup - 1 query với IN clause. */
    List<PermissionEntity> findAllByPermissionCodeIn(Set<String> codes);
}
