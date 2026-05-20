package com.personal.identity.core.audit;

import java.util.List;

/**
 * <b>PORT</b> cho audit log. Infrastructure adapter persist vào bảng
 * {@code admin_audit_log}.
 *
 * <p><b>Vì sao không có {@code findAll(Pageable)}:</b> core không biết Pageable
 * (đó là Spring Data). Ta tự định nghĩa cursor/offset param đơn giản.
 *
 * <p><b>Filter:</b> chỉ 2 filter thông dụng. Cần thêm thì thêm method mới,
 * tránh "god method findByXxx" nhận hàng tá optional.
 */
public interface AuditLogRepository {

    /**
     * Lưu event. Trả về event đã persist (có id, createdAt).
     */
    AdminAuditEvent save(AdminAuditEvent event);

    /**
     * Liệt kê event mới nhất, offset-based pagination. createdAt DESC.
     *
     * @param offset bỏ qua N event đầu (>=0)
     * @param limit  số event trả về (1..100)
     */
    List<AdminAuditEvent> findRecent(int offset, int limit);

    /**
     * Filter theo target user. createdAt DESC.
     */
    List<AdminAuditEvent> findByTargetUserId(Long targetUserId, int offset, int limit);

    /**
     * Filter theo loại action. createdAt DESC.
     */
    List<AdminAuditEvent> findByActionType(AdminAction actionType, int offset, int limit);

    /**
     * Đếm tổng record (cho metadata pagination).
     */
    long count();
}
