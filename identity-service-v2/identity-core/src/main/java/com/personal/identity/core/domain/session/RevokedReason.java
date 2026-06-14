package com.personal.identity.core.domain.session;

import com.personal.identity.core.domain.shared.enums.CodeEnum;

/**
 * Reasons for session revocation. Important for audit logs and attack detection.
 *
 * <p>Special {@link #TOKEN_REUSE}: when a used refresh token is detected this is a very strong
 * indicator of an attack - revoke the entire token family and optionally send an email alert to the
 * user
 */
public enum RevokedReason implements CodeEnum<String> {
  /** Admin revoke session/user */
  ADMIN_REVOKED("AR"),
  /** The user logged out */
  LOGOUT("L"),
  /** The user manually logs out of another device from sessions list */
  USER_ACTION("UA"),
  /** The token has expired naturally */
  EXPIRED("E"),
  /** Token reuse detected → revoke all family. */
  TOKEN_REUSE("TR");
  private final String code;

  private RevokedReason(String code) {
    this.code = code;
  }

  @Override
  public String getCode() {
    return code;
  }

  public static RevokedReason fromCode(String code) {
    for (RevokedReason revokedReason : RevokedReason.values()) {
      if (revokedReason.getCode().equals(code)) {
        return revokedReason;
      }
    }
    throw new IllegalArgumentException("Invalid RevokedReason code");
  }
}
