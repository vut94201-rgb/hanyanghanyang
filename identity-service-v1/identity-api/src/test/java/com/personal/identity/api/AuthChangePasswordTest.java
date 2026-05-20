package com.personal.identity.api;


import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.ChangePasswordRequest;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.RegisterRequest;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho change-password flow.
 *
 * <h3>Phạm vi</h3>
 * <ul>
 *   <li>Change password thành công → 204, password mới hoạt động.</li>
 *   <li>Login bằng password cũ sau change → fail 401.</li>
 *   <li>Session hiện tại GIỮ ACTIVE (UX: không kick user khỏi device họ vừa thao tác).</li>
 *   <li>Session KHÁC bị REVOKED (cắt access của attacker trên device khác).</li>
 *   <li>Change password với currentPassword sai → 401.</li>
 *   <li>Change password không authenticated → 403.</li>
 * </ul>
 */
class AuthChangePasswordTest extends IntegrationTestBase {

    @Test
    @DisplayName("Change password thành công → password mới hoạt động, password cũ thì không")
    void changePasswordSuccess() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String oldPassword = "OldPass@123";
        String newPassword = "NewPass@456";

        // Register
        restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(new RegisterRequest(
                        username, username + "@example.com", oldPassword, "Test"),
                        authHeaders(null)),
                Object.class);

        // Login
        AuthResponse auth = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, oldPassword), authHeaders(null)),
                AuthResponse.class).getBody();
        assertThat(auth).isNotNull();

        // Change password
        ResponseEntity<Void> resp = exchange(
                "/api/v1/auth/change-password", HttpMethod.POST, auth.accessToken(),
                new ChangePasswordRequest(oldPassword, newPassword), Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Login bằng password cũ → fail
        ResponseEntity<String> oldLogin = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, oldPassword), authHeaders(null)),
                String.class);
        assertThat(oldLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login bằng password mới → OK
        ResponseEntity<AuthResponse> newLogin = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, newPassword), authHeaders(null)),
                AuthResponse.class);
        assertThat(newLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("After change-password:  current session  keep ACTIVE, other session  REVOKED")
    void changePasswordKeepsCurrentSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String password = "OldPass@123";

        // Register
        restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(new RegisterRequest(
                        username, username + "@example.com", password, "Test"),
                        authHeaders(null)),
                Object.class);

        // Login 2 lần để có 2 session
        AuthResponse session1 = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        AuthResponse session2 = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(new LoginRequest(username, password), authHeaders(null)),
                AuthResponse.class).getBody();
        assertThat(session1).isNotNull();
        assertThat(session2).isNotNull();
        assertThat(session1.sessionId()).isNotEqualTo(session2.sessionId());

        // Change password TỪ session1
        exchange("/api/v1/auth/change-password", HttpMethod.POST, session1.accessToken(),
                new ChangePasswordRequest(password, "NewPass@456"), Void.class);

        // Verify ở DB:
        // - session1 vẫn ACTIVE (user đang dùng, không kick)
        // - session2 đã REVOKED (cắt access của "thiết bị khác")
        String s1Status = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, session1.sessionId());
        String s2Status = jdbcTemplate.queryForObject(
                "SELECT session_status FROM sessions WHERE id = ?",
                String.class, session2.sessionId());

        assertThat(s1Status).isEqualTo("ACTIVE");
        assertThat(s2Status).isEqualTo("REVOKED");

        // Session2's reason = USER_ACTION (do change-password gây revoke)
        String s2Reason = jdbcTemplate.queryForObject(
                "SELECT revoked_reason FROM sessions WHERE id = ?",
                String.class, session2.sessionId());
        assertThat(s2Reason).isEqualTo("USER_ACTION");
    }

    @Test
    @DisplayName("Change password sai currentPassword → 401")
    void changePasswordWrongCurrent() {
        AuthResponse auth = registerAndLogin();

        ResponseEntity<String> resp = exchange(
                "/api/v1/auth/change-password", HttpMethod.POST, auth.accessToken(),
                new ChangePasswordRequest("WrongOldPass", "NewPass@456"), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("AUTH.INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Change password không authenticated → 403")
    void changePasswordWithoutAuth() {
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/change-password"),
                new HttpEntity<>(new ChangePasswordRequest("a", "b"), authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
