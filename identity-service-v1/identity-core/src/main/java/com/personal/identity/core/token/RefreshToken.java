package com.personal.identity.core.token;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Refresh Token - 1 mắt xích trong chain rotation của 1 session.
 *
 * <p><b>Đặc điểm:</b>
 * <ul>
 *   <li>ID là String UUID, generate ở application layer.</li>
 *   <li>{@code tokenHash} là SHA-256 của raw token. DB CHỈ lưu hash, không bao
 *       giờ lưu plain token. Khi client gửi token, ta hash rồi lookup.</li>
 *   <li>{@code replacedByTokenId} trỏ tới token kế tiếp khi rotate - tạo thành
 *       linked list. Token cuối cùng trong chain có {@code replacedByTokenId == null}.</li>
 * </ul>
 */
@Getter
@Builder
@NoArgsConstructor
public class RefreshToken {

    private String id;

    private String sessionId;

    /** SHA-256 hex của raw token, KHÔNG bao giờ là plain. */
    private String tokenHash;

    private RefreshTokenStatus tokenStatus;

    private Instant createdAt;

    private Instant expiresAt;

    /** Khi nào token này được dùng để rotate. Null nếu chưa USED. */
    private Instant usedAt;

    /** IP đã rotate token này. Hữu ích cho audit khi detect reuse. */
    private String usedFromIp;

    /** Token kế tiếp trong chain, null nếu là token mới nhất. */
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

    // ============================================================
    // DOMAIN BEHAVIOR
    // ============================================================

    /**
     * Mark token đã được dùng để rotate. Set replacedByTokenId trỏ tới token mới.
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

    /** Revoke (do session revoke hoặc detect reuse). */
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
