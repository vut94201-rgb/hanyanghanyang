package com.personal.identity.core.domain.token;

import com.personal.identity.core.domain.shared.enums.CodeEnum;

/**
 * Status of a refresh token within the rotation chain.
 *
 * <p>State machine:
 *
 * <pre>
 * ACTIVE ——> USED     (already rotated into a new token, pointed to by replacedByTokenId)
 * ACTIVE ——> REVOKED  (session revoked / reuse detected)
 * USED   ——> REVOKED  (reuse detected -> immediately mark the USED token as REVOKED for audit clarity)
 * </pre>
 *
 * <p><b>Important for reuse detection:</b> If a client sends a refresh token and the lookup reveals
 * its status is USED, this is an indicator of a stolen token — revoke the entire token family.
 */
public enum RefreshTokenStatus implements CodeEnum<String> {
  ACTIVE("A"),
  REVOKED("R"),
  USED("U");
  private final String code;

  private RefreshTokenStatus(String code) {
    this.code = code;
  }

  @Override
  public String getCode() {
    return code;
  }

  public static RefreshTokenStatus fromCode(String code) {
    for (RefreshTokenStatus refreshTokenStatus : RefreshTokenStatus.values()) {
      if (refreshTokenStatus.code.equalsIgnoreCase(code)) {
        return refreshTokenStatus;
      }
    }
    throw new IllegalArgumentException("Invalid refresh token status code");
  }
}
