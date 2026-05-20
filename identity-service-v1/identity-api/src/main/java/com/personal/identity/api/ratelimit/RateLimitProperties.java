package com.personal.identity.api.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bind config namespace {@code app.rate-limit.*} từ yml.
 *
 * <p>Mỗi endpoint nhạy cảm có 2 tầng limit:
 * <ul>
 *   <li><b>Short window</b> (vài giây - 1 phút): chống burst attack.</li>
 *   <li><b>Long window</b> (1 giờ): chống slow drip attack.</li>
 * </ul>
 *
 * <p><b>Vì sao 2 tầng:</b> chỉ short window thì attacker có thể spam đúng giới
 * hạn rồi nghỉ 1 phút, lặp lại - vẫn có thể thử hàng nghìn password/giờ. Chỉ
 * long window thì cho phép burst 100 request trong 1 giây mà vẫn pass. Hai
 * bucket áp đồng thời → cả hai phải còn token request mới đi qua.
 *
 * <p><b>Sample yml:</b>
 * <pre>
 * app:
 *   rate-limit:
 *     enabled: true
 *     login:
 *       short-window-capacity: 5
 *       short-window-duration: 1m
 *       long-window-capacity: 20
 *       long-window-duration: 1h
 * </pre>
 *
 * @param enabled bật/tắt toàn bộ rate limit (handy để test)
 * @param login  config cho POST /api/v1/auth/login (key: IP client)
 * @param register config cho POST /api/v1/auth/register (key: IP client)
 * @param refresh config cho POST /api/v1/auth/refresh (key: 8 ký tự đầu của refresh token)
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        EndpointLimit login,
        EndpointLimit register,
        EndpointLimit refresh
) {

    /**
     * Config cho 1 endpoint.
     */
    public record EndpointLimit(
            long shortWindowCapacity,
            Duration shortWindowDuration,
            long longWindowCapacity,
            Duration longWindowDuration
    ) {
    }
}
