package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.session.DeviceType;
import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.SessionStatus;
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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity cho bảng {@code sessions}.
 *
 * <p><b>Đặc điểm:</b>
 * <ul>
 *   <li>PK String UUID, generate ở application (không qua DB sequence).</li>
 *   <li>KHÔNG kế thừa base entity - có timestamps riêng theo lifecycle session.</li>
 *   <li>DeviceInfo/GeoLocation từ domain được flatten thành cột riêng - mapper
 *       layer (C3) sẽ gom lại thành value object khi convert sang domain.</li>
 * </ul>
 */
@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(name = "idx_sessions_user", columnList = "user_id"),
                @Index(name = "idx_sessions_user_status", columnList = "user_id,session_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class SessionEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ---- Device info (flatten) ----

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "browser_name", length = 50)
    private String browserName;

    @Column(name = "browser_version", length = 50)
    private String browserVersion;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ---- Network + GeoLocation ----

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "city_name", length = 100)
    private String cityName;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    // ---- Lifecycle ----

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private SessionStatus sessionStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 50)
    private RevokedReason revokedReason;
}
