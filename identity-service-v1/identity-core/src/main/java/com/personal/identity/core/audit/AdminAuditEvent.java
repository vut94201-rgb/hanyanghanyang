package com.personal.identity.core.audit;

import java.time.Instant;

/**
 * Một record audit log. Immutable value object, không có behavior.
 *
 * @param id              PK, null khi chưa persist
 * @param actorUserId     id admin thực hiện
 * @param actorUsername   username admin (denormalized cho audit query)
 * @param targetUserId    user bị tác động, null nếu action không target user cụ thể
 * @param targetUsername  username target (denormalized)
 * @param actionType      enum {@link AdminAction}
 * @param payloadJson     JSON serialize của thay đổi (old/new state)
 * @param ipAddress       IP của admin lúc gọi action
 * @param outcome         {@link Outcome#SUCCESS} hoặc {@link Outcome#FAILURE}
 * @param errorMessage    chỉ có khi outcome=FAILURE
 * @param createdAt       set khi persist
 */
public record AdminAuditEvent(
        Long id,
        Long actorUserId,
        String actorUsername,
        Long targetUserId,
        String targetUsername,
        AdminAction actionType,
        String payloadJson,
        String ipAddress,
        Outcome outcome,
        String errorMessage,
        Instant createdAt
) {

    public enum Outcome {
        SUCCESS,
        FAILURE
    }

    /**
     * Builder cho event SUCCESS phổ biến nhất.
     */
    public static AdminAuditEvent success(
            Long actorUserId,
            String actorUsername,
            Long targetUserId,
            String targetUsername,
            AdminAction actionType,
            String payloadJson,
            String ipAddress
    ) {
        return new AdminAuditEvent(
                null, actorUserId, actorUsername, targetUserId, targetUsername,
                actionType, payloadJson, ipAddress, Outcome.SUCCESS, null, null
        );
    }

    /**
     * Builder cho event FAILURE (vd: admin thử action lên user không tồn tại).
     */
    public static AdminAuditEvent failure(
            Long actorUserId,
            String actorUsername,
            Long targetUserId,
            String targetUsername,
            AdminAction actionType,
            String payloadJson,
            String ipAddress,
            String errorMessage
    ) {
        return new AdminAuditEvent(
                null, actorUserId, actorUsername, targetUserId, targetUsername,
                actionType, payloadJson, ipAddress, Outcome.FAILURE, errorMessage, null
        );
    }
}
