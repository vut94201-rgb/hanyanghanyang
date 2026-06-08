package com.personal.identity.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data repository for {@link AuditLogEntity}.
 *
 * <p>Native Spring Data methods are only sufficient for insert/findById. More complex queries
 * use JPQL in custom defined methods.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("SELECT a FROM AuditLogEntity a ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findRecent(Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.targetUserId = :targetUserId ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findByTargetUserId(@Param("targetUserId") Long targetUserId, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.actionType = :actionType ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findByActionType(@Param("actionType") AdminAction actionType, Pageable pageable);
}