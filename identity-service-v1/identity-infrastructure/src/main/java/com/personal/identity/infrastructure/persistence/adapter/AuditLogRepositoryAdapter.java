package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.audit.AdminAction;
import com.personal.identity.core.audit.AdminAuditEvent;
import com.personal.identity.core.audit.AuditLogRepository;
import com.personal.identity.infrastructure.persistence.entity.AuditLogEntity;
import com.personal.identity.infrastructure.persistence.jpa.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter implement {@link AuditLogRepository} - port của core - dùng JPA.
 *
 * <p><b>Vì sao không có mapper riêng (MapStruct):</b> chỉ 11 field flat
 * record → mapper riêng overkill. Inline 2 method nhỏ trực tiếp ở đây, dễ
 * debug. Nếu sau này thêm complex mapping (vd: lazy load actor User entity)
 * thì tách MapStruct sau.
 */
@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    @Override
    public AdminAuditEvent save(AdminAuditEvent event) {
        AuditLogEntity entity = toEntity(event);
        AuditLogEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<AdminAuditEvent> findRecent(int offset, int limit) {
        Pageable pageable = pageOf(offset, limit);
        return jpaRepository.findRecent(pageable).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AdminAuditEvent> findByTargetUserId(Long targetUserId, int offset, int limit) {
        return jpaRepository.findByTargetUserId(targetUserId, pageOf(offset, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AdminAuditEvent> findByActionType(AdminAction actionType, int offset, int limit) {
        return jpaRepository.findByActionType(actionType, pageOf(offset, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    /**
     * Convert offset/limit (port API) sang Spring Pageable.
     * Page = offset / limit (giả sử limit cố định mỗi page).
     */
    private Pageable pageOf(int offset, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        return PageRequest.of(safeOffset / safeLimit, safeLimit);
    }

    private AuditLogEntity toEntity(AdminAuditEvent event) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setActorUserId(event.actorUserId());
        entity.setActorUsername(event.actorUsername());
        entity.setTargetUserId(event.targetUserId());
        entity.setTargetUsername(event.targetUsername());
        entity.setActionType(event.actionType());
        entity.setPayloadJson(event.payloadJson());
        entity.setIpAddress(event.ipAddress());
        entity.setOutcome(event.outcome());
        entity.setErrorMessage(event.errorMessage());
        // KHÔNG set createdAt - @CreationTimestamp tự set lúc INSERT.
        return entity;
    }

    private AdminAuditEvent toDomain(AuditLogEntity entity) {
        return new AdminAuditEvent(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorUsername(),
                entity.getTargetUserId(),
                entity.getTargetUsername(),
                entity.getActionType(),
                entity.getPayloadJson(),
                entity.getIpAddress(),
                entity.getOutcome(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }
}
