package com.personal.auth.domain.repository;

import com.personal.auth.domain.model.User;

import java.util.Optional;

/**
 * Domain port for User persistence.
 *
 * <p>This is the <b>outbound port</b> in Hexagonal Architecture terminology.
 * The domain layer defines what it needs (this interface); infrastructure
 * provides an adapter that implements it (e.g. {@code JpaUserRepositoryAdapter}
 * in {@code auth-infrastructure}).
 *
 * <p>Methods deliberately use domain types ({@link User}) — never JPA entities
 * or {@code Page}/{@code Pageable} from Spring Data. The domain stays unaware
 * of how persistence is implemented.
 */
public interface UserRepository {
    /**
     * Persist a new user or update an existing one.
     *
     * <p>If {@link User#getId()} is null, this is an insert — the adapter
     * assigns the generated ID back onto the User instance before returning.
     * If non-null, it's an update.
     *
     * @return the same User reference, with id populated if it was an insert
     */
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
