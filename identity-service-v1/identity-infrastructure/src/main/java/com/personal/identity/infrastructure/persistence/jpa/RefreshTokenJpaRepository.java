package com.personal.identity.infrastructure.persistence.jpa;

import com.personal.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, String> {

    /** Lookup KEY duy nhất - bằng hash, không bao giờ bằng plain. */
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /** Tất cả token thuộc session, mới nhất trước (cho audit chain). */
    List<RefreshTokenEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Bulk revoke mọi token ACTIVE của session. KEY cho reuse detection.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RefreshTokenEntity t
            SET t.tokenStatus = com.personal.identity.core.token.RefreshTokenStatus.REVOKED
            WHERE t.sessionId = :sessionId
              AND t.tokenStatus = com.personal.identity.core.token.RefreshTokenStatus.ACTIVE
            """)
    int revokeAllBySessionId(@Param("sessionId") String sessionId);
}
