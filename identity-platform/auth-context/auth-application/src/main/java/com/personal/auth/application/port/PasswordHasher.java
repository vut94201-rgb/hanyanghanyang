package com.personal.auth.application.port;

/**
 * Outbound port for password hashing.
 *
 * <p>The application layer needs to hash a password before handing it to
 * the domain ({@code User.register(...)}), but it doesn't want to know
 * <i>how</i> — BCrypt today, Argon2 tomorrow, an HSM the day after.
 * This interface is the seam.
 *
 * <p>Implemented by {@code BCryptPasswordHasher} in {@code auth-infrastructure}.
 * Wired in via Spring DI — the use case receives a {@code PasswordHasher},
 * Spring picks the only implementation on the classpath.
 *
 * <p>Convention: <b>this layer does NOT depend on Spring Security</b>.
 * That's the whole point of the port — keep the BCrypt dependency
 * (and its {@code spring-security-crypto} jar) out of the application
 * module's classpath.
 */
public interface PasswordHasher {
    /**
     * Hash a plain-text password. Must be deterministic only in the
     * sense that {@link #matches(String, String)} can later verify it;
     * the raw hash output is typically salted and therefore differs
     * across calls for the same input.
     *
     * @param rawPassword plain text from the client; must be non-blank
     * @return opaque hash string suitable for persistence
     */
    String hash(String rawPassword);

    /**
     * Verify a plain-text password against a previously stored hash.
     * Used at login time (will be wired in when the Login flow lands).
     *
     * @param rawPassword    plain text candidate
     * @param hashedPassword previously stored hash
     * @return true if the candidate matches
     */
    boolean matches(String rawPassword, String hashedPassword);
}
