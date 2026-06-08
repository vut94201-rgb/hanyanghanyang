package com.personal.identity.core.application.security;

/**
 * <b>PORT</b> for password hashing and verification.
 *
 * <p>Default Implementation: {@code BCryptPasswordEncoderAdapter} which wraps
 * {@code org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}.
 *
 * <p>Placing this port in the core domain (instead of tightly coupling with Spring Security classes directly) ensures:
 * <ul>
 * <li>The core domain remains independent of Spring Security framework classes.</li>
 * <li>Unit tests for services can easily mock this interface without the overhead of loading actual BCrypt components.</li>
 * <li>Provides the flexibility to seamlessly swap out the underlying hashing algorithm to Argon2, PBKDF2, etc., if requirements change in the future.</li>
 * </ul>
 *
 * <p><b>NOTE:</b> This interface shares the identical simple name with {@code org.springframework.security.crypto.password.PasswordEncoder}.
 * When importing, please ensure you select the correct package corresponding to the core domain.
 */
public interface PasswordEncoder {

    /**
     * Hashes a plain text password into a secure cryptographic hash (e.g., BCrypt {@code $2a$10$...}).
     */
    String encode(String rawPassword);

    /**
     * Verifies whether a plain text password matches an existing encoded hash.
     * <p>NEVER hash the raw password input to perform a direct string equality comparison (e.g., {@code .equals()}) —
     * BCrypt produces a distinct hash result on every execution due to its randomized salt generation.
     * You must utilize the library-provided {@code matches} evaluation method.
     */
    boolean matches(String rawPassword, String encodedPassword);
}