package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.RoleRepository;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.domain.audit.AdminAction;
import com.personal.identity.core.domain.audit.AdminAuditEvent;
import com.personal.identity.core.domain.audit.AuditLogRepository;
import com.personal.identity.core.domain.permission.Role;
import com.personal.identity.core.domain.permission.RoleNotFoundException;
import com.personal.identity.core.domain.session.RequestContext;
import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.token.RefreshTokenRepository;
import com.personal.identity.core.domain.user.User;
import com.personal.identity.core.domain.user.UserNotFoundException;
import com.personal.identity.core.domain.user.UserStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Use case for user administration by an admin. Consolidates all admin actions into a single class because:
 *
 * <ul>
 * <li>Splitting into 5 separate use cases -> results in repetitive audit logging code.</li>
 * <li>Shared logic: loading the actor, loading the target, audit logging, and session revocation.</li>
 * <li>Still highly testable - each method possesses a clear, distinct signature.</li>
 * </ul>
 *
 * <p><b>General Contract:</b> Every method accepts an {@code actor} (the admin triggering the action), a {@code targetUserId},
 * and a {@link RequestContext} (providing the IP for auditing). Audit logging is managed DIRECTLY WITHIN the method -
 * if a validation failure occurs, the audit record is still persisted with outcome=FAILURE prior to throwing the exception.
 *
 * <p><b>Critical Side Effect:</b> Disabling or locking a user automatically revokes ALL active sessions for that user.
 * Rationale: If an admin disables an account but its sessions remain alive → the user can continue operating until their
 * access token expires (up to 15 minutes). This delay is unacceptable for security-sensitive actions.
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
            AuditLogRepository auditLogRepository

    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogRepository = auditLogRepository;

    }
    // ------------------------------------------------------------------
    // Query
    // ------------------------------------------------------------------

    /**
     * Lists users, paginated. Not audited (read-only action).
     */
    public List<User> listUsers(
            int offset,
            int limit,
            UserStatus statusFilter
    ) {
        return userRepository.findAll(
                offset,
                limit,
                statusFilter
        );
    }

    public long countUsers(UserStatus statusFilter) {
        return userRepository.count(statusFilter);
    }

    public User findUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> UserNotFoundException.byId(id));
    }
    // ------------------------------------------------------------------
    // Mutation (Audited)
    // ------------------------------------------------------------------

    /**
     * Disables a user: status → DISABLED + revokes all active sessions.
     *
     * @return The newly disabled user (updated state).
     */
    public User disableUser(
            User actor,
            Long targetUserId,
            RequestContext context
    ) {
        return changerUserStatus(
                actor,
                targetUserId,
                UserStatus.DISABLED,
                AdminAction.DISABLE_USER,
                RevokedReason.ADMIN_REVOKED,
                context
        );

    }

    /**
     * Activates a user: status → ACTIVE. DOES NOT revoke sessions (disabled/locked users
     * have no active sessions — they were proactively revoked during the disablement phase).
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
     * Reassigns the role set for a user. Replaces the entire collection — any role not present in
     * {@code roleCodes} will be removed.
     *
     * <p>DOES NOT allow an admin to strip their own ADMIN role (safety check). Rationale: If the last
     * remaining admin removes their role, the system is left without administrators → an irrecoverable state.
     * While a production environment should ideally implement an "at least 1 ADMIN remains" validation,
     * this MVP simplifies it to a self-protection constraint.
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

        // Self-protection: if actor == target AND the ADMIN role is being removed from roleCodes → reject.
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
            throw new IllegalArgumentException("Admins are prohibited from removing their own ADMIN role");
        }

        // Resolve role codes → Role entities
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
    private User changerUserStatus(
            User actor,
            Long targetUserId,
            UserStatus newStatus,
            AdminAction adminAction,
            RevokedReason revokedReason,
            RequestContext context
    ) {
        var targetUser = userRepository.findById(targetUserId).orElse(null);
        if (Objects.isNull(targetUser)) {
            logFailure(
                    actor,
                    targetUserId,
                    null,
                    adminAction,
                    null,
                    context,
                    "User not found"
            );
            throw UserNotFoundException.byId(targetUserId);

        }
        UserStatus oldStatus = targetUser.getAccountStatus();
        switch (newStatus) {
            case DISABLED -> targetUser.disable();
            case LOCKED -> targetUser.lock();
            default -> throw new IllegalArgumentException(
                    "ACTIVE is not supported by changeStatusAndRevokeSessions; use activateUser instead");
        }
        User savedUser = userRepository.save(targetUser);
        List<Session> activeSessions = sessionRepository.findActiveByUserId(savedUser.getId());
        activeSessions.stream().peek(session -> {
            session.revoke(revokedReason);
            sessionRepository.save(session);
        });
        String payload =
                String.format(
                        "{\"oldStatus\":\"%s\",\"newStatus\":\"%s\",\"revokedSessions\":%d}",
                        oldStatus,
                        newStatus,
                        activeSessions.size()
                );
        logSuccess(
                actor,
                savedUser,
                adminAction,
                payload,
                context
        );
        return savedUser;

    }

    private void logFailure(
            User actor,
            Long targetUserId,
            String targetUsername,
            AdminAction adminAction,
            String payloadJson,
            RequestContext context,
            String errorMessage
    ) {
        auditLogRepository.save(AdminAuditEvent.failure(
                actor.getId(),
                actor.getUsername(),
                targetUserId,
                targetUsername,
                adminAction,
                payloadJson,
                context.ipAddress(),
                errorMessage
        ));

    }

    private void logSuccess(
            User actor,
            User target,
            AdminAction action,
            String payloadJson,
            RequestContext context
    ) {
        auditLogRepository.save(
                AdminAuditEvent.success(
                        actor.getId(),
                        actor.getUsername(),
                        target.getId(),
                        target.getUsername(),
                        action,
                        payloadJson,
                        context.ipAddress()
                ));
    }

    /**
     * Inline JSON serialization for a {@code Set<String>} — avoids dragging the Jackson
     * dependency into the core domain. Format: ["A","B","C"]
     */
    private String jsonStringArray(Set<String> values) {
        return values.stream().sorted().map(this::joinString).collect(Collectors.joining(
                ",",
                "[",
                "]"
        ));

    }

    private String joinString(String value) {
        return "\"" + value.replace(
                "\\",
                "\\\\"
        ).replace(
                "\"",
                "\\\""
        ) + "\"";
    }

}
