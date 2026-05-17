package com.personal.identity.core.session;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Aggregate Session = 1 lần login = 1 record.
 *
 * <p><b>Đặc điểm:</b>
 * <ul>
 *   <li>ID là {@code String} (UUID format) - generate tại application layer
 *       qua {@code UUID.randomUUID().toString()}. KHÔNG dùng SEQUENCE vì cần
 *       unique-globally và không guessable (dùng làm "family ID" cho refresh token).</li>
 *   <li>KHÔNG kế thừa AuditableEntity - có timestamps riêng theo lifecycle
 *       ({@code createdAt}, {@code lastActiveAt}, {@code expiresAt}, {@code revokedAt}).</li>
 *   <li>KHÔNG soft delete - dùng cột {@code sessionStatus} để track lifecycle.</li>
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

    /** UUID format. Generate ở application, KHÔNG dùng DB sequence. */
    private String id;

    private Long userId;

    private DeviceInfo deviceInfo;

    private GeoLocation location;

    private String ipAddress;

    /** Raw User-Agent, lưu để debug khi parse sai. */
    private String userAgent;

    private SessionStatus sessionStatus;

    private Instant createdAt;

    /** Update mỗi request authenticated qua JwtAuthenticationFilter. */
    private Instant lastActiveAt;

    private Instant expiresAt;

    /** Null khi chưa revoke. */
    private Instant revokedAt;

    /** Null khi chưa revoke. */
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
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.revokedReason = revokedReason;
    }

    // ============================================================
    // DOMAIN BEHAVIOR
    // ============================================================

    /**
     * Revoke session với lý do. Idempotent: revoke 1 session đã REVOKED chỉ no-op,
     * không throw. Lưu revokedAt và revokedReason để audit.
     */
    public void revoke(RevokedReason reason) {
        if (sessionStatus != SessionStatus.ACTIVE) return;
        this.sessionStatus = SessionStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revokedReason = reason;
    }

    /** Đánh dấu expired (cleanup job). */
    public void markExpired() {
        if (sessionStatus != SessionStatus.ACTIVE) return;
        this.sessionStatus = SessionStatus.EXPIRED;
        this.revokedReason = RevokedReason.EXPIRED;
    }

    /** Cập nhật last_active mỗi request có token hợp lệ. */
    public void touch() {
        this.lastActiveAt = Instant.now();
    }

    /** Session còn được dùng không. */
    public boolean isActive() {
        return sessionStatus == SessionStatus.ACTIVE
                && expiresAt != null
                && expiresAt.isAfter(Instant.now());
    }
}
