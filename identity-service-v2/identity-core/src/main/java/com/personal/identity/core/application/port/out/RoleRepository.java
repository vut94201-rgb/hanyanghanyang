package com.personal.identity.core.application.port.out;

import com.personal.identity.core.domain.permission.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <b>PORT</b> cho Role persistence.
 */
public interface RoleRepository {
    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByRoleCode(String code);

    /**
     * Lists all roles for the admin UI to render dropdowns.
     */
    List<Role> findAll();

    /**
     * Bulk lookups multiple codes at once. Useful when assigning multiple roles to a user
     * (e.g., AssignRolesRequest contains 3 role codes -> executes 1 query instead of 3).
     */
    List<Role> findAllByRoleCodeIn(Set<String> codes);


    boolean existsByRoleCode(String code);
}
