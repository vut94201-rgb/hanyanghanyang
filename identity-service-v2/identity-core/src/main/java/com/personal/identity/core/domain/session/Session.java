package com.personal.identity.core.domain.session;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Aggregate Session = 1 login session = 1 record.
 *
 * <p><b>Characteristics:</b>
 * <ul>
 *   <li>ID is a {@code String} (UUID format) - generate  at the application layer
 *       via {@code UUID.randomUUID().toString()}. DO NOT use a SEQUENCE because it needs
 *       to be unique-globally and unguessable (used "family ID" for refresh token).</li>
 *   <li>Does Not inherit from  AuditableEntity - has it own  timestamps base on  lifecycle
 *       ({@code createdAt}, {@code lastActiveAt}, {@code expiresAt}, {@code revokedAt}).</li>
 *   <li>NO  soft delete - use the {@code sessionStatus} column to  track lifecycle.</li>
 * </ul>
 *
 * <p><b>State machine:</b>
 * <pre>
 *   ACTIVE ──→ REVOKED  (logout / token_reuse / user_action)
 *   ACTIVE ──→ EXPIRED  (qua expires_at)
 * </pre>
 */
@Getter
@Builder
@NoArgsConstructor
public class Session {

    /**
     * UUID format. Generated at application layer, DO NOT use Database sequence
     */
    private String id;
    private Long userId;
    private DeviceInfo deviceInfo;
    private GeoLocation location;
    private String ipAddress;

    /**
     * Raw user-agent, stored for debugging for parsing fails.
     */
    private String userAgent;
    private SessionStatus sessionStatus;
    private Instant createdAt;

    /**
     * Update  on every  authenticated  request via JwtAuthenticationFilter
     */
    private Instant lastActiveAt;
    private Instant expiredAt;

    /**
     * Null if not yet revoke
     */
    private Instant revokedAt;

    /**
     * Null if not yet revoke
     */
    private RevokedReason revokedReason;


    // ----- AllArgs constructor cho @Builder -----
    public Session(
            String id,
            Long userId,
            DeviceInfo deviceInfo,
            GeoLocation location,
            String ipAddress,
            String userAgent,
            SessionStatus sessionStatus,
            Instant createdAt,
            Instant lastActiveAt,
            Instant expiresAt,
            Instant revokedAt,
            RevokedReason revokedReason
    ) {
        this.id = id;
        this.userId = userId;
        this.deviceInfo = deviceInfo;
        this.location = location;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionStatus = sessionStatus;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.expiredAt = expiresAt;
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
    }

    // ============================================================
    // DOMAIN BEHAVIOR
    // ============================================================

    /**
     * Revokes the session with a reason. Idempotent: revoking an already REVOKED session
     * results in a no-op and does not throw an exception. Saves revokedAt and revokedReason for auditing.
     */
    public void revoke(RevokedReason reason) {
        if (sessionStatus != SessionStatus.ACTIVE) return;
        this.sessionStatus = SessionStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    /**
     * Marks the session as expired (cleanup job).
     */
    public void markExpired() {
        if (sessionStatus != SessionStatus.ACTIVE) return;
        this.sessionStatus = SessionStatus.EXPIRED;
        this.revokedReason = RevokedReason.EXPIRED;
    }

    /**
     * Updates last_active for every request with a valid token.
     */
    public void touch() {
        this.lastActiveAt = Instant.now();
    }

    /**
     * Checks if the session is still active / usable.
     */
    public boolean isActive() {
        return this.sessionStatus == SessionStatus.ACTIVE
                && this.expiredAt != null
                && this.expiredAt.isAfter(Instant.now());
    }

}
