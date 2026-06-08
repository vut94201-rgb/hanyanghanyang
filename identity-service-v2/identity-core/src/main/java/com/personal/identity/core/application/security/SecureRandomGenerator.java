package com.personal.identity.core.application.security;

/**
 * <b>PORT</b> for generating a cryptographically-secure random string to serve as the raw refresh token.
 *
 * <p>Default Implementation: Uses {@code java.security.SecureRandom} with 32 bytes of
 * entropy → Base64 URL-safe encoded → producing a ~43 character string that is secure for HTTP transport.
 *
 * <p>NEVER use {@code Math.random()} or {@code java.util.Random}.
 */
public interface SecureRandomGenerator {

    /**
     * Generates a new token string with sufficient entropy to withstand brute-force guessing attacks.
     * <p>Default configuration expects >= 256-bit entropy, encoded using Base64 URL-safe.
     */
    String generateToken();
}