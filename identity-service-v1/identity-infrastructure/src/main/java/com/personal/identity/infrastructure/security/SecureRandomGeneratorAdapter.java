package com.personal.identity.infrastructure.security;

import com.personal.identity.core.security.SecureRandomGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Adapter implements {@link SecureRandomGenerator} (port của core) dùng
 * {@link SecureRandom} của JDK.
 *
 * <p><b>Mục đích:</b> sinh chuỗi raw cho refresh token. Sau khi sinh, raw token
 * sẽ được hash (SHA-256) trước khi lưu DB - DB không bao giờ lưu raw token, chỉ
 * lưu hash. Khi client gửi raw lên để rotate, server hash lại rồi so với hash
 * trong DB. Đây là pattern giống "lưu password" - kể cả admin DB cũng không
 * impersonate được session.
 *
 * <p><b>32 byte = 256 bit entropy.</b> Encode Base64 URL-safe (thay {@code +/} bằng
 * {@code -_}) và bỏ padding {@code =} ra cho token HTTP-friendly:
 * không cần URL-encode, không bị cắt khi qua một số HTTP client cũ.
 * Kết quả ~43 ký tự.
 *
 * <p><b>Vì sao {@link SecureRandom} là field, không tạo mới mỗi lần gọi:</b>
 * <ul>
 *   <li>{@code SecureRandom} thread-safe, dùng chung được.</li>
 *   <li>Lần đầu khởi tạo có thể block để gom entropy từ OS (đặc biệt trên Linux
 *       nếu dùng {@code /dev/random}). Khai báo field => chỉ pay cost 1 lần lúc
 *       app start, không phải mỗi request login.</li>
 * </ul>
 */
@Component
public class SecureRandomGeneratorAdapter implements SecureRandomGenerator {

    /**
     * 32 byte = 256 bit. Đủ chống brute-force trong thời gian sống của token
     * (refresh token TTL 30 ngày). Tham khảo NIST SP 800-63B: minimum 64 bit
     * cho session token, 256 bit là dư an toàn.
     */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * URL-safe encoder, không có padding {@code '='} - thuận tiện cho HTTP header,
     * cookie, URL param.
     */
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}