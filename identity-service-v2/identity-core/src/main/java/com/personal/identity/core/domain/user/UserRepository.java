package com.personal.identity.core.domain.user;

import java.util.List;
import java.util.Optional;
/**
 * <b>PORT</b> - an interface that the infrastructure must implement (via
 * {@code UserRepositoryAdapter} using JPA underneath).
 *
 * <p>The Core only knows "there is a way to save / query User", it does NOT know if the database is Oracle,
 * does NOT know about Hibernate, and does NOT know about Spring Data. This is Dependency Inversion:
 * the abstraction (this interface) belongs to the core, while the implementation belongs to the infrastructure.
 *
 * <p>Conventions:
 * <ul>
 * <li>Search methods return {@link Optional} - to avoid scattered null checks.</li>
 * <li>{@link #save(User)} is used for both CREATE and UPDATE - JPA distinguishes them automatically
 * based on whether {@code @Id == null} or not.</li>
 * <li>{@link #softDelete(User)} is explicit so the service knows this is a soft delete,
 * not an actual physical DELETE.</li>
 * </ul>
 */
public interface UserRepository {
    /**
     * Save the user. Returns the persisted user (with id, createdAt, and updatedAt set).
     */
    User save(User user);

    /**
     * Find by PK. The adapter will automatically ignore soft-deleted users
     * (thanks to {@code @SQLRestriction("is_deleted = 0")} on the entity).
     */
    Optional<User> findById(Long id);

    /** Find by username (unique). */
    Optional<User> findByUsername(String username);

    /** Find by email (unique). */
    Optional<User> findByEmailAddress(String emailAddress);

    /** Check existence - lighter than calling {@code findByXxx().isPresent()}. */
    boolean existsByUsername(String username);

    boolean existsByEmailAddress(String emailAddress);

    /**
     * Soft delete: UPDATE is_deleted=1, deleted_at=now. DOES NOT physically delete the record.
     * After calling this, default queries will no longer return this user.
     */
    void softDelete(User user);

    List<User> findAll(int offset, int limit, UserStatus statusFilter);

    long count(UserStatus statusFilter);
}
