package com.personal.identity.core.domain.audit;

import java.util.List;

/**
 * <b>PORT</b> for the audit log. The infrastructure adapter will persist this into the
 * {@code admin_audit_log} table.
 *
 * <p><b>Why is there no {@code findAll(Pageable)}:</b> The core domain is unaware of {@code Pageable}
 * (which belongs to Spring Data). We define simple cursor/offset parameters ourselves.
 *
 * <p><b>Filters:</b> Only two common filters are provided. If more are needed, add new methods
 * to avoid a "god method findByXxx" that accepts dozens of optional parameters.
 */
public interface AuditLogRepository {

    /**
     * Saves the event. Returns the persisted event (populated with id and createdAt).
     */
    AdminAuditEvent save(AdminAuditEvent event);

    /**
     * Lists the most recent events using offset-based pagination. Ordered by createdAt DESC.
     *
     * @param offset Skips the first N events (>= 0).
     * @param limit  Number of events to return (1..100).
     */
    List<AdminAuditEvent> findRecent(int offset, int limit);

    /**
     * Filters by target user. Ordered by createdAt DESC.
     */
    List<AdminAuditEvent> findByTargetUserId(Long targetUserId, int offset, int limit);

    /**
     * Filters by action type. Ordered by createdAt DESC.
     */
    List<AdminAuditEvent> findByActionType(AdminAction actionType, int offset, int limit);

    /**
     * Counts total records (used for pagination metadata).
     */
    long count();
}