package com.personal.auth.domain.model;

import com.personal.auth.domain.exception.AuthDomainException;
import com.personal.auth.domain.exception.AuthErrorCode;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class User {

    /**
     * DB-generated identifier. Null until first persisted.
     */
    @Setter(AccessLevel.PUBLIC)   // repository adapter sets after insert
    private Long id;

    private String username;
    private String email;

    /**
     * BCrypt (or equivalent) hash. Never plain text.
     */
    private String password;

    private String fullName;
    private UserStatus status;

    // ------------------------------------------------------------------
    // Factory
    // ------------------------------------------------------------------

    /**
     * Create a new user in {@link UserStatus#PENDING} state.
     *
     * <p>Validates only what the domain itself owns (non-null/non-blank fields).
     * Uniqueness checks (email/username already taken) are an application-layer
     * concern — they require a repository lookup.
     *
     * @param username       non-blank
     * @param email          non-blank (format validation happens at the API boundary
     *                       via {@code @Email} on the request DTO)
     * @param hashedPassword already-hashed password — domain stores as-is
     * @param fullName       nullable
     * @return a new User with id=null and status=PENDING
     */
    public static User register(String username,
                                String email,
                                String hashedPassword,
                                String fullName) {
        requireNonBlank(username, "username");
        requireNonBlank(email, "email");
        requireNonBlank(hashedPassword, "hashedPassword");

        return User.builder()
                .id(null)
                .username(username)
                .email(email)
                .password(hashedPassword)
                .fullName(fullName)
                .status(UserStatus.PENDING)
                .build();
    }

    /**
     * Reconstruct a User from persistence. Used by the repository adapter
     * when mapping from {@code UserJpaEntity}. Skips factory validation —
     * the data is assumed to already be consistent (it survived the DB
     * constraints).
     */
    public static User restore(Long id,
                               String username,
                               String email,
                               String hashedPassword,
                               String fullName,
                               UserStatus status) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password(hashedPassword)
                .fullName(fullName)
                .status(status)
                .build();
    }

    // ------------------------------------------------------------------
    // State transitions
    // ------------------------------------------------------------------

    /**
     * Move from PENDING to ACTIVE. Idempotent: ACTIVE → ACTIVE is a no-op.
     */
    public void activate() {
        if (this.status == UserStatus.LOCKED) {
            throw new AuthDomainException(AuthErrorCode.USER_LOCKED,
                    "Cannot activate a locked user");
        }
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Lock the account. Allowed from any state.
     */
    public void lock() {
        this.status = UserStatus.LOCKED;
    }

    /**
     * Replace the password hash. The caller must hash beforehand.
     *
     * @param newHashedPassword already-hashed new password
     */
    public void changePassword(String newHashedPassword) {
        requireNonBlank(newHashedPassword, "newHashedPassword");
        this.password = newHashedPassword;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
