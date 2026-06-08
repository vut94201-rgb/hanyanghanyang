package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {

    /** Lookup by unique KEY - via hash, never by plain text. */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /** All tokens belonging to a session, ordered by most recent first (for the audit chain). */
    List<RefreshTokenEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Bulk revokes all ACTIVE tokens of a session. Key mechanism for reuse detection.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenEntity t
            SET t.tokenStatus = com.personal.identity.core.domain.token.RefreshTokenStatus.REVOKED
            WHERE t.sessionId = :sessionId
              AND t.tokenStatus = com.personal.identity.core.domain.token.RefreshTokenStatus.ACTIVE
            """)
    int revokeAllBySessionId(@Param("sessionId") String sessionId);
}