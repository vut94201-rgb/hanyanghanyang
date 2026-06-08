package com.personal.identity.infrastructure.security.jwt;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Provides a singleton {@link SecretKey} instance shared by both sides: token signing (building)
 * and token verification (parsing).
 *
 * <p><b>Why decouple from {@link JwtTokenProviderAdapter}:</b> Later during step F,
 * the {@code JwtAuthenticationFilter} running in the identity-api module will need to verify incoming JWTs
 * for every incoming request — a filter that requires this exact same {@link SecretKey}. Separating this out
 * as a standalone bean allows both the adapter and the filter to inject the same shared instance,
 * preventing the overhead of decoding the Base64 secret twice.
 *
 * <p><b>Fail-fast design in {@link PostConstruct}:</b> If the secret is missing,
 * not valid Base64, or shorter than 256 bits (required for HS256) -> the application will refuse to start.
 * This is significantly safer than letting the application boot up and then fail abruptly upon handling the very first request.
 *
 * <p><b>JJWT 0.12+ Pitfall:</b> {@code Keys.hmacShaKeyFor(byte[])} throws a
 * {@link WeakKeyException} if the key length is under 256 bits. This built-in feature prevents developers
 * from deploying weak secrets. Production environments should utilize a secret >= 384 bits to provide cryptographic margin.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyProvider {

    private final JwtProperties properties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret cannot be empty. Dev: set in application-dev.yml. "
                            + "Production: set env JWT_SECRET."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.jwt.secret is not a valid Base64 string. Generate a new secret using: "
                            + "\"openssl rand -base64 48\"", e
            );
        }

        try {
            // hmacShaKeyFor selects the appropriate algorithm based on key length:
            // 256-383 bits -> HS256, 384-511 bits -> HS384, >=512 bits -> HS512.
            // We utilize 384 bits in dev -> HS384.
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "app.jwt.secret is too short (must be >= 256 bits = 32 bytes after Base64 decoding). "
                            + "Generate a new secret using: openssl rand -base64 48", e
            );
        }

        log.info("JWT signing key initialized: algorithm={}, length={} bits",
                signingKey.getAlgorithm(), keyBytes.length * 8);
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}