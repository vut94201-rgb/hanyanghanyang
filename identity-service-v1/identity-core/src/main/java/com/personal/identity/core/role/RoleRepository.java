package com.personal.identity.core.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * <b>PORT</b> cho Role persistence.
 */
public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByRoleCode(String roleCode);

    /**
     * Bulk lookup theo nhiều code 1 lần. Hữu ích khi gán nhiều role cho user
     * (vd: AssignRolesRequest có 3 role codes → 1 query thay vì 3).
     */
    List<Role> findAllByRoleCodeIn(Set<String> roleCodes);

    /** List tất cả role để admin UI render dropdown. */
    List<Role> findAll();

    boolean existsByRoleCode(String roleCode);
}
