package com.personal.identity.api;


import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.RefreshTokenRequest;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho refresh token flow.
 *
 * <h3>Phạm vi</h3>
 * <ul>
 *   <li>Refresh hợp lệ → 200, access/refresh token MỚI, sessionId giữ nguyên.</li>
 *   <li>Reuse rotated token (dùng lại token cũ sau refresh) → 401 + revoke family + session.</li>
 *   <li>Refresh token không tồn tại → 401.</li>
 *   <li>Refresh sau khi session đã REVOKED → 401.</li>
 * </ul>
 */
class AuthRefreshTokenTest extends IntegrationTestBase {

    @Test
    @DisplayName("Refresh hợp lệ → access/refresh token mới, sessionId giữ nguyên")
    void refreshRotation() {
        AuthResponse first = registerAndLogin();

        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(first.refreshToken()), authHeaders(null)),
                AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse rotated = resp.getBody();
        assertThat(rotated).isNotNull();
        // Token mới khác token cũ
        assertThat(rotated.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(first.refreshToken());
        // Session vẫn cùng một - đây là điểm cốt lõi của rotation pattern
        assertThat(rotated.sessionId()).isEqualTo(first.sessionId());
    }

    @Test
    @DisplayName("Reuse rotated refresh token → 401 TOKEN_REUSE_DETECTED + revoke session")
    void refreshReuseDetection() {
        AuthResponse first = registerAndLogin();

        // Refresh lần 1 thành công, token cũ thành USED
        ResponseEntity<AuthResponse> firstRefresh = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(first.refreshToken()), authHeaders(null)),
                AuthResponse.class);
        assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Dùng LẠI token cũ → server phải detect reuse
        ResponseEntity<String> reuseAttempt = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(first.refreshToken()), authHeaders(null)),
                String.class);

        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reuseAttempt.getBody()).contains("TOKEN.REUSE_DETECTED");

        // Session phải bị revoke (cascade từ reuse detection)
        String status = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, first.sessionId());
        assertThat(status).isEqualTo("REVOKED");

        // Refresh token mới (từ firstRefresh) cũng phải bị revoke
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE session_id = ? AND token_status = 'ACTIVE'",
                Integer.class, first.sessionId());
        assertThat(activeCount).isZero();
    }

    @Test
    @DisplayName("Refresh sau reuse → token MỚI (vừa cấp sau reuse) cũng fail")
    void tokenAfterReuseIsAlsoRevoked() {
        AuthResponse first = registerAndLogin();

        // Refresh hợp lệ → cấp newRefresh
        AuthResponse rotated = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(first.refreshToken()), authHeaders(null)),
                AuthResponse.class).getBody();

        // Reuse token cũ → trigger cascade revoke
        restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(first.refreshToken()), authHeaders(null)),
                String.class);

        // Bây giờ thử refresh bằng token "mới" (rotated) - phải fail
        assertThat(rotated).isNotNull();
        ResponseEntity<String> attempt = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest(rotated.refreshToken()), authHeaders(null)),
                String.class);

        assertThat(attempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Refresh token rác → 401")
    void refreshGarbageToken() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"),
                new HttpEntity<>(new RefreshTokenRequest("garbage-token-not-in-db"), authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("TOKEN.INVALID_REFRESH_TOKEN");
    }
}