package com.personal.auth.infrastructure.persistence.repository;

import com.personal.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UserJpaEntity}.
 *
 * <p>Pure framework concern — Spring Data generates the implementation at
 * runtime from the method names. <b>Never injected directly from
 * application or api layers</b>: callers there go through the domain port
 * {@link com.personal.auth.domain.repository.UserRepository}, and the
 * adapter ({@code JpaUserRepositoryAdapter}) is what depends on this
 * interface.
 *
 * <p>Method naming follows Spring Data conventions; the framework parses
 * the names ({@code findByEmail}, {@code existsByUsername}, ...) to build
 * the JPQL/SQL automatically.
 */
public interface UserJpaSpringRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}