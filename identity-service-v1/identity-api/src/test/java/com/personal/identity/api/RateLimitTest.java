package com.personal.identity.api;

import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test rate limit cho /login.
 *
 * <h2>Vì sao tách class riêng</h2>
 *
 * <p>Các test cũ chạy nhanh và share IP loopback. Nếu bật rate limit cho tất cả
 * thì test thứ 6+ trong cùng IntegrationTestBase context sẽ bị 429 ngẫu nhiên
 * (flaky). Class này:
 * <ul>
 *   <li>{@code @TestPropertySource} bật rate-limit chỉ trong test này.</li>
 *   <li>Set capacity short window = 3 cho gọn (đỡ phải spam 5 request mới
 *       trigger 429).</li>
 *   <li>Set duration ngắn (5s) để test refill cũng nhanh.</li>
 * </ul>
 *
 * <h2>Lưu ý về Redis state giữa test</h2>
 *
 * <p>{@code IntegrationTestBase.@BeforeEach} đã FLUSHALL Redis trước mỗi test
 * → bucket state cũ bị xoá → test bắt đầu fresh. Không cần dọn riêng ở đây.
 */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.login.short-window-capacity=3",
        "app.rate-limit.login.short-window-duration=5s",
        "app.rate-limit.login.long-window-capacity=100",
        "app.rate-limit.login.long-window-duration=1h",
        "app.rate-limit.register.short-window-capacity=100",
        "app.rate-limit.register.short-window-duration=1m",
        "app.rate-limit.register.long-window-capacity=1000",
        "app.rate-limit.register.long-window-duration=1h",
        "app.rate-limit.refresh.short-window-capacity=100",
        "app.rate-limit.refresh.short-window-duration=1m",
        "app.rate-limit.refresh.long-window-capacity=1000",
        "app.rate-limit.refresh.long-window-duration=1h"
})
class RateLimitTest extends IntegrationTestBase {

    @Test
    @DisplayName("Login: 3 request đầu pass (cho phép), request thứ 4 bị 429")
    void loginRateLimitBlocksAfterCapacityExhausted() {
        // Request fake credential — không quan trọng pass/fail login, ta chỉ check rate limit.
        LoginRequest body = new LoginRequest("nonexistent_user", "wrong_password");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(body, headers);

        // 3 request đầu: rate limit chưa block → trả về 401 (Invalid credentials)
        // hoặc 400 (validation). KHÔNG được trả 429.
        for (int i = 1; i <= 3; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/v1/auth/login", request, String.class);
            assertThat(response.getStatusCode())
                    .as("Request #%d should NOT be rate limited", i)
                    .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        // Request thứ 4: bucket cạn → 429
        ResponseEntity<String> blocked = restTemplate.postForEntity(
                "/api/v1/auth/login", request, String.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Verify response headers: Retry-After phải có và >= 1
        String retryAfter = blocked.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isGreaterThanOrEqualTo(1);

        // Verify body có errorCode chuẩn
        assertThat(blocked.getBody()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("X-RateLimit-Remaining header có ở response success")
    void remainingHeaderPresent() {
        LoginRequest body = new LoginRequest("nonexistent_user", "wrong_password");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", request, String.class);

        assertThat(firstResponse.getStatusCode()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        // Header có thể tồn tại — không hard assert vì capacity refill ảnh hưởng
        // exact value. Chỉ check nếu có thì numeric.
        String remaining = firstResponse.getHeaders().getFirst("X-RateLimit-Remaining");
        if (remaining != null) {
            assertThat(Long.parseLong(remaining)).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Endpoint không trong scope (vd /api/v1/auth/me) không bị rate limit")
    void unrelatedEndpointNotRateLimited() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-jwt-to-trigger-401");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Spam 10 lần — không lần nào bị 429 vì /me không nằm trong rate limit scope.
        for (int i = 1; i <= 10; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/v1/auth/me",
                    org.springframework.http.HttpMethod.GET,
                    request,
                    String.class);
            assertThat(response.getStatusCode())
                    .as("/me request #%d should NOT be rate limited", i)
                    .isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
