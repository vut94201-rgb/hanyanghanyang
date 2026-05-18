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
 * Cung cấp {@link SecretKey} singleton cho cả 2 bên: sign (build) và verify (parse).
 *
 * <p><b>Vì sao tách khỏi {@link JwtTokenProviderAdapter}:</b> sau này bước F sẽ
 * có {@code JwtAuthenticationFilter} chạy ở identity-api để verify JWT trong
 * request - filter đó cần cùng 1 {@code SecretKey}. Tách ra bean riêng để cả
 * adapter và filter inject chung 1 instance, KHÔNG decode Base64 lại 2 lần.
 *
 * <p><b>Fail-fast ở {@link PostConstruct}:</b> nếu secret thiếu, không phải
 * Base64 hợp lệ, hoặc &lt; 256 bit (HS256 yêu cầu) → app KHÔNG start. An toàn hơn
 * là cho app chạy rồi fail ở request đầu tiên.
 *
 * <p><b>Bẫy JJWT 0.12+:</b> {@code Keys.hmacShaKeyFor(byte[])} throw
 * {@link WeakKeyException} nếu key &lt; 256 bit. Đây là feature - chống developer
 * dùng secret yếu. Production set secret &gt;= 384 bit để có biên độ.
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
                    "app.jwt.secret không được rỗng. Dev: set trong application-dev.yml. "
                            + "Production: set env JWT_SECRET."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.jwt.secret không phải Base64 hợp lệ. Sinh secret mới: "
                            + "openssl rand -base64 48", e
            );
        }

        try {
            // hmacShaKeyFor chọn algorithm phù hợp theo độ dài key:
            // 256-383 bit → HS256, 384-511 bit → HS384, >=512 bit → HS512.
            // Ta dùng 384 bit ở dev → HS384.
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "app.jwt.secret quá ngắn (cần >= 256 bit = 32 byte sau Base64 decode). "
                            + "Sinh secret mới: openssl rand -base64 48", e
            );
        }

        log.info("JWT signing key initialized: algorithm={}, length={} bits",
                signingKey.getAlgorithm(), keyBytes.length * 8);
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }
}