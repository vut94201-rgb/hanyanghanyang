package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data repository cho {@link AuditLogEntity}.
 *
 * <p>Native Spring Data method chỉ đủ cho insert/findById. Query phức tạp hơn
 * dùng JPQL trong method tự define.
 */
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("SELECT a FROM AuditLogEntity a ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findRecent(Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.targetUserId = :targetUserId ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findByTargetUserId(@Param("targetUserId") Long targetUserId, Pageable pageable);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.actionType = :actionType ORDER BY a.createdAt DESC")
    List<AuditLogEntity> findByActionType(@Param("actionType") AdminAction actionType, Pageable pageable);
}
