package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.session.RevokedReason;
import com.personal.identity.core.session.Session;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.session.SessionStatus;
import com.personal.identity.infrastructure.persistence.entity.SessionEntity;
import com.personal.identity.infrastructure.persistence.jpa.SessionJpaRepository;
import com.personal.identity.infrastructure.persistence.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implements {@link SessionRepository}.
 *
 * <p>Session đơn giản hơn User vì:
 * <ul>
 *   <li>Không có quan hệ M2M để reattach</li>
 *   <li>ID String UUID generate ở application (caller phải set trước khi gọi save)</li>
 *   <li>Không có soft delete</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRepositoryAdapter implements SessionRepository {

    private final SessionJpaRepository jpaRepository;
    private final SessionMapper mapper;

    @Override
    @Transactional
    public Session save(Session session) {
        SessionEntity entity;
        if (session.getId() == null) {
            throw new IllegalArgumentException(
                    "Session id is required (must be UUID generated at application layer)");
        }
        // Try load existing; nếu không có → CREATE, có → UPDATE
        Optional<SessionEntity> existing = jpaRepository.findById(session.getId());
        if (existing.isPresent()) {
            entity = existing.get();
            mapper.updateEntity(session, entity);
        } else {
            entity = mapper.toEntity(session);
        }
        SessionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Session> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Session> findActiveByUserId(Long userId) {
        return mapper.toDomainList(
                jpaRepository.findByUserIdAndSessionStatusOrderByLastActiveAtDesc(
                        userId, SessionStatus.ACTIVE));
    }

    @Override
    public List<Session> findAllByUserId(Long userId) {
        return mapper.toDomainList(jpaRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional
    public int revokeAllOtherSessions(Long userId, String currentSessionId, RevokedReason reason) {
        return jpaRepository.revokeAllOtherSessions(
                userId, currentSessionId, reason, Instant.now());
    }

    @Override
    @Transactional
    public int revokeAllByUserId(Long userId, RevokedReason reason) {
        return jpaRepository.revokeAllByUserId(userId, reason, Instant.now());
    }
}