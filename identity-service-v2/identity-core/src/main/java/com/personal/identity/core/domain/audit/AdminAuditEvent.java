package com.personal.identity.core.domain.audit;

import java.time.Instant;

/**
 * An audit log record. An immutable value object, possessing no behavior.
 *
 * @param id             PK, null when not yet persisted.
 * @param actorUserId    The ID of the admin performing the action.
 * @param actorUsername  The admin's username (denormalized for audit queries).
 * @param targetUserId   The user impacted by the action, null if the action has no specific target user.
 * @param targetUsername The target's username (denormalized).
 * @param actionType     The {@link AdminAction} enum.
 * @param payloadJson    Serialized JSON of the changes (old/new state).
 * @param ipAddress      The admin's IP address at the time of the action.
 * @param outcome        {@link Outcome#SUCCESS} or {@link Outcome#FAILURE}.
 * @param errorMessage   Only present when outcome=FAILURE.
 * @param createdAt      Set when persisted.
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
    public enum Outcome implements com.personal.identity.core.domain.shared.enums.CodeEnum<String> {
        SUCCESS("S"),
        FAILURE("F");

        private final String code;

        Outcome(String code) {
            this.code = code;
        }

        @Override
        public String getCode() {
            return code;
        }
    }

    /**
     * Builder for the most common event: SUCCESS.
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
     * Builder for the FAILURE event (e.g., an admin attempts an action on a non-existent user).
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