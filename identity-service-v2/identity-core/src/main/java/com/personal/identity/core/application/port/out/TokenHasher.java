package com.personal.identity.core.application.port.out;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashes raw refresh tokens into SHA-256 hex strings.
 *
 * <h2>Why SHA-256 instead of BCrypt?</h2>
 * <ul>
 * <li>BCrypt is slow-by-design (~100ms/execution), which is excellent for passwords because it thwarts
 * brute-force attacks even if the database leaks. However, a refresh token generated via
 * {@code SecureRandom} already possesses 256 bits of high entropy ⟿ brute-forcing it is cryptographically
 * impossible, making computationally expensive hashing redundant.</li>
 * <li>Refresh tokens are verified upon every rotation sequence. Utilizing BCrypt here would introduce
 * a mandatory 100ms latency penalty per token refresh request ⟿ choking system throughput under load.
 * SHA-256 executes in ~0.01ms, offering negligible overhead.</li>
 * <li>This matches standard OAuth2 authorization server reference implementations (e.g., Spring Authorization
 * Server, Keycloak) when handling refresh tokens or authorization codes.</li>
 * </ul>
 *
 * <h2>Why hash instead of storing plain text?</h2>
 * Database breaches occur routinely. Hashing inside the DB guarantees that even if an attacker
 * (or a rogue database administrator) gains full read access to the tables, they cannot impersonate active user
 * sessions. When a client submits a raw refresh token, the server hashes the input and executes a lookup
 * against the DB hash — identical to the "password storage" security pattern.
 *
 * <h2>Why a static utility class instead of a port + adapter?</h2>
 * SHA-256 is part of the standard JDK specification; it features no runtime behavioral variations
 * and requires zero infrastructure configuration. Keeping it as a pure static utility inside the core layer
 * is highly decoupled, lightweight, and sufficient. This stands in contrast to {@code PasswordEncoder},
 * which depends on external crypto strengths/configurations (BCrypt rounds) and must remain wrapped behind a port.
 */
public final class TokenHasher {

    private TokenHasher() {
        // static utility class, prevent instantiation
    }

    /**
     * Hashes a raw refresh token into a lowercase 64-character hex string.
     *
     * @param rawToken The raw refresh token string emitted by {@code SecureRandomGenerator.generateToken()}.
     * @return A 64-character lowercase hexadecimal string representation of the SHA-256 hash.
     * @throws IllegalStateException If the underlying JVM lacks SHA-256 support (highly anomalous edge case).
     */
    public static String hash(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken must not be blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory standard JDK algorithm since Java 1.4 — it is guaranteed to be present.
            // If missing, the JVM environment is profoundly broken; failing-fast is the correct behavior.
            throw new IllegalStateException("SHA-256 algorithm is unavailable on this JVM", e);
        }
    }

    /**
     * Converts a byte array into a lowercase hex string. 1 byte ⟿ 2 hex characters.
     */
    private static String toHex(byte[] bytes) {
        // Initialize capacity to exactly bytes.length * 2 to prevent internal array resizing.
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Masking 'b & 0xff' converts signed Java byte (-128 to 127) into an unsigned integer (0 to 255).
            // Avoid utilizing String.format("%02x", ...) — parsing formatting expressions on every byte
            // introduces high latency on this rotation hot-path when concurrent token requests surge.
            int v = b & 0xff;
            sb.append(Character.forDigit(v >>> 4, 16));
            sb.append(Character.forDigit(v & 0x0f, 16));
        }
        return sb.toString();
    }
}