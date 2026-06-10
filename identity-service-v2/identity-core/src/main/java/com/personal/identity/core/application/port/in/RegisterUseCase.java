package com.personal.identity.core.application.port.in;

import com.personal.identity.core.application.port.out.RoleRepository;
import com.personal.identity.core.application.port.out.UserRepository;
import com.personal.identity.core.application.security.PasswordEncoder;
import com.personal.identity.core.domain.permission.Role;
import com.personal.identity.core.domain.permission.RoleNotFoundException;
import com.personal.identity.core.domain.user.DuplicateEmailException;
import com.personal.identity.core.domain.user.DuplicateUsernameException;
import com.personal.identity.core.domain.user.User;

/**
 * Use case: Register a new user.
 *
 * <p><b>Execution Flow:</b>
 * <ol>
 * <li>Validate input (sanity checks, not business validation — which is handled at the DTO layer).</li>
 * <li>Verify username uniqueness ⟿ if it exists, throw {@link DuplicateUsernameException}.</li>
 * <li>Verify email uniqueness ⟿ if it exists, throw {@link DuplicateEmailException}.</li>
 * <li>Hash the raw password using BCrypt.</li>
 * <li>Look up the default role {@code USER} ⟿ if not found, throw {@link RoleNotFoundException}
 * (this represents a system configuration error, not a user error).</li>
 * <li>Build a new {@link User} entity with an ACTIVE status, assign the default role, and persist it.</li>
 * </ol>
 *
 * <p><b>No automatic login post-registration.</b> Returns the persisted user; the client must explicitly invoke
 * the login endpoint. Rationale: Certain UX flows require email verification prior to the initial login.
 * Decoupling these two use cases maximizes architectural flexibility.
 *
 * <p><b>Delegating unique constraint race conditions:</b> If two registration requests for the same username
 * arrive concurrently, both might evaluate {@code existsByUsername == false}. One will succeed, while the other
 * fails with a {@code DataIntegrityViolationException} at the JPA layer. This is an extremely rare occurrence
 * in practice (given the UX of registration forms), and the infrastructure adapter will wrap this into a domain
 * exception later if necessary. Avoid over-engineering (e.g., locking via SELECT FOR UPDATE for a negligible edge case).
 *
 * <p><b>Why pure Java instead of {@code @Service}:</b> To maintain a 100% framework-agnostic core domain.
 * The infrastructure adapter in the identity-api module will instantiate this bean via a {@code @Bean} method
 * within {@code UseCaseConfig}.
 */
public class RegisterUseCase {

    /**
     * The default role assigned to newly registered users.
     * This must strictly align with the seed data in {@code V3__seed_default_data.sql}.
     */
    private static final String DEFAULT_ROLE_CODE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user with the default role.
     *
     * @param command The new user payload (pre-validated at the DTO layer prior to reaching this core service).
     * @return The persisted User entity (populated with id, createdAt, and the USER role).
     * @throws DuplicateUsernameException if the username is already taken.
     * @throws DuplicateEmailException    if the email is already registered.
     * @throws RoleNotFoundException      if the foundational USER seed role is missing (system error).
     */
    public User execute(RegisterCommand command) {
        // 1. Sanity check - acts as a defensive guard in case the service is invoked programmatically
        //    (e.g., via tests) bypassing DTO validation. DOES NOT replace @Valid at the controller layer.
        validateInput(command);

        // 2. Check for duplicates - failing fast with a user-friendly exception instead of letting
        //    JPA throw a raw DataIntegrityViolationException containing SQL errors.
        if (userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(command.username());
        }
        if (userRepository.existsByEmailAddress(command.emailAddress())) {
            throw new DuplicateEmailException(command.emailAddress());
        }

        // 3. Hash password
        String passwordHash = passwordEncoder.encode(command.rawPassword());

        // 4. Lookup default role USER
        Role defaultRole = roleRepository.findByRoleCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> RoleNotFoundException.byCode(DEFAULT_ROLE_CODE));

        // 5. Construct the new user via the domain factory - the ACTIVE status is implicitly set inside createNew
        User user = User.createNew(
                command.username(),
                command.emailAddress(),
                passwordHash,
                command.fullName()
        );
        user.addRole(defaultRole);

        return userRepository.save(user);
    }

    private void validateInput(RegisterCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("RegisterCommand must not be null");
        }
        if (command.username() == null || command.username().isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (command.emailAddress() == null || command.emailAddress().isBlank()) {
            throw new IllegalArgumentException("emailAddress must not be blank");
        }
        if (command.rawPassword() == null || command.rawPassword().isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }
        // fullName is permitted to be null/blank - it is an optional user provision.
    }

    /**
     * Input payload for {@link #execute(RegisterCommand)}. Modeled as a record for immutability and conciseness.
     *
     * <p>Declared as a nested record since it is exclusively utilized by this use case — eliminating the need
     * for a separate file. This pattern is more lightweight than maintaining a standalone {@code RegisterCommand.java} class.
     *
     * @param username     Unique username, pre-trimmed and lowercased at the DTO layer.
     * @param emailAddress Unique email, pre-normalized at the DTO layer.
     * @param rawPassword  Plain text password — to be securely hashed within the use case.
     * @param fullName     Display name, nullable.
     */
    public record RegisterCommand(
            String username,
            String emailAddress,
            String rawPassword,
            String fullName
    ) {
    }
}
