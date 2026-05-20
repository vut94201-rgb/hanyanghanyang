package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.core.audit.AdminAuditEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity mapping bảng {@code admin_audit_log}.
 *
 * <p><b>Vì sao KHÔNG extends AuditableEntity:</b> đây LÀ bảng audit. Audit log
 * tự thân không cần createdBy/updatedBy/version - không ai update audit row
 * sau khi tạo (append-only).
 *
 * <p><b>CreatedAt:</b> dùng {@code @CreationTimestamp} của Hibernate vì giá
 * trị này được DB set qua DEFAULT SYSTIMESTAMP. Cũng có thể set thủ công trong
 * code - chọn DB-side để tránh clock drift giữa app instance.
 *
 * <p><b>Lombok visibility:</b> {@code @NoArgsConstructor(access = PUBLIC)} cho JPA + adapter
 * tạo instance trực tiếp. Audit log không có domain method nào nên không cần guard.
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "admin_audit_log_gen")
    @SequenceGenerator(name = "admin_audit_log_gen", sequenceName = "admin_audit_log_seq",
            allocationSize = 50)
    @Column(name = "id")
    private Long id;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_username", length = 64)
    private String targetUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 64)
    private AdminAction actionType;

    @Lob
    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 16)
    private AdminAuditEvent.Outcome outcome;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
