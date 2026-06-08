package com.personal.identity.infrastructure.redis;

import com.personal.identity.core.domain.token.AccessTokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adapter implementing {@link AccessTokenBlacklist} (a core port) using Redis.
 *
 * <p><b>Pattern:</b> For each revoked token (upon logout), a key is stored in Redis with
 * {@code TTL = remaining time before natural token expiry}. Redis automatically deletes the key
 * once the TTL expires -> eliminating the need for manual garbage collection.
 *
 * <p><b>Key namespace {@code "blacklist:access:"}:</b> This namespace is CRITICAL.
 * This service may share the Redis instance with other features (cache, rate-limiting,
 * idempotency...) so clear prefixes must be defined to prevent key collisions. Convention in
 * this repository: {@code <feature>:<entity>:<id>}.
 *
 * <p><b>Why value is {@code "1"}, not a JSON metadata string:</b> We only need to
 * verify "does the key exist" (no need to read content). Keeping the value as small as
 * possible saves Redis RAM — 1 byte per revoked token. If future requirements mandate storing
 * additional information (e.g., reason for revocation), this can be migrated to a Hash or JSON.
 *
 * <p><b>Why {@link StringRedisTemplate} is used instead of {@code RedisTemplate<String, Object>}:</b>
 * Spring Boot auto-configures a clean {@code StringRedisTemplate} bean for raw string data.
 * No custom serializer config required -> zero extra configuration lines. Using generic
 * {@code RedisTemplate<String, Object>} would trigger the default {@code JdkSerializationRedisSerializer},
 * which wastes space and is difficult to debug via {@code redis-cli}.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistAdapter implements AccessTokenBlacklist {

    /**
     * Prefix for all blacklist keys. DO NOT change arbitrarily — changing the prefix means
     * "losing all current data" (existing blacklisted keys still reside in Redis but the adapter will fail to find them).
     */
    private static final String KEY_PREFIX = "blacklist:access:";

    /**
     * Marker value. Any non-empty string works; using "1" for brevity.
     */
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void add(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            // Edge case: TTL <= 0 means the token has already expired — no blacklisting needed
            // (the subsequent JWT verification will fail naturally). Avoid calling Redis with TTL 0 (Redis would delete immediately).
            return;
        }
        redisTemplate.opsForValue().set(buildKey(tokenId), MARKER, ttl);
    }

    @Override
    public boolean contains(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        // hasKey() returns a Boolean (can be null if a connection error occurs) — wrapped via
        // Boolean.TRUE.equals() to handle it null-safely and avoid primitive NPE unboxing.
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(tokenId)));
    }

    private String buildKey(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}