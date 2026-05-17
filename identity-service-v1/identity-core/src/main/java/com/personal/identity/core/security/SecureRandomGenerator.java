package com.personal.identity.core.security;

/**
 * <b>PORT</b> sinh chuỗi ngẫu nhiên cryptographically-secure để làm refresh token raw.
 *
 * <p>Implementation mặc định: dùng {@code java.security.SecureRandom} với 32 byte
 * entropy → Base64 URL-safe encode → ~43 ký tự an toàn cho HTTP transport.
 *
 * <p>KHÔNG bao giờ dùng {@code Math.random()} hay {@code java.util.Random}.
 */
public interface SecureRandomGenerator {

    /**
     * Sinh chuỗi token mới, đủ entropy để chống guess.
     * Mặc định >= 256-bit entropy, encode Base64 URL-safe.
     */
    String generateToken();
}
