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
public class RefreshToken {

    private String id;
    private String sessionId;
    private String tokenHash;
    private RefreshTokenStatus tokenStatus;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
    private String usedFromIp;
    private String replacedByTokenId;

    private RefreshToken(
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

    public static RefreshToken createNew(
            String id,
            String sessionId,
            String tokenHash,
            Instant expiresAt
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("refresh token id must not be blank");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("tokenHash must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        return new RefreshToken(
                id,
                sessionId,
                tokenHash,
                RefreshTokenStatus.ACTIVE,
                Instant.now(),
                expiresAt,
                null,
                null,
                null
        );
    }

    public static RefreshToken rehydrate(
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
        return new RefreshToken(
                id,
                sessionId,
                tokenHash,
                tokenStatus,
                createdAt,
                expiresAt,
                usedAt,
                usedFromIp,
                replacedByTokenId
        );
    }

    public void markUsed(String newTokenId, String fromIp) {
        if (tokenStatus != RefreshTokenStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot mark non-ACTIVE token as USED. Current=" + tokenStatus
            );
        }
        if (newTokenId == null || newTokenId.isBlank()) {
            throw new IllegalArgumentException("newTokenId must not be blank");
        }

        this.tokenStatus = RefreshTokenStatus.USED;
        this.usedAt = Instant.now();
        this.usedFromIp = fromIp;
        this.replacedByTokenId = newTokenId;
    }

    public void revoke() {
        if (tokenStatus == RefreshTokenStatus.REVOKED) {
            return;
        }

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

    public boolean isExpired() {
        return expiresAt == null || !expiresAt.isAfter(Instant.now());
    }
}