package com.personal.identity.core.service;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash refresh token raw thành SHA-256 hex string.
 *
 * <p><b>Vì sao SHA-256, không phải BCrypt:</b>
 * <ul>
 *   <li>BCrypt slow-by-design (~100ms/lần) là tốt cho password (chống brute-force
 *       khi attacker leak DB). Nhưng refresh token có entropy 256-bit từ
 *       SecureRandom rồi - brute-force là vô vọng dù dùng hash thường.</li>
 *   <li>Refresh token verify mỗi lần rotate. Dùng BCrypt sẽ tốn 100ms/refresh
 *       không cần thiết. SHA-256 ~0.01ms.</li>
 *   <li>Đây cũng là cách OAuth2 reference implementation (Spring Authorization
 *       Server, Keycloak) làm với refresh token / authorization code.</li>
 * </ul>
 *
 * <p><b>Vì sao hash thay vì lưu plain:</b> DB leak xảy ra hàng ngày. Hash trong
 * DB nghĩa là kể cả admin DB cũng không impersonate được session. Khi rotate,
 * server hash lại raw token client gửi rồi so với hash trong DB - giống pattern
 * password.
 *
 * <p><b>Tại sao là utility static, không phải port + adapter:</b> SHA-256 là
 * standard JDK, không có biến thể runtime, không cần config. Đặt utility static
 * trong core đơn giản và đủ. (Khác với {@code PasswordEncoder} - BCrypt strength
 * là config-dependent nên cần là port.)
 */
public final class TokenHasher {

    private TokenHasher() {
        // utility class, no instance
    }

    /**
     * Hash raw token thành hex string lowercase 64 ký tự.
     *
     * @param rawToken raw refresh token từ {@code SecureRandomGenerator.generateToken()}
     * @return SHA-256 hash dạng hex lowercase, 64 ký tự
     * @throws IllegalStateException nếu JVM thiếu SHA-256 (gần như không thể xảy ra)
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
            // SHA-256 là standard JDK từ Java 1.4 - không bao giờ thiếu.
            // Nếu thiếu = JVM bị hỏng, fail-fast là đúng.
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM này", e);
        }
    }

    /**
     * byte[] → hex string lowercase. 1 byte → 2 ký tự hex.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // 0xff & b: convert signed byte → unsigned int 0-255
            // String.format("%02x", ...) chậm vì parse format string mỗi lần - tự
            // build cho nhanh, hot path khi nhiều request rotate cùng lúc.
            int v = b & 0xff;
            sb.append(Character.forDigit(v >>> 4, 16));
            sb.append(Character.forDigit(v & 0x0f, 16));
        }
        return sb.toString();
    }
}