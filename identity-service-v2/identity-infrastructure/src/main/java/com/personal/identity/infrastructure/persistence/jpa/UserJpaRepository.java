package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for {@link UserEntity}.
 *
 * <p><b>All queries are filtered by default via {@code @SQLRestriction("is_deleted = 0")}</b>
 * on the entity -> DOES NOT return soft-deleted users.
 *
 * <p><b>2 types of methods:</b>
 * <ul>
 * <li>{@code findByXxx} - lightweight, does not fetch roles/directPermissions. Used when
 * only checking for existence or basic information.</li>
 * <li>{@code findWithAuthoritiesByXxx} - eagerly fetches FULL authorization data: roles +
 * roles.permissions + directPermissions. Used in the LOGIN flow and
 * JwtAuthenticationFilter to build effective permissions.</li>
 * </ul>
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAddress(String emailAddress);

    boolean existsByUsername(String username);

    boolean existsByEmailAddress(String emailAddress);

    /**
     * Finds a user with FULL authorization data in a single query.
     *
     * <p>{@code attributePaths}:
     * <ul>
     * <li>{@code roles} - roles assigned to the user</li>
     * <li>{@code roles.permissions} - permissions derived from roles (RBAC)</li>
     * <li>{@code directPermissions} - permissions granted directly, independent of roles</li>
     * </ul>
     *
     * <p>Hibernate generates 1 query using LEFT JOIN FETCH across 4 tables (users + user_roles +
     * roles + role_permissions, parallel join user_permissions). Prevents N+1 query problems.
     *
     * <p>Trade-off: large query (Cartesian product may result in many rows). Acceptable for a
     * demo scale, but in production, it might be split into 2 queries if necessary.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "directPermissions"})
    Optional<UserEntity> findWithAuthoritiesByUsername(String username);

    /**
     * Similar, but looks up by id - used in JwtAuthenticationFilter.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "directPermissions"})
    Optional<UserEntity> findWithAuthoritiesById(Long id);

    @Query(
            "SELECT u FROM UserEntity u WHERE (:status IS NULL OR u.accountStatus = :status) ORDER BY u.createdAt DESC"
    )
    List<UserEntity> findPaginated(
            @Param("status") com.personal.identity.core.domain.user.UserStatus status,
            Pageable pageable
    );

    @Query(
            "SELECT COUNT(u) FROM UserEntity u WHERE (:status IS NULL OR u.accountStatus = :status)"
    )
    long countWithFilter(@Param("status") com.personal.identity.core.domain.user.UserStatus status);
}