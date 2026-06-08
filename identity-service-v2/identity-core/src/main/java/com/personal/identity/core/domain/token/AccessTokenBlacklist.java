package com.personal.identity.core.domain.token;

import java.time.Duration;

/**
 * <b>PORT</b> for the blacklisted access tokens that have been revoked.
 *
 * <p>Since access tokens are stateless JWTs (not stored on the server), when a user logs out, we CANNOT
 * "delete" the token. Workaround: add the token (or its ID) to a blacklist with a TTL
 * equal to its remaining time before expiry. For every request, a filter checks the blacklist before
 * verifying the JWT.
 *
 * <p>Default implementation: {@code AccessTokenBlacklistAdapter} utilizes Redis with
 * SETEX (set + expire) so the TTL automatically garbage collects the entry once the token expires.
 */
public interface AccessTokenBlacklist {

    /**
     * Adds a token to the blacklist. The TTL should equal the token's remaining lifespan so Redis
     * automatically cleans it up upon expiration (there is no value in blacklisting an already expired token).
     *
     * @param tokenId the token identifier (JWT JTI claim, or the token's hash)
     * @param ttl     the retention duration inside the blacklist
     */
    void add(String tokenId, Duration ttl);

    /**
     * Checks whether the token is present in the blacklist.
     */
    boolean contains(String tokenId);
}