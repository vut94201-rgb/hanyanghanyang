package com.personal.identity.infrastructure.security.jwt;

import com.personal.identity.core.domain.token.TokenClaims;
import com.personal.identity.core.domain.token.TokenProvider;
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
 * Adapter implementing {@link TokenProvider} (a core port) using JJWT 0.13.x.
 *
 * <p><b>Custom claims:</b>
 * <ul>
 * <li>{@code sub} (reserved): userId as a string.</li>
 * <li>{@code sid}: sessionId UUID - utilized by the filter to verify if the session is still ACTIVE.</li>
 * <li>{@code roles}: Set of role codes.</li>
 * <li>{@code perms}: Set of permission codes.</li>
 * <li>{@code jti}: A random UUID - acts as the unique identifier for token blacklisting upon logout.</li>
 * <li>{@code iss}, {@code iat}, {@code exp}: Standard JWT claims.</li>
 * </ul>
 *
 * <p><b>Why we need a JTI:</b> Access tokens are inherently stateless JWTs. When a user logs out,
 * we append the JTI to the Redis blacklist with a TTL equal to the token's remaining lifespan. The JWT filter
 * inspects this blacklist prior to verifying the signature. Without a JTI, we would be forced to blacklist
 * the entire lengthy token string → resulting in high RAM consumption.
 *
 * <p><b>JJWT API: 0.13 vs 0.11 (interview notes):</b>
 * <ul>
 * <li>{@code setSubject(...)} → {@code subject(...)} (dropping the {@code set} prefix).</li>
 * <li>{@code setExpiration(...)} → {@code expiration(...)}.</li>
 * <li>{@code parserBuilder()} → {@code parser()}.</li>
 * <li>{@code setSigningKey(...)} → {@code verifyWith(...)} (verb-based, conveying clearer intent).</li>
 * <li>{@code parseClaimsJws(...).getBody()} → {@code parseSignedClaims(...).getPayload()}.</li>
 * </ul>
 *
 * <p><b>Error handling philosophy on the verify path:</b> Invalid JWTs (invalid signatures, expired,
 * malformed) are a DAILY occurrence in production - caused by clients sending stale tokens, replay
 * attacks, transmission errors, etc. DO NOT log ERROR/WARN on the verify path - doing so will flood
 * the application logs. Logging at DEBUG is sufficient. Returning {@link Optional#empty()} provides
 * enough context for the service layer to decide how to handle the failure.
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

        // JTI: If the caller explicitly sets the tokenId (rare, usually just for tests), use it; otherwise, auto-generate.
        // In a normal login/refresh flow, the service passes tokenId=null - letting the adapter handle generation.
        String tokenId = claims.tokenId() != null ? claims.tokenId() : UUID.randomUUID().toString();

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(claims.userId()))
                .id(tokenId)                                   // JTI for blacklisting purposes
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
                    .requireIssuer(properties.issuer())   // Rejects tokens originating from a different issuer
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Optional.of(toTokenClaims(jwtClaims));

        } catch (JwtException e) {
            // Catches ALL variations of JwtException: ExpiredJwtException, SignatureException,
            // MalformedJwtException, MissingClaimException, IncorrectClaimException...
            // Logged at DEBUG because this is a routine event in a production environment.
            log.debug("JWT verify fail: {}", e.getMessage());
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            // Defensive: parseSignedClaims might throw IAE if the token is null
            // (already guarded above) or strangely empty.
            log.debug("Invalid JWT format: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private TokenClaims toTokenClaims(Claims jwtClaims) {
        String tokenId = jwtClaims.getId();   // JTI - utilized for blacklisting upon logout
        Long userId = Long.valueOf(jwtClaims.getSubject());
        String sessionId = jwtClaims.get(CLAIM_SESSION_ID, String.class);
        Set<String> roles = readStringSet(jwtClaims, CLAIM_ROLES);
        Set<String> permissions = readStringSet(jwtClaims, CLAIM_PERMISSIONS);
        Instant issuedAt = jwtClaims.getIssuedAt().toInstant();
        Instant expiresAt = jwtClaims.getExpiration().toInstant();

        return new TokenClaims(tokenId, userId, sessionId, roles, permissions, issuedAt, expiresAt);
    }

    /**
     * Extracts a string set from a claim. JJWT deserializes JSON arrays into a {@link List},
     * not a Set - hence explicit conversion is required. Null-safe implementation ensures legacy tokens
     * missing this claim can still be parsed (returning an empty set).
     */
    @SuppressWarnings("unchecked")
    private Set<String> readStringSet(Claims claims, String claimName) {
        Object raw = claims.get(claimName);
        if (raw instanceof Collection<?> collection) {
            // JJWT might return List<String> or List<Object> - safely cast each element individually.
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