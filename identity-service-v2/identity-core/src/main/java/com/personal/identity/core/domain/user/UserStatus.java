package com.personal.identity.core.domain.user;

import com.personal.identity.core.domain.shared.enums.CodeEnum;

/**
 * Account status. The {@link com.personal.identity.core.domain.shared.enums.CodeEnum#getCode() code}
 * values match the CHECK constraint in migration V2:
 *
 * <pre>{@code CHECK (account_status IN ('A','D','L'))}</pre>
 *
 * <p>Distinctions:
 *
 * <ul>
 *   <li>{@link #ACTIVE} - operating normally, login allowed.
 *   <li>{@link #DISABLED} - manually disabled by an admin, login NOT allowed. Can be re-enabled
 *       later.
 *   <li>{@link #LOCKED} - automatically locked due to too many failed login attempts, or other
 *       security reasons. Distinction from DISABLED: lock is triggered by the system, disable is
 *       triggered by an admin.
 * </ul>
 *
 * <p>Soft-delete is NOT handled here - that uses a separate {@code is_deleted} flag. A user can be
 * both {@code ACTIVE} and soft-deleted (but default queries will filter them out).
 */
public enum UserStatus implements CodeEnum<String> {
  ACTIVE("A"),
  LOCKED("L"),
  DISABLED("D");
  private final String code;

  private UserStatus(String code) {
    this.code = code;
  }

  @Override
  public String getCode() {
    return code;
  }

  public static UserStatus fromCode(String code) {
    for (UserStatus userStatus : UserStatus.values()) {
      if (userStatus.code.equalsIgnoreCase(code)) {
        return userStatus;
      }
    }
    throw new IllegalArgumentException("invalid user status code");
  }
}
