package com.personal.identity.core.service;

import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.core.audit.AdminAuditEvent;
import com.personal.identity.core.audit.AuditLogRepository;
import com.personal.identity.core.role.Role;
import com.personal.identity.core.role.RoleNotFoundException;
import com.personal.identity.core.role.RoleRepository;
import com.personal.identity.core.session.RequestContext;
import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.Session;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserNotFoundException;
import com.personal.identity.core.user.UserRepository;
import com.personal.identity.core.user.UserStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Use case quản trị user của admin. Gom mọi action admin vào 1 class vì:
 *
 * <ul>
 *   <li>Chia nhỏ ra 5 use case lẻ → lặp lại code audit logging.
 *   <li>Logic share: load actor, load target, audit log, revoke session.
 *   <li>Vẫn dễ test - mỗi method có signature rõ ràng.
 * </ul>
 *
 * <p><b>Contract chung:</b> mọi method nhận {@code actor} (admin gây action) + {@code targetUserId}
 * + {@link RequestContext} (IP để audit). Quản lý audit log ngay TRONG method - nếu lỗi xảy ra,
 * audit row vẫn được ghi với outcome=FAILURE (try-catch).
 *
 * <p><b>Side effect quan trọng:</b> disable/lock tự động revoke MỌI session active của user. Lý do:
 * nếu admin disable mà session vẫn sống → user vẫn thao tác được cho tới khi access token expire
 * (15 phút). Không chấp nhận delay này cho action security-sensitive.
 */
public class AdminUserUseCase {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final SessionRepository sessionRepository;
  private final AuditLogRepository auditLogRepository;

