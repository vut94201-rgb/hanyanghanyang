package com.personal.identity.infrastructure.redis;

import com.personal.identity.core.token.AccessTokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adapter implements {@link AccessTokenBlacklist} (port của core) dùng Redis.
 *
 * <p><b>Pattern:</b> với mỗi token bị revoke (logout), lưu 1 key vào Redis với
 * TTL = thời gian còn lại trước khi token expire tự nhiên. Redis tự xoá key
 * khi hết TTL → không phải tự dọn rác.
 *
 * <p><b>Key namespace {@code "blacklist:access:"}:</b> namespace là TỐI QUAN TRỌNG.
 * Service này có thể dùng chung Redis với các tính năng khác (cache, rate-limit,
 * idempotency...) nên phải đặt prefix rõ ràng để tránh đụng key. Quy ước trong
 * repo này: {@code <feature>:<entity>:<id>}.
 *
 * <p><b>Vì sao value là {@code "1"}, không phải JSON metadata:</b> ta chỉ cần
 * biết "key có tồn tại không" (chứ không cần đọc nội dung). Value càng nhỏ
 * càng tiết kiệm RAM Redis - 1 byte cho mỗi token revoke. Nếu sau này cần lưu
 * thêm thông tin (vd: lý do revoke), đổi sang Hash hoặc JSON.
 *
 * <p><b>Vì sao {@link StringRedisTemplate}, không phải {@code RedisTemplate<String, Object>}:</b>
 * Spring Boot auto-config sẵn {@code StringRedisTemplate} bean cho dữ liệu string thuần.
 * Không cần custom serializer → 0 dòng config thêm. Dùng generic
 * {@code RedisTemplate<String, Object>} sẽ kéo theo JdkSerializationRedisSerializer
 * mặc định - vừa tốn dung lượng vừa khó debug bằng redis-cli.
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlacklistAdapter implements AccessTokenBlacklist {

    /**
     * Prefix cho mọi key blacklist. KHÔNG đổi tuỳ tiện - đổi prefix = "mất sạch"
     * blacklist hiện có (key cũ vẫn nằm trong Redis nhưng adapter không tìm thấy).
     */
    private static final String KEY_PREFIX = "blacklist:access:";

    /**
     * Marker value. Bất kỳ string non-empty nào cũng được, dùng "1" cho ngắn.
     */
    private static final String MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void add(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            // Edge case: TTL <= 0 nghĩa là token đã expire - không cần blacklist
            // (verify JWT sẽ tự fail). Tránh gọi Redis với TTL 0 (Redis sẽ delete ngay).
            return;
        }
        redisTemplate.opsForValue().set(buildKey(tokenId), MARKER, ttl);
    }

    @Override
    public boolean contains(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        // hasKey() trả Boolean (có thể null nếu connection lỗi) - wrap qua
        // Boolean.TRUE.equals() để xử lý null-safe và tránh NPE unbox.
        return redisTemplate.hasKey(buildKey(tokenId));
    }

    private String buildKey(String tokenId) {
        return KEY_PREFIX + tokenId;
    }
}