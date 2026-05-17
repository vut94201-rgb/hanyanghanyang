package com.personal.identity.core.token;

import java.time.Duration;

/**
 * <b>PORT</b> cho blacklist access token đã revoke.
 *
 * <p>Vì access token là JWT stateless (server không lưu), khi user logout ta KHÔNG
 * thể "xóa" token. Workaround: thêm token (hoặc id của nó) vào blacklist với TTL
 * = thời gian còn lại trước expiry. Mỗi request, filter check blacklist trước khi
 * verify JWT.
 *
 * <p>Implementation mặc định: {@code AccessTokenBlacklistAdapter} dùng Redis với
 * SETEX (set + expire) để TTL tự dọn rác khi token hết hạn.
 */
public interface AccessTokenBlacklist {

    /**
     * Thêm token vào blacklist. TTL nên = thời gian còn lại của token để Redis
     * tự dọn khi token hết hạn (không có giá trị blacklist 1 token đã expire).
     *
     * @param tokenId  định danh token (JWT JTI claim, hoặc hash của token)
     * @param ttl      thời gian giữ trong blacklist
     */
    void add(String tokenId, Duration ttl);

    /**
     * Check token có trong blacklist không.
     */
    boolean contains(String tokenId);
}
