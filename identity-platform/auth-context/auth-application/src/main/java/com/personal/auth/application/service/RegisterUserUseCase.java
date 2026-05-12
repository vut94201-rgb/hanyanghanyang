package com.personal.auth.application.service;

import com.personal.auth.application.dto.RegisterUserCommand;
import com.personal.auth.application.port.PasswordHasher;
import com.personal.auth.domain.event.UserRegistered;
import com.personal.auth.domain.exception.AuthDomainException;
import com.personal.auth.domain.exception.AuthErrorCode;
import com.personal.auth.domain.model.User;
import com.personal.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating the "register a new user" use case.
 *
 * <p>Responsibilities (and only these):
 * <ol>
 *   <li>Enforce <b>application-level invariants</b> the domain can't see
 *       on its own: uniqueness of email and username (require repository
 *       lookups).</li>
 *   <li>Bridge transport concerns into domain: hash the raw password
 *       before handing it to {@link User#register}.</li>
 *   <li>Persist via the domain {@link UserRepository} port.</li>
 *   <li>Publish the {@link UserRegistered} domain event so other contexts
 *       (email verification, audit log, welcome notification) can react
 *       — none of which exist yet, but the seam is in place.</li>
 * </ol>
 *
 * <p>Explicitly NOT responsibilities:
 * <ul>
 *   <li>HTTP / JSON mapping — that's {@code RegisterController}.</li>
 *   <li>BCrypt details — that's {@code BCryptPasswordHasher} behind the
 *       {@link PasswordHasher} port.</li>
 *   <li>JPA / SQL — that's {@code JpaUserRepositoryAdapter} behind the
 *       {@link UserRepository} port.</li>
 *   <li>Status-transition rules ({@code activate}, {@code lock}) — those
 *       belong to the {@link User} aggregate itself.</li>
 * </ul>
 *
 * <p><b>Transactionality:</b> {@code @Transactional} wraps the whole
 * method. The unique-check + insert + event publish all commit or
 * roll back together. Note that with the default Spring transactional
 * event listener, event subscribers wired with
 * {@code @TransactionalEventListener} won't fire if the transaction
 * rolls back — which is what we want.
 *
 * <p><b>Event publishing leak:</b> using {@link ApplicationEventPublisher}
 * here couples the application layer to Spring's eventing API. That's
 * a deliberate Phần-2 trade-off (context v2 Q1 → option A). If we ever
 * need to swap event infrastructure (Kafka, an in-process bus), the
 * refactor is to define a {@code DomainEventPublisher} port in
 * {@code platform-shared} and adapt Spring behind it. One small file
 * change in this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Execute the registration. Returns the persisted {@link User} with
     * its DB-generated id populated.
     *
     * @throws AuthDomainException if email or username already exists
     */
    @Transactional
    public User register(RegisterUserCommand command) {
        // Defence in depth: these were validated at the API boundary,
        // but the use case is a reusable entry point — never trust the caller.
        requireNonBlank(command.username(), "username");
        requireNonBlank(command.email(), "email");
        requireNonBlank(command.password(), "password");

        // 1. Uniqueness checks — application concern, not domain.
        //    Race-safe enough: the unique constraints on the users table
        //    (uk_users_email, uk_users_username) are the ultimate guard.
        //    Doing the check here gives a clean error code for the common
        //    happy-path conflict instead of surfacing a DataIntegrityViolation.
        if (userRepository.existsByEmail(command.email())) {
            throw new AuthDomainException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByUsername(command.username())) {
            throw new AuthDomainException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 2. Hash via port — application layer does not import BCrypt.
        String hashed = passwordHasher.hash(command.password());

        // 3. Domain factory enforces its own invariants and seeds status=PENDING.
        User user = User.register(
                command.username(),
                command.email(),
                hashed,
                command.fullName()
        );

        // 4. Persist. Adapter writes the generated id back onto the User.
        User saved = userRepository.save(user);
        log.debug("Registered user id={} username={}", saved.getId(), saved.getUsername());


        // 5. Publish event. Subscribers fire after commit when they use
        //    @TransactionalEventListener (none defined yet — placeholder for later).
        eventPublisher.publishEvent(new UserRegistered(saved.getId(), saved.getEmail()));

        return saved;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
