package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.domain.session.RevokedReason;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.domain.session.SessionStatus;
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
 * <p>Session is simpler than User because:
 * <ul>
 *   <li>No M2M relationships to reattach</li>
 *   <li>UUID String ID is generated at the application layer (the caller must set it before calling save)</li>
 *   <li>No soft delete</li>
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
