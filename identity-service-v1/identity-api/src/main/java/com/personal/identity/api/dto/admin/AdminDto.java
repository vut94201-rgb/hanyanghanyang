package com.personal.identity.api.dto.admin;

import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.core.audit.AdminAuditEvent;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gom mọi DTO của admin API vào 1 file (mỗi DTO là static record).
 *
 * <p><b>Vì sao gom 1 file:</b> 5 record nhỏ, dùng cùng 1 controller. Tách 5
 * file riêng tạo noise trong project explorer.
 */
public final class AdminDto {

    private AdminDto() {} // utility holder, không instantiate

    // ------------------------------------------------------------------
    // Response DTOs
    // ------------------------------------------------------------------

    /**
     * User row trong list. Compact - chỉ field admin cần thấy trong table.
     */
    public record UserSummary(
            Long id,
            String username,
            String emailAddress,
            String fullName,
            UserStatus accountStatus,
            Set<String> roleCodes,
            Instant createdAt
    ) {
        public static UserSummary from(User user) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getEmailAddress(),
                    user.getFullName(),
                    user.getAccountStatus(),
                    user.getRoles().stream().map(r -> r.getRoleCode())
                            .collect(Collectors.toCollection(java.util.LinkedHashSet::new)),
                    user.getCreatedAt()
            );
        }
    }

    /**
     * Page wrapper.
     */
    public record PageResponse<T>(
            List<T> items,
            long total,
            int offset,
            int limit
    ) {
    }

    /**
     * Audit log entry trong list.
     */
    public record AuditEventResponse(
            Long id,
            Long actorUserId,
            String actorUsername,
            Long targetUserId,
            String targetUsername,
            AdminAction actionType,
            String payloadJson,
            String ipAddress,
            AdminAuditEvent.Outcome outcome,
            String errorMessage,
            Instant createdAt
    ) {
        public static AuditEventResponse from(AdminAuditEvent event) {
            return new AuditEventResponse(
                    event.id(),
                    event.actorUserId(),
                    event.actorUsername(),
                    event.targetUserId(),
                    event.targetUsername(),
                    event.actionType(),
                    event.payloadJson(),
                    event.ipAddress(),
                    event.outcome(),
                    event.errorMessage(),
                    event.createdAt()
            );
        }
    }

    // ------------------------------------------------------------------
    // Request DTOs
    // ------------------------------------------------------------------

    /**
     * Body cho POST /admin/users/{id}/roles
     */
    public record UpdateRolesRequest(
            @NotEmpty(message = "roleCodes không được rỗng - dùng list chứa 1 role tối thiểu")
            @Size(max = 10, message = "Không gán quá 10 role một lúc")
            Set<String> roleCodes
    ) {
    }
}
