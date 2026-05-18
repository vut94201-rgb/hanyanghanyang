package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByRoleCode(String roleCode);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<RoleEntity> findWithPermissionsByRoleCode(String roleCode);

    List<RoleEntity> findAllByRoleCodeIn(Set<String> roleCodes);

    @EntityGraph(attributePaths = {"permissions"})
    List<RoleEntity> findAllWithPermissionsBy();

    boolean existsByRoleCode(String roleCode);
}