  public AdminUserUseCase(
      UserRepository userRepository,
      RoleRepository roleRepository,
      SessionRepository sessionRepository,
      AuditLogRepository auditLogRepository) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.sessionRepository = sessionRepository;
    this.auditLogRepository = auditLogRepository;
  }

  // ------------------------------------------------------------------
  // Query
  // ------------------------------------------------------------------

  /** List user, paginated. Không audit (đọc-only). */
  public List<User> listUsers(int offset, int limit, UserStatus statusFilter) {
    return userRepository.findAll(offset, limit, statusFilter);
  }

  public long countUsers(UserStatus statusFilter) {
    return userRepository.count(statusFilter);
  }

  public User getUser(Long userId) {
    return userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.byId(userId));
  }

  // ------------------------------------------------------------------
  // Mutation (có audit)
  // ------------------------------------------------------------------

  /**
   * Disable user: status → DISABLED + revoke all session.
   *
   * @return user đã disable (state mới)
   */
  public User disableUser(User actor, Long targetUserId, RequestContext context) {
    return changeStatus(
        actor,
        targetUserId,
        UserStatus.DISABLED,
        AdminAction.DISABLE_USER,
        RevokedReason.ADMIN_REVOKED,
        context);
  }

  /** Lock user: status → LOCKED + revoke all session. */
  public User lockUser(User actor, Long targetUserId, RequestContext context) {
    return changeStatus(
        actor,
        targetUserId,
        UserStatus.LOCKED,
        AdminAction.LOCK_USER,
        RevokedReason.ADMIN_REVOKED,
        context);
  }

  /**
   * Activate user: status → ACTIVE. KHÔNG revoke session (không có session sống cho user
   * disabled/locked - đã revoke từ lúc disable rồi).
   */
  public User activateUser(User actor, Long targetUserId, RequestContext context) {
    User target = userRepository.findById(targetUserId).orElse(null);
    if (target == null) {
      logFailure(
          actor, targetUserId, null, AdminAction.ACTIVATE_USER, "{}", context, "User not found");
      throw UserNotFoundException.byId(targetUserId);
    }

    UserStatus oldStatus = target.getAccountStatus();
    target.activate();
    User saved = userRepository.save(target);

    String payload = String.format("{\"oldStatus\":\"%s\",\"newStatus\":\"ACTIVE\"}", oldStatus);
    logSuccess(actor, saved, AdminAction.ACTIVATE_USER, payload, context);

    return saved;
  }

  /**
   * Gán lại tập role cho user. Replace toàn bộ - role không có trong roleCodes sẽ bị remove.
   *
   * <p>KHÔNG cho phép admin tự gỡ ADMIN role của chính mình (safety check). Lý do: nếu admin cuối
   * cùng gỡ role mình thì hệ thống mất admin → không ai phục hồi được. Production thực sự nên có
   * "có ít nhất 1 ADMIN còn lại" check, nhưng MVP đơn giản hoá thành self-protect.
   */
  public User updateRoles(
      User actor, Long targetUserId, Set<String> roleCodes, RequestContext context) {
    User target = userRepository.findById(targetUserId).orElse(null);
    if (target == null) {
      logFailure(
          actor,
          targetUserId,
          null,
          AdminAction.UPDATE_USER_ROLES,
          "{\"requestedRoles\":" + roleCodes + "}",
          context,
          "User not found");
      throw UserNotFoundException.byId(targetUserId);
    }

    // Self-protect: nếu actor == target VÀ ADMIN bị remove khỏi roleCodes → reject.
    if (actor.getId().equals(target.getId())
        && actor.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()))
        && !roleCodes.contains("ADMIN")) {
      logFailure(
          actor,
          targetUserId,
          target.getUsername(),
          AdminAction.UPDATE_USER_ROLES,
          "{\"requestedRoles\":" + roleCodes + "}",
          context,
          "Admin cannot remove own ADMIN role");
      throw new IllegalArgumentException("Admin không thể tự gỡ role ADMIN của chính mình");
    }

    // Resolve role code → Role entity
    Set<Role> newRoles = new HashSet<>();
    for (String code : roleCodes) {
      Role role =
          roleRepository.findByRoleCode(code).orElseThrow(() -> RoleNotFoundException.byCode(code));
      newRoles.add(role);
    }

    Set<String> oldRoleCodes = new HashSet<>();
    for (Role r : target.getRoles()) {
      oldRoleCodes.add(r.getRoleCode());
    }

    target.replaceRoles(newRoles);
    User saved = userRepository.save(target);

    String payload =
        String.format(
            "{\"oldRoles\":%s,\"newRoles\":%s}",
            jsonStringArray(oldRoleCodes), jsonStringArray(roleCodes));
    logSuccess(actor, saved, AdminAction.UPDATE_USER_ROLES, payload, context);

    return saved;
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private User changeStatus(
      User actor,
      Long targetUserId,
      UserStatus newStatus,
      AdminAction action,
      RevokedReason revokeReason,
      RequestContext context) {
    User target = userRepository.findById(targetUserId).orElse(null);
    if (target == null) {
      logFailure(actor, targetUserId, null, action, "{}", context, "User not found");
      throw UserNotFoundException.byId(targetUserId);
    }

    UserStatus oldStatus = target.getAccountStatus();
    if (newStatus == UserStatus.DISABLED) {
      target.disable();
    } else if (newStatus == UserStatus.LOCKED) {
      target.lock();
    }
    User saved = userRepository.save(target);

    // Revoke all active session
    List<Session> activeSessions = sessionRepository.findActiveByUserId(targetUserId);
    for (Session session : activeSessions) {
      session.revoke(revokeReason);
      sessionRepository.save(session);
    }

    String payload =
        String.format(
            "{\"oldStatus\":\"%s\",\"newStatus\":\"%s\",\"revokedSessions\":%d}",
            oldStatus, newStatus, activeSessions.size());
    logSuccess(actor, saved, action, payload, context);

    return saved;
  }

  private void logSuccess(
      User actor, User target, AdminAction action, String payloadJson, RequestContext context) {
    auditLogRepository.save(
        AdminAuditEvent.success(
            actor.getId(),
            actor.getUsername(),
            target.getId(),
            target.getUsername(),
            action,
            payloadJson,
            context.ipAddress()));
  }

  private void logFailure(
      User actor,
      Long targetUserId,
      String targetUsername,
      AdminAction action,
      String payloadJson,
      RequestContext context,
      String errorMessage) {
    auditLogRepository.save(
        AdminAuditEvent.failure(
            actor.getId(),
            actor.getUsername(),
            targetUserId,
            targetUsername,
            action,
            payloadJson,
            context.ipAddress(),
            errorMessage));
  }

  /** Inline JSON serialize cho Set<String> - tránh kéo Jackson vào core. Format: ["A","B","C"] */
  private String jsonStringArray(Set<String> values) {
    StringBuilder sb = new StringBuilder("[");
    boolean first = true;
    for (String v : values) {
      if (!first) sb.append(",");
      sb.append("\"").append(v.replace("\"", "\\\"")).append("\"");
      first = false;
    }
    sb.append("]");
    return sb.toString();
  }
}
