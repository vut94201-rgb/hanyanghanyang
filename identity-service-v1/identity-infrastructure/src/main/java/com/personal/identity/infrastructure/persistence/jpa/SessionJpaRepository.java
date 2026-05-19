package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.SessionStatus;
import com.personal.identity.infrastructure.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, String> {

    /** List session ACTIVE của user, mới nhất trước. */
    List<SessionEntity> findByUserIdAndSessionStatusOrderByLastActiveAtDesc(
            Long userId, SessionStatus status);

    /** Tất cả session (mọi trạng thái) - cho admin/audit. */
    List<SessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Bulk revoke tất cả session ACTIVE của user TRỪ session hiện tại.
     * 1 query UPDATE, không load N entity.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SessionEntity s
            SET s.sessionStatus = com.personal.identity.core.session.SessionStatus.REVOKED,
                s.revokedAt = :now,
                s.revokedReason = :reason
            WHERE s.userId = :userId
              AND s.id <> :currentSessionId
              AND s.sessionStatus = com.personal.identity.core.session.SessionStatus.ACTIVE
            """)
    int revokeAllOtherSessions(
            @Param("userId") Long userId,
            @Param("currentSessionId") String currentSessionId,
            @Param("reason") RevokedReason reason,
            @Param("now") Instant now);

    /**
     * Bulk revoke TẤT CẢ session ACTIVE của user (không loại trừ ai).
     * Dùng cho logout-all.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SessionEntity s
            SET s.sessionStatus = com.personal.identity.core.session.SessionStatus.REVOKED,
                s.revokedAt = :now,
                s.revokedReason = :reason
            WHERE s.userId = :userId
              AND s.sessionStatus = com.personal.identity.core.session.SessionStatus.ACTIVE
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("reason") RevokedReason reason,
            @Param("now") Instant now);
}