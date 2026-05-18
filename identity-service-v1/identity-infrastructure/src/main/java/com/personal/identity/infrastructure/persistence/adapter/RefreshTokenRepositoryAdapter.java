package com.personal.identity.infrastructure.persistence.adapter;

import com.personal.identity.core.token.RefreshToken;
import com.personal.identity.core.token.RefreshTokenRepository;
import com.personal.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import com.personal.identity.infrastructure.persistence.jpa.RefreshTokenJpaRepository;
import com.personal.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implements {@link RefreshTokenRepository}.
 *
 * <p>Tương tự Session, ID là String UUID generate ở application.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenMapper mapper;

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        if (token.getId() == null) {
            throw new IllegalArgumentException(
                    "RefreshToken id is required (must be UUID generated at application layer)");
        }
        RefreshTokenEntity entity;
        Optional<RefreshTokenEntity> existing = jpaRepository.findById(token.getId());
        if (existing.isPresent()) {
            entity = existing.get();
            mapper.updateEntity(token, entity);
        } else {
            entity = mapper.toEntity(token);
        }
        RefreshTokenEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<RefreshToken> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllBySessionId(String sessionId) {
        return mapper.toDomainList(jpaRepository.findBySessionIdOrderByCreatedAtDesc(sessionId));
    }

    @Override
    @Transactional
    public int revokeAllBySessionId(String sessionId) {
        return jpaRepository.revokeAllBySessionId(sessionId);
    }
}
