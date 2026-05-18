package com.personal.identity.infrastructure.security.jwt;

import com.personal.identity.core.token.TokenClaims;
import com.personal.identity.core.token.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Adapter implements {@link TokenProvider} (port của core) dùng JJWT 0.13.x.
 *
 * <p><b>Custom claims:</b>
 * <ul>
 *   <li>{@code sub} (reserved): userId dạng string</li>
 *   <li>{@code sid}: sessionId UUID - filter dùng để check session ACTIVE</li>
 *   <li>{@code roles}: Set role codes</li>
 *   <li>{@code perms}: Set permission codes</li>
 *   <li>{@code jti}: UUID random - dùng làm key cho blacklist khi logout</li>
 *   <li>{@code iss}, {@code iat}, {@code exp}: chuẩn JWT</li>
 * </ul>
 *
 * <p><b>Tại sao có JTI:</b> access token là JWT stateless. Khi user logout, ta
 * thêm JTI vào blacklist Redis với TTL = thời gian còn lại của token. JWT filter
 * check blacklist trước verify signature. Không có JTI thì phải blacklist cả
 * string token dài → tốn RAM.
 *
 * <p><b>API JJWT 0.13 vs 0.11 (cho phỏng vấn):</b>
 * <ul>
 *   <li>{@code setSubject(...)} → {@code subject(...)} (bỏ prefix {@code set})</li>
 *   <li>{@code setExpiration(...)} → {@code expiration(...)}</li>
 *   <li>{@code parserBuilder()} → {@code parser()}</li>
 *   <li>{@code setSigningKey(...)} → {@code verifyWith(...)} (verb-based, đọc rõ ý)</li>
 *   <li>{@code parseClaimsJws(...).getBody()} → {@code parseSignedClaims(...).getPayload()}</li>
 * </ul>
 *
 * <p><b>Triết lý error handling ở verify path:</b> JWT sai (signature, expired,
 * malformed) là chuyện THƯỜNG NGÀY trên production - client gửi token cũ, replay
 * attack, đường truyền lỗi... Không log ERROR/WARN ở verify path - sẽ làm ngập
 * log. Chỉ log DEBUG. Trả {@link Optional#empty()} là đủ - service decide xử lý.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProviderAdapter implements TokenProvider {

    private static final String CLAIM_SESSION_ID = "sid";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "perms";

    private final JwtProperties properties;
    private final JwtKeyProvider keyProvider;

    @Override
    public String generateAccessToken(TokenClaims claims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.accessTokenTtlMinutes()));

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(claims.userId()))
                .id(UUID.randomUUID().toString())              // JTI cho blacklist
                .claim(CLAIM_SESSION_ID, claims.sessionId())
                .claim(CLAIM_ROLES, claims.roleCodes())
                .claim(CLAIM_PERMISSIONS, claims.permissionCodes())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(keyProvider.getSigningKey())
                .compact();
    }

    @Override
    public Optional<TokenClaims> parseAndVerify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims jwtClaims = Jwts.parser()
                    .verifyWith(keyProvider.getSigningKey())
                    .requireIssuer(properties.issuer())   // Reject token từ issuer khác
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(toTokenClaims(jwtClaims));

        } catch (JwtException e) {
            // Bắt MỌI loại JwtException: ExpiredJwtException, SignatureException,
            // MalformedJwtException, MissingClaimException, IncorrectClaimException...
            // Log DEBUG vì là sự kiện bình thường ở production.
            log.debug("JWT verify fail: {}", e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            // Defensive: parseSignedClaims có thể throw IAE nếu token null
            // (đã guard ở trên) hoặc rỗng quá lạ.
            log.debug("JWT format không hợp lệ: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private TokenClaims toTokenClaims(Claims jwtClaims) {
        Long userId = Long.valueOf(jwtClaims.getSubject());
        String sessionId = jwtClaims.get(CLAIM_SESSION_ID, String.class);
        Set<String> roles = readStringSet(jwtClaims, CLAIM_ROLES);
        Set<String> permissions = readStringSet(jwtClaims, CLAIM_PERMISSIONS);
        Instant issuedAt = jwtClaims.getIssuedAt().toInstant();
        Instant expiresAt = jwtClaims.getExpiration().toInstant();

        return new TokenClaims(userId, sessionId, roles, permissions, issuedAt, expiresAt);
    }

    /**
     * Trích set string từ claim. JJWT deserialize array JSON thành {@link List},
     * không phải Set - phải convert. Null-safe để token cũ không có claim này
     * vẫn parse được (trả empty set).
     */
    @SuppressWarnings("unchecked")
    private Set<String> readStringSet(Claims claims, String claimName) {
        Object raw = claims.get(claimName);
        if (raw instanceof Collection<?> collection) {
            // JJWT có thể trả về List<String> hoặc List<Object> - safe cast từng phần tử.
            Set<String> result = new HashSet<>(collection.size());
            for (Object item : collection) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return Set.of();
    }
}