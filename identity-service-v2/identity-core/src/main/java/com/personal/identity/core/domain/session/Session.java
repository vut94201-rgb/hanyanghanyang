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
public class Session {

    private String id;
    private Long userId;
    private DeviceInfo deviceInfo;
    private GeoLocation location;
    private String ipAddress;
    private String userAgent;
    private SessionStatus sessionStatus;
    private Instant createdAt;
    private Instant lastActiveAt;
    private Instant expiredAt;
    private Instant revokedAt;
    private RevokedReason revokedReason;

    private Session(
            String id,
            Long userId,
            DeviceInfo deviceInfo,
            GeoLocation location,
            String ipAddress,
            String userAgent,
            SessionStatus sessionStatus,
            Instant createdAt,
            Instant lastActiveAt,
            Instant expiredAt,
            Instant revokedAt,
            RevokedReason revokedReason
    ) {
        this.id = id;
        this.userId = userId;
        this.deviceInfo = deviceInfo != null ? deviceInfo : DeviceInfo.unknown();
        this.location = location != null ? location : GeoLocation.empty();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionStatus = sessionStatus;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
    }

    public static Session createNew(
            String id,
            Long userId,
            DeviceInfo deviceInfo,
            GeoLocation location,
            String ipAddress,
            String userAgent,
            Instant expiresAt
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("session id must not be blank");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("ipAddress must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }

        Instant now = Instant.now();

        return new Session(
                id,
                userId,
                deviceInfo,
                location,
                ipAddress,
                userAgent,
                SessionStatus.ACTIVE,
                now,
                now,
                expiresAt,
                null,
                null
        );
    }

    public static Session rehydrate(
            String id,
            Long userId,
            DeviceInfo deviceInfo,
            GeoLocation location,
            String ipAddress,
            String userAgent,
            SessionStatus sessionStatus,
            Instant createdAt,
            Instant lastActiveAt,
            Instant expiredAt,
            Instant revokedAt,
            RevokedReason revokedReason
    ) {
        return new Session(
                id,
                userId,
                deviceInfo,
                location,
                ipAddress,
                userAgent,
                sessionStatus,
                createdAt,
                lastActiveAt,
                expiredAt,
                revokedAt,
                revokedReason
        );
    }

    public void revoke(RevokedReason reason) {
        if (sessionStatus != SessionStatus.ACTIVE) {
            return;
        }
        if (reason == null) {
            throw new IllegalArgumentException("revokedReason must not be null");
        }

        this.sessionStatus = SessionStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    public void markExpired() {
        if (sessionStatus != SessionStatus.ACTIVE) {
            return;
        }

        this.sessionStatus = SessionStatus.EXPIRED;
        this.revokedReason = RevokedReason.EXPIRED;
    }

    public void touch() {
        if (sessionStatus != SessionStatus.ACTIVE) {
            return;
        }

        this.lastActiveAt = Instant.now();
    }

    public boolean isActive() {
        return sessionStatus == SessionStatus.ACTIVE
                && expiredAt != null
                && expiredAt.isAfter(Instant.now());
    }
}