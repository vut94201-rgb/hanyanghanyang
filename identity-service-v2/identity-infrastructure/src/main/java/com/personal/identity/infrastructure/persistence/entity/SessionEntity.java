package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.domain.session.DeviceType;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.SessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code sessions} table.
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>PK is a String UUID, generated at the application layer (does not use a DB sequence).
 *   <li>Does NOT inherit from a base entity - has its own timestamps tracking the session
 *       lifecycle.
 *   <li>DeviceInfo/GeoLocation from the domain are flattened into separate columns - the mapper
 *       layer (C3) will reconstruct them back into value objects when converting to the domain.
 * </ul>
 */
@Entity
@Table(
    name = "sessions",
    indexes = {
      @Index(name = "idx_sessions_user", columnList = "user_id"),
      @Index(name = "idx_sessions_user_status", columnList = "user_id,session_status")
    })
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

  @Column(name = "device_type")
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

  @Column(name = "session_status")
  private SessionStatus sessionStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_active_at", nullable = false)
  private Instant lastActiveAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_reason")
  private RevokedReason revokedReason;
}
