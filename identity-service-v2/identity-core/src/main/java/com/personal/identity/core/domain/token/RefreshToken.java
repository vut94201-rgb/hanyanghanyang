package com.personal.identity.core.domain.token;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Refresh Token - a single link within the rotation chain of a session.
 *
 * <p><b>Characteristics:</b>
 * <ul>
 * <li>ID is a String UUID, generated at the application layer.</li>
 * <li>{@code tokenHash} is the SHA-256 hash of the raw token. The DB ONLY stores the hash,
 * never the plain token. When a client submits a token, we hash it and then perform the lookup.</li>
 * <li>{@code replacedByTokenId} points to the subsequent token upon rotation - forming
 * a linked list. The final token in the chain has {@code replacedByTokenId == null}.</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
public class RefreshToken {
    private String id;

    private String sessionId;

    /** SHA-256 hex of the raw token, NEVER stored as plain text. */
    private String tokenHash;

    private RefreshTokenStatus tokenStatus;

    private Instant createdAt;

    private Instant expiresAt;

    /** Timestamp of when this token was used to rotate. Null if not yet USED. */
    private Instant usedAt;

    /** The IP address from which this token was rotated. Useful for auditing when detecting reuse. */
    private String usedFromIp;

    /** The subsequent token in the chain, null if this is the newest token. */
    private String replacedByTokenId;

    public RefreshToken(
            String id,
            String sessionId,
            String tokenHash,
            RefreshTokenStatus tokenStatus,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            String usedFromIp,
            String replacedByTokenId
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.tokenStatus = tokenStatus;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
        this.usedFromIp = usedFromIp;
        this.replacedByTokenId = replacedByTokenId;
    }

    // =========================================================================
    // DOMAIN BEHAVIOR
    // =========================================================================

    /**
     * Marks the token as having been used for rotation. Sets replacedByTokenId pointing to the new token.
     */
    public void markUsed(String newTokenId, String fromIp) {
        if (tokenStatus != RefreshTokenStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot mark non-ACTIVE token as USED. Current=" + tokenStatus);
        }
        this.tokenStatus = RefreshTokenStatus.USED;
        this.usedAt = Instant.now();
        this.usedFromIp = fromIp;
        this.replacedByTokenId = newTokenId;
    }

    /** Revokes the token (due to session revocation or reuse detection). */
    public void revoke() {
        this.tokenStatus = RefreshTokenStatus.REVOKED;
    }

    public boolean isActive() {
        return tokenStatus == RefreshTokenStatus.ACTIVE
                && expiresAt != null
                && expiresAt.isAfter(Instant.now());
    }

    public boolean isUsed() {
        return tokenStatus == RefreshTokenStatus.USED;
    }
}
