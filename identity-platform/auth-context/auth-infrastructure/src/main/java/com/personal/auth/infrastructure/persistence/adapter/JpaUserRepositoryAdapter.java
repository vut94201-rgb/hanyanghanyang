package com.personal.auth.infrastructure.persistence.adapter;

import com.personal.auth.domain.model.User;
import com.personal.auth.domain.repository.UserRepository;
import com.personal.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.personal.auth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.personal.auth.infrastructure.persistence.repository.UserJpaSpringRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Infrastructure adapter implementing the domain
 * {@link UserRepository} port using Spring Data JPA.
 *
 * <p>Two responsibilities only:
 * <ol>
 *   <li>Translate between {@link User} (domain) and {@link UserJpaEntity}
 *       (persistence) using {@link UserPersistenceMapper}.</li>
 *   <li>Delegate the actual SQL work to {@link UserJpaSpringRepository}.</li>
 * </ol>
 *
 * <p><b>Save semantics:</b> the domain port's contract says insert vs.
 * update is decided by whether {@code user.getId()} is null. For INSERT
 * we build a fresh entity, persist it, set the generated id back on the
 * domain object (per {@link UserRepository#save}), and return the same
 * domain reference. For UPDATE we load the managed entity, copy mutable
 * fields onto it via the mapper, and let JPA dirty-checking flush.
 */
@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {
    private final UserJpaSpringRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            // INSERT path.
            UserJpaEntity entity = mapper.toEntity(user);
            UserJpaEntity persisted = jpaRepository.save(entity);
            // Write the DB-generated id back onto the domain instance.
            // The domain User has a public setter on id specifically for
            // this trade-off (context v2 §4.3).
            user.setId(persisted.getId());
            return user;
        }

        // UPDATE path. Loading first avoids overwriting audit metadata
        // and surfaces a clear error if the row vanished between domain
        // operations.
        UserJpaEntity managed = jpaRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot update User id=" + user.getId() + " — row not found"));
        mapper.updateEntity(user, managed);
        // jpaRepository.save() is optional here (dirty checking flushes
        // automatically on transaction commit), but calling it makes the
        // intent explicit and works the same in any transaction setup.
        jpaRepository.save(managed);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }
}
