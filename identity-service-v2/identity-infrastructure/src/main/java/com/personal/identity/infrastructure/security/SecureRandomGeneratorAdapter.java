package com.personal.identity.infrastructure.security;

import com.personal.identity.core.application.security.SecureRandomGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Adapter implementing {@link SecureRandomGenerator} (a core port) by wrapping
 * the JDK's {@link SecureRandom}.
 *
 * <p><b>Purpose:</b> Generates a raw cryptographic random string for the refresh token.
 * Once generated, the raw token is hashed (SHA-256) before persisting to the DB — the DB NEVER
 * stores the raw token, only the hash. When a client submits the raw token for rotation,
 * the server hashes it again and compares it with the hash in the DB. This mimics the "password storage"
 * pattern — preventing even DB administrators from impersonating a user session.
 *
 * <p><b>32 bytes = 256-bit entropy:</b> Encoded via Base64 URL-safe (replacing {@code +/} with
 * {@code -_}) and removing the padding ({@code =}) to make the token completely HTTP-friendly:
 * no URL-encoding needed, and immune to being truncated by legacy HTTP clients.
 * Results in a clean 43-character string.
 *
 * <p><b>Why {@link SecureRandom} is a field, not instantiated on every call:</b>
 * <ul>
 * <li>{@link SecureRandom} is thread-safe and safe for concurrent use.</li>
 * <li>Initial instantiation can block while gathering entropy from the OS (especially on Linux
 * systems utilizing {@code /dev/random}). Declaring it as a final field ensures you pay this
 * cost exactly once during application startup, rather than penalizing individual login requests.</li>
 * </ul>
 */
@Component
public class SecureRandomGeneratorAdapter implements SecureRandomGenerator {

    /**
     * 32 bytes = 256 bits. Sufficient to withstand brute-force attacks over the token's lifespan
     * (refresh token TTL is 30 days). Reference NIST SP 800-63B: minimum 64 bits for session tokens;
     * 256 bits is a highly secure, future-proof choice.
     */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * URL-safe encoder omitting the padding character ({@code '='}) — ideal for HTTP headers,
     * cookies, and URL parameters.
     */
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}