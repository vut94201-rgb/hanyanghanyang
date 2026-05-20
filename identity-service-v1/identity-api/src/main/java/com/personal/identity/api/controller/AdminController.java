package com.personal.identity.api.controller;

import com.personal.identity.api.dto.admin.AdminDto;
import com.personal.identity.api.dto.admin.AdminDto.AuditEventResponse;
import com.personal.identity.api.dto.admin.AdminDto.PageResponse;
import com.personal.identity.api.dto.admin.AdminDto.UpdateRolesRequest;
import com.personal.identity.api.dto.admin.AdminDto.UserSummary;
import com.personal.identity.api.security.AuthenticatedUser;
import com.personal.identity.api.util.RequestContextExtractor;
import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.core.audit.AuditLogRepository;
import com.personal.identity.core.service.AdminUserUseCase;
import com.personal.identity.core.session.RequestContext;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserRepository;
import com.personal.identity.core.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin API. Mọi endpoint require {@code ROLE_ADMIN}.
 *
 * <h2>Authorization</h2>
 *
 * <p>{@code @PreAuthorize("hasRole('ADMIN')")} ở class level - Spring Security
 * sẽ check ngay sau khi JWT filter set Authentication, TRƯỚC khi vào method.
 * User không có ADMIN → 403 Forbidden, không vào tới method.
 *
 * <p><b>Lưu ý setup:</b> {@code @PreAuthorize} chỉ active khi class config
 * có {@code @EnableMethodSecurity}. Patch sẽ thêm vào SecurityConfig.
 *
 * <h2>Audit</h2>
 *
 * <p>Mutation endpoint tự log qua {@link AdminUserUseCase}. Query endpoint
 * (list, get detail, view audit) KHÔNG log để tránh meta-noise.
 *
 * <h2>Self-target protection</h2>
 *
 * <p>Admin có thể disable/lock CHÍNH MÌNH (use case không chặn). Trade-off:
 * <ul>
 *   <li>Pro: nếu admin nghi tài khoản bị compromise, disable ngay được.</li>
 *   <li>Con: lỡ tay disable mình thì cần admin khác cứu.</li>
 * </ul>
 * Chỉ tự gỡ ADMIN role là bị chặn (xem AdminUserUseCase.updateRoles).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserUseCase adminUserUseCase;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final RequestContextExtractor contextExtractor;

    // ==================================================================
    // User management
    // ==================================================================

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserSummary>> listUsers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) UserStatus status
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);

        List<UserSummary> items = adminUserUseCase.listUsers(safeOffset, safeLimit, status).stream()
                .map(UserSummary::from)
                .toList();
        long total = adminUserUseCase.countUsers(status);

        return ResponseEntity.ok(new PageResponse<>(items, total, safeOffset, safeLimit));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserSummary> getUser(@PathVariable Long userId) {
        User user = adminUserUseCase.getUser(userId);
        return ResponseEntity.ok(UserSummary.from(user));
    }

    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<UserSummary> disableUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request
    ) {
        User actor = loadActor(principal);
        RequestContext context = contextExtractor.extract(request);
        User updated = adminUserUseCase.disableUser(actor, userId, context);
        return ResponseEntity.ok(UserSummary.from(updated));
    }

    @PostMapping("/users/{userId}/lock")
    public ResponseEntity<UserSummary> lockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request
    ) {
        User actor = loadActor(principal);
        RequestContext context = contextExtractor.extract(request);
        User updated = adminUserUseCase.lockUser(actor, userId, context);
        return ResponseEntity.ok(UserSummary.from(updated));
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<UserSummary> activateUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request
    ) {
        User actor = loadActor(principal);
        RequestContext context = contextExtractor.extract(request);
        User updated = adminUserUseCase.activateUser(actor, userId, context);
        return ResponseEntity.ok(UserSummary.from(updated));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<UserSummary> updateRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRolesRequest body,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletRequest request
    ) {
        User actor = loadActor(principal);
        RequestContext context = contextExtractor.extract(request);
        User updated = adminUserUseCase.updateRoles(actor, userId, body.roleCodes(), context);
        return ResponseEntity.ok(UserSummary.from(updated));
    }

    // ==================================================================
    // Audit log
    // ==================================================================

    /**
     * Liệt kê audit log. Filter optional:
     *   - targetUserId: chỉ event nhắm vào user này
     *   - actionType: chỉ event loại này
     * Nếu cả 2 filter cùng có, targetUserId thắng (filter quan trọng hơn).
     */
    @GetMapping("/audit-log")
    public ResponseEntity<PageResponse<AuditEventResponse>> listAuditLog(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(required = false) AdminAction actionType
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);

        List<AuditEventResponse> items;
        if (targetUserId != null) {
            items = auditLogRepository.findByTargetUserId(targetUserId, safeOffset, safeLimit).stream()
                    .map(AuditEventResponse::from)
                    .toList();
        } else if (actionType != null) {
            items = auditLogRepository.findByActionType(actionType, safeOffset, safeLimit).stream()
                    .map(AuditEventResponse::from)
                    .toList();
        } else {
            items = auditLogRepository.findRecent(safeOffset, safeLimit).stream()
                    .map(AuditEventResponse::from)
                    .toList();
        }

        long total = auditLogRepository.count();
        return ResponseEntity.ok(new PageResponse<>(items, total, safeOffset, safeLimit));
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Load User entity từ principal (JWT chỉ chứa id + username + roles).
     * Cần entity thật để có Set<Role> đầy đủ + Long id cho audit.
     */
    private User loadActor(AuthenticatedUser principal) {
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal not found in DB: " + principal.userId()));
    }
}
