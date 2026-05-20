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
 * Spring Data repository cho {@link UserEntity}.
 *
 * <p><b>Mọi query mặc định bị filter bởi {@code @SQLRestriction("is_deleted = 0")}</b>
 * trên entity → KHÔNG trả về user đã soft-delete.
 *
 * <p><b>2 dạng method:</b>
 * <ul>
 *   <li>{@code findByXxx} - nhẹ, không fetch roles/directPermissions. Dùng khi
 *       chỉ cần check tồn tại hoặc thông tin cơ bản.</li>
 *   <li>{@code findWithAuthoritiesByXxx} - eager fetch FULL phân quyền: roles +
 *       roles.permissions + directPermissions. Dùng trong flow LOGIN và
 *       JwtAuthenticationFilter để build effective permissions.</li>
 * </ul>
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmailAddress(String emailAddress);

    boolean existsByUsername(String username);

    boolean existsByEmailAddress(String emailAddress);

    /**
     * Tìm user kèm FULL phân quyền trong 1 query.
     *
     * <p>{@code attributePaths}:
     * <ul>
     *   <li>{@code roles} - các role gán cho user</li>
     *   <li>{@code roles.permissions} - permissions từ role (RBAC)</li>
     *   <li>{@code directPermissions} - permissions gán trực tiếp ngoài role</li>
     * </ul>
     * Hibernate sinh ra 1 query với LEFT JOIN FETCH 4 bảng (users + user_roles +
     * roles + role_permissions, parallel join user_permissions). Tránh N+1.
     *
     * <p>Trade-off: query lớn (Cartesian product có thể nhiều rows). Ở scale
     * fresher demo OK, production có thể tách 2 query nếu cần.
     */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "directPermissions"})
    Optional<UserEntity> findWithAuthoritiesByUsername(String username);

    /** Tương tự nhưng lookup theo id - dùng trong JwtAuthenticationFilter. */
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "directPermissions"})
    Optional<UserEntity> findWithAuthoritiesById(Long id);

    @Query(
            "SELECT u FROM UserEntity u WHERE (:status IS NULL OR u.accountStatus = :status) ORDER BY u.createdAt DESC"
    )
    List<UserEntity> findPaginated(
            @Param("status") com.personal.identity.core.user.UserStatus status,
            Pageable pageable
    );

    @Query(
            "SELECT COUNT(u) FROM UserEntity u WHERE (:status IS NULL OR u.accountStatus = :status)"
    )
    long countWithFilter(@Param("status") com.personal.identity.core.user.UserStatus status);
}
