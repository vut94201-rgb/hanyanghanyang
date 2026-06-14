package com.personal.identity.core.domain.session;

import com.personal.identity.core.domain.shared.enums.CodeEnum;

/**
 * Session status. Matches {@code CHECK (session_status IN ('A','R','E'))}.
 *
 * <p>State machine:
 *
 * <pre>
 *     ACTIVE → REVOKED  (logout / token reuse / admin action)
 *     ACTIVE → EXPIRED  (qua expires_at, cleanup job đánh dấu)
 * </pre>
 *
 * No reverse transitions: a REVOKED/EXPIRED cannot return to ACTIVE.
 */
public enum SessionStatus implements CodeEnum<String> {
  ACTIVE("A"),
  REVOKED("R"),
  EXPIRED("E");
  private final String code;

  private SessionStatus(String code) {
    this.code = code;
  }

  @Override
  public String getCode() {
    return code;
  }

  public static SessionStatus fromCode(String code) {
    for (SessionStatus sessionStatus : SessionStatus.values()) {
      if (sessionStatus.code.equalsIgnoreCase(code)) {
        return sessionStatus;
      }
    }
    throw new IllegalArgumentException("SessionStatus code not found");
  }
}
