package com.personal.identity.api;


import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.RegisterRequest;
import com.personal.identity.api.dto.UserResponse;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho luồng register + login.
 *
 * <h3>Phạm vi</h3>
 * <ul>
 *   <li>Register thành công → 201, trả UserResponse KHÔNG có passwordHash.</li>
 *   <li>Register duplicate username → 409.</li>
 *   <li>Register duplicate email → 409.</li>
 *   <li>Register validation fail → 400.</li>
 *   <li>Login admin (seed data) → 200 + JWT.</li>
 *   <li>Login user vừa register → 200 + JWT có sessionId.</li>
 *   <li>Login sai password → 401.</li>
 *   <li>Login username không tồn tại → 401 (cùng message, không leak).</li>
 * </ul>
 */
class AuthRegisterLoginTest extends IntegrationTestBase {

    @Test
    @DisplayName("Register hợp lệ → 201 + UserResponse")
    void registerSuccess() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest req = new RegisterRequest(
                "user_" + suffix,
                "user_" + suffix + "@example.com",
                "Password@123",
                "Full Name");

        ResponseEntity<UserResponse> resp = restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(req, authHeaders(null)),
                UserResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().username()).isEqualTo("user_" + suffix);
        assertThat(resp.getBody().roles()).containsExactly("USER");
        assertThat(resp.getBody().id()).isPositive();
    }

    @Test
    @DisplayName("Register username trùng → 409")
    void registerDuplicateUsername() {
        AuthResponse first = registerAndLogin();
        // Lấy username vừa register
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE id = ?",
                String.class,
                getUserIdFromToken(first.accessToken()));

        RegisterRequest dup = new RegisterRequest(
                username, "other@example.com", "Password@123", "Other");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(dup, authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("USER.DUPLICATE_USERNAME");
    }

    @Test
    @DisplayName("Register email trùng → 409")
    void registerDuplicateEmail() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "shared_" + suffix + "@example.com";

        RegisterRequest first = new RegisterRequest(
                "user1_" + suffix, email, "Password@123", "First");
        restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(first, authHeaders(null)),
                Object.class);

        RegisterRequest second = new RegisterRequest(
                "user2_" + suffix, email, "Password@123", "Second");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(second, authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody()).contains("USER.DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("Register password ngắn → 400 + field error")
    void registerValidationFails() {
        RegisterRequest req = new RegisterRequest(
                "user_x", "x@example.com", "123", "X");

        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(req, authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("fieldErrors");
    }

    @Test
    @DisplayName("Login admin (seed) thành công")
    void loginAdminSuccess() {
        LoginRequest req = new LoginRequest("admin", "Admin@123");
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(req, authHeaders(null)),
                AuthResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().accessToken()).isNotBlank();
        assertThat(resp.getBody().refreshToken()).isNotBlank();
        assertThat(resp.getBody().sessionId()).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Login sai password → 401 + INVALID_CREDENTIALS")
    void loginWrongPassword() {
        registerAndLogin();
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM users WHERE id = (SELECT MAX(id) FROM users)",
                String.class);

        LoginRequest req = new LoginRequest(username, "WrongPassword");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(req, authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).contains("AUTH.INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("Login username không tồn tại → 401 cùng message (tránh user enumeration)")
    void loginUsernameNotFound() {
        LoginRequest req = new LoginRequest("nonexistent_" + UUID.randomUUID(), "any");
        ResponseEntity<String> resp = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(req, authHeaders(null)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Cùng error code với login sai password - không leak "user không tồn tại"
        assertThat(resp.getBody()).contains("AUTH.INVALID_CREDENTIALS");
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Decode userId từ JWT (sub claim) bằng base64 raw, không verify - chỉ để query DB. */
    private Long getUserIdFromToken(String jwt) {
        String[] parts = jwt.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        // payload format: {"iss":"...","sub":"42",...}
        int subStart = payload.indexOf("\"sub\":\"") + 7;
        int subEnd = payload.indexOf("\"", subStart);
        return Long.parseLong(payload.substring(subStart, subEnd));
    }
}
