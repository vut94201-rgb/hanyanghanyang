package com.personal.identity.infrastructure.security;


import com.personal.identity.core.application.security.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link PasswordEncoder} (a core port) by wrapping
 * Spring Security's {@link BCryptPasswordEncoder}.
 *
 * <p><b>Strength = 10:</b> (Spring's default) - this is also the strength used to
 * generate hashes for the seed admin user inside the {@code V3__seed_data.sql} migration.
 * Changing this strength will make legacy hashes unverifiable; therefore, DO NOT alter it
 * arbitrarily once users exist in the database.
 *
 * <p><b>Note on naming:</b> The core port shares the exact same name with
 * {@code org.springframework.security.crypto.password.PasswordEncoder}. When injecting
 * into services within the core, Spring resolves it by TYPE — meaning it maps to the core PasswordEncoder
 * (which this adapter implements) — so there is no ambiguity. If Spring's PasswordEncoder is needed
 * elsewhere for {@code DaoAuthenticationProvider} in the identity-api module, declare that bean
 * SEPARATELY inside {@code SecurityConfig}, rather than injecting this adapter.
 *
 * <p><b>Why @Component, not @Service:</b> This adapter resides in the purely technical
 * infrastructure layer, not the business logic layer. Using {@code @Component} is more accurate
 * in terms of semantics.
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    /**
     * Stateless and thread-safe - declared as final, shared across all requests.
     * DO NOT instantiate a new encoder on every encode call to avoid redundant CPU initialization costs.
     */
    private final BCryptPasswordEncoder delegate;

    public BCryptPasswordEncoderAdapter() {
        // Strength 10 = 2^10 = 1024 rounds. Takes ~100ms/hash on a standard CPU - a balanced sweet spot
        // between security (mitigating brute-force attacks) and UX (ensuring no login lag).
        this.delegate = new BCryptPasswordEncoder(10);
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        // BCryptPasswordEncoder.matches() natively handles null-safety and constant-time comparisons
        // (mitigating timing attacks). DO NOT rewrite manually.
        return delegate.matches(rawPassword, encodedPassword);
    }
}