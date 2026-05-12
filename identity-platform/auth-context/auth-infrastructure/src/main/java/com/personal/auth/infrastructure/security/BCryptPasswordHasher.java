package com.personal.auth.infrastructure.security;

import com.personal.auth.application.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt-backed implementation of the {@link PasswordHasher} port.
 *
 * <p>Lives in {@code auth-infrastructure} because only this layer is
 * allowed to depend on {@code spring-security-crypto}. The application
 * layer talks to the abstraction.
 *
 * <p>Default work factor (strength=10) is appropriate for this hardware
 * tier. If we ever profile login and need faster verification, drop to
 * 8; if we need stronger hashing for high-value accounts, raise to 12.
 *
 * <p>The encoder is held in a field rather than re-created per call
 * because constructing a {@link BCryptPasswordEncoder} is cheap but the
 * underlying {@code SecureRandom} initialisation isn't free.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("rawPassword must not be blank");
        }
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, hashedPassword);
    }
}
