package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.domain.token.RefreshTokenStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.Instant;

/**
 * JPA entity for the {@code refresh_tokens} table.
 *
 * <p>The FK {@code sessionId} and self-FK {@code replacedByTokenId} use String,
 * DO NOT declare {@code @ManyToOne}. The "foreign-key-as-primitive" pattern:
 * keeps the entity lightweight, avoiding unnecessary lazy-loading dependencies.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_session", columnList = "session_id"),
                @Index(name = "idx_refresh_tokens_status", columnList = "token_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "session_id", length = 36, nullable = false)
    private String sessionId;

    /** SHA-256 hex of the raw token. UNIQUE for fast lookups. */
    @Column(name = "token_hash", length = 128, nullable = false, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_status", nullable = false, length = 20)
    private RefreshTokenStatus tokenStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_from_ip", length = 45)
    private String usedFromIp;

    /** Self-FK pointing to the subsequent token. Null if this is the newest token. */
    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;
}