package com.personal.identity.core.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <b>PORT</b> cho Permission persistence.
 */
public interface PermissionRepository {

    Permission save(Permission permission);

    Optional<Permission> findById(Long id);

    Optional<Permission> findByPermissionCode(String permissionCode);

    List<Permission> findAllByPermissionCodeIn(Set<String> codes);

    List<Permission> findAll();
}
