package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.token.RefreshTokenStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity cho bảng {@code refresh_tokens}.
 *
 * <p>FK {@code sessionId} và self-FK {@code replacedByTokenId} dùng String,
 * KHÔNG khai báo {@code @ManyToOne}. Pattern "foreign-key-as-primitive":
 * giữ entity gọn, không phụ thuộc lazy-load không cần.
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

    /** SHA-256 hex của raw token. UNIQUE để lookup nhanh. */
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

    /** Self-FK trỏ tới token kế tiếp. Null khi là token mới nhất. */
    @Column(name = "replaced_by_token_id", length = 36)
    private String replacedByTokenId;
}
