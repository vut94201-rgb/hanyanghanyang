package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.SessionStatus;
import com.personal.identity.infrastructure.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, String> {

    /** Lists ACTIVE sessions of a user, ordered by most recent activity first. */
    List<SessionEntity> findByUserIdAndSessionStatusOrderByLastActiveAtDesc(
            Long userId, SessionStatus status);

    /** Lists all sessions (regardless of status) belonging to a user — used for admin/audit purposes. */
    List<SessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Bulk revokes all ACTIVE sessions of a user EXCEPT for the current session.
     * Executes a single UPDATE query without loading N entities into memory.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SessionEntity s
            SET s.sessionStatus = com.personal.identity.core.domain.session.SessionStatus.REVOKED,
                s.revokedAt = :now,
                s.revokedReason = :reason
            WHERE s.userId = :userId
              AND s.id <> :currentSessionId
              AND s.sessionStatus = com.personal.identity.core.domain.session.SessionStatus.ACTIVE
            """)
    int revokeAllOtherSessions(
            @Param("userId") Long userId,
            @Param("currentSessionId") String currentSessionId,
            @Param("reason") RevokedReason reason,
            @Param("now") Instant now);

    /**
     * Bulk revokes ALL ACTIVE sessions of a user without exception.
     * Typically utilized during a global logout (logout-all).
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SessionEntity s
            SET s.sessionStatus = com.personal.identity.core.domain.session.SessionStatus.REVOKED,
                s.revokedAt = :now,
                s.revokedReason = :reason
            WHERE s.userId = :userId
              AND s.sessionStatus = com.personal.identity.core.domain.session.SessionStatus.ACTIVE
            """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("reason") RevokedReason reason,
            @Param("now") Instant now);
}