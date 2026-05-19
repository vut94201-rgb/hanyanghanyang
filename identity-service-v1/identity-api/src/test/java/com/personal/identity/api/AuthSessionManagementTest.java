package com.personal.identity.api;

import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.LogoutAllResponse;
import com.personal.identity.api.dto.RegisterRequest;
import com.personal.identity.api.dto.SessionResponse;
import com.personal.identity.api.dto.UserResponse;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho Step G endpoints: /me, /sessions, DELETE /sessions/{id}, /logout-all.
 *
 * <h3>Phạm vi</h3>
 * <ul>
 *   <li>GET /me → 200 + UserResponse đúng user.</li>
 *   <li>GET /me không token → 403.</li>
 *   <li>GET /sessions → list session ACTIVE, có flag current đúng.</li>
 *   <li>DELETE /sessions/{id} chính chủ → 204, session REVOKED.</li>
 *   <li>DELETE /sessions/{id} session của user khác (IDOR) → 403.</li>
 *   <li>DELETE /sessions/{id} không tồn tại → 404.</li>
 *   <li>POST /logout-all → 200 + revokedCount, mọi session REVOKED.</li>
 *   <li>Sau /logout-all: token cũ → 403.</li>
 * </ul>
 */
class AuthSessionManagementTest extends IntegrationTestBase {

    @Test
    @DisplayName("GET /me → trả UserResponse của user hiện tại")
    void getMeSuccess() {
        AuthResponse auth = registerAndLogin();

        ResponseEntity<UserResponse> resp = exchange(
                "/api/v1/auth/me", HttpMethod.GET, auth.accessToken(), null, UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().username()).startsWith("user_");
        assertThat(resp.getBody().roles()).containsExactly("USER");
    }

    @Test
    @DisplayName("GET /me không token → 403")
    void getMeWithoutAuth() {
        ResponseEntity<String> resp = restTemplate.getForEntity(url("/api/v1/auth/me"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("GET /sessions → list active session, current flag đúng")
    void listSessionsCurrentFlag() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String password = "Password@123";

        // Register + login 2 lần để có 2 session
        restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(new RegisterRequest(
                        username, username + "@example.com", password, "Test"),
                        authHeaders(null)),
                Object.class);

        AuthResponse s1 = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        AuthResponse s2 = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        assertThat(s1).isNotNull();
        assertThat(s2).isNotNull();

        // GET /sessions từ session 1
        ResponseEntity<List<SessionResponse>> resp = restTemplate.exchange(
                url("/api/v1/auth/sessions"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(s1.accessToken())),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<SessionResponse> sessions = resp.getBody();
        assertThat(sessions).isNotNull().hasSize(2);

        // Đúng 1 session có current=true, là session1
        long currentCount = sessions.stream().filter(SessionResponse::current).count();
        assertThat(currentCount).isEqualTo(1);
        SessionResponse currentSession = sessions.stream()
                .filter(SessionResponse::current).findFirst().orElseThrow();
        assertThat(currentSession.id()).isEqualTo(s1.sessionId());
    }

    @Test
    @DisplayName("DELETE /sessions/{id} chính chủ → 204, session REVOKED")
    void revokeOwnSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String password = "Password@123";
        restTemplate.postForEntity(url("/api/v1/auth/register"),
                new HttpEntity<>(new RegisterRequest(
                        username, username + "@example.com", password, "Test"),
                        authHeaders(null)),
                Object.class);

        AuthResponse s1 = restTemplate.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        AuthResponse s2 = restTemplate.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        assertThat(s1).isNotNull();
        assertThat(s2).isNotNull();

        // Từ s1, revoke s2
        ResponseEntity<Void> resp = exchange(
                "/api/v1/auth/sessions/" + s2.sessionId(),
                HttpMethod.DELETE, s1.accessToken(), null, Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // s2 đã REVOKED, s1 vẫn ACTIVE
        String s2Status = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, s2.sessionId());
        String s1Status = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, s1.sessionId());
        assertThat(s2Status).isEqualTo("REVOKED");
        assertThat(s1Status).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("DELETE /sessions/{id} của user khác (IDOR) → 403")
    void revokeOtherUserSession() {
        // User A
        AuthResponse userA = registerAndLogin();

        // User B
        AuthResponse userB = registerAndLogin();

        // User A cố revoke session của user B
        ResponseEntity<String> resp = exchange(
                "/api/v1/auth/sessions/" + userB.sessionId(),
                HttpMethod.DELETE, userA.accessToken(), null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).contains("SESSION.ACCESS_DENIED");

        // Session B vẫn ACTIVE
        String bStatus = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, userB.sessionId());
        assertThat(bStatus).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("DELETE /sessions/{id} không tồn tại → 404")
    void revokeNonexistentSession() {
        AuthResponse auth = registerAndLogin();
        String fakeId = UUID.randomUUID().toString();

        ResponseEntity<String> resp = exchange(
                "/api/v1/auth/sessions/" + fakeId,
                HttpMethod.DELETE, auth.accessToken(), null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).contains("SESSION.NOT_FOUND");
    }

    @Test
    @DisplayName("POST /logout-all → revoke tất cả session, token cũ fail")
    void logoutAll() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String password = "Password@123";
        restTemplate.postForEntity(url("/api/v1/auth/register"),
                new HttpEntity<>(new RegisterRequest(
                        username, username + "@example.com", password, "Test"),
                        authHeaders(null)),
                Object.class);

        // 3 lần login để có 3 session
        AuthResponse s1 = restTemplate.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        restTemplate.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class);
        restTemplate.postForEntity(url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class);
        assertThat(s1).isNotNull();

        // Logout-all từ s1
        ResponseEntity<LogoutAllResponse> resp = exchange(
                "/api/v1/auth/logout-all", HttpMethod.POST, s1.accessToken(),
                null, LogoutAllResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().revokedCount()).isEqualTo(3);

        // Verify: tất cả session của user này đều REVOKED
        Integer activeCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM sessions s
                JOIN users u ON s.user_id = u.id
                WHERE u.username = ? AND s.session_status = 'ACTIVE'
                """,
                Integer.class, username);
        assertThat(activeCount).isZero();

        // Token cũ giờ không dùng được nữa
        ResponseEntity<String> meResp = exchange(
                "/api/v1/auth/me", HttpMethod.GET, s1.accessToken(), null, String.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}