package com.personal.identity.infrastructure.persistence.entity;

import com.personal.identity.core.domain.audit.AdminAction;
import com.personal.identity.core.domain.audit.AdminAuditEvent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Entity mapping to the {@code admin_audit_log} table.
 *
 * <p><b>Why it does NOT extend AuditableEntity:</b> This IS an audit table. An audit log,
 * by its very nature, does not require fields like createdBy, updatedBy, or version — no one should ever
 * update an audit row after its creation (it is append-only).
 *
 * <p><b>CreatedAt:</b> Utilizes Hibernate's {@code @CreationTimestamp} because this value
 * is set by the database via {@code DEFAULT SYSTIMESTAMP}. While it could be set manually in
 * the code, delegating to the database-side prevents clock drift between application instances.
 *
 * <p><b>Lombok visibility:</b> {@code @NoArgsConstructor(access = AccessLevel.PUBLIC)} is used for JPA + adapter
 * direct instantiation. Since the audit log does not possess any domain behaviors/methods, encapsulation guards are unnecessary.
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
