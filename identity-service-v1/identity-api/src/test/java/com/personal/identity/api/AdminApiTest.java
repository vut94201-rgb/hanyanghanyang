package com.personal.identity.api;

import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test admin API end-to-end.
 *
 * <h2>Setup mặc định</h2>
 *
 * <p>Seed data có user {@code admin/Admin@123} (id=1) với role ADMIN. Login
 * admin để lấy JWT, rồi gọi /admin/* endpoint.
 *
 * <p>Tạo thêm user thường để test disable/lock/role assignment.
 */
class AdminApiTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Non-admin → 403 khi gọi /admin/users")
    void nonAdminForbidden() {
        // Tạo user thường rồi login
        AuthResponse userAuth = registerAndLogin();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAuth.accessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/users", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin login → list users trả về ít nhất admin row")
    void adminListUsers() throws Exception {
        String adminToken = loginAdmin();

        HttpHeaders headers = bearerHeaders(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/users?offset=0&limit=10",
                HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("total").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(body.get("items").isArray()).isTrue();
        assertThat(body.get("items").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Admin disable user → status DISABLED + session bị revoke")
    void adminDisableUser() throws Exception {
        // Tạo user thường, lấy id
        AuthResponse userAuth = registerAndLogin();
        // Decode JWT để lấy userId — đơn giản: gọi /me
        Long userId = fetchUserIdFromMe(userAuth.accessToken());

        // Admin disable user đó
        String adminToken = loginAdmin();
        HttpHeaders headers = bearerHeaders(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> disableResp = restTemplate.exchange(
                "/api/v1/admin/users/" + userId + "/disable",
                HttpMethod.POST, request, String.class);

        assertThat(disableResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(disableResp.getBody());
        assertThat(body.get("accountStatus").asText()).isEqualTo("DISABLED");

        // Session cũ phải bị revoke → access token vẫn dùng được tới khi expire,
        // nhưng JwtAuthenticationFilter check session active → /me phải 403.
        // Spring Security 6 default khi filter skip set Authentication.
        HttpHeaders userHeaders = bearerHeaders(userAuth.accessToken());
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
        ResponseEntity<String> meResp = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, userRequest, String.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin gán role MODERATOR cho user")
    void adminUpdateRoles() throws Exception {
        AuthResponse userAuth = registerAndLogin();
        Long userId = fetchUserIdFromMe(userAuth.accessToken());

        String adminToken = loginAdmin();
        HttpHeaders headers = bearerHeaders(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("roleCodes", java.util.List.of("USER", "MODERATOR"));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/admin/users/" + userId + "/roles",
                HttpMethod.POST, request, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode json = objectMapper.readTree(resp.getBody());
        JsonNode rolesNode = json.get("roleCodes");
        assertThat(rolesNode.isArray()).isTrue();
        java.util.List<String> roleCodes = new java.util.ArrayList<>();
        rolesNode.forEach(n -> roleCodes.add(n.asText()));
        assertThat(roleCodes).containsExactlyInAnyOrder("USER", "MODERATOR");
    }

    @Test
    @DisplayName("Admin tự gỡ role ADMIN của mình → 400")
    void adminCannotRemoveOwnAdminRole() throws Exception {
        String adminToken = loginAdmin();
        Long adminId = 1L; // admin seed id

        HttpHeaders headers = bearerHeaders(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("roleCodes", java.util.List.of("USER"));
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/admin/users/" + adminId + "/roles",
                HttpMethod.POST, request, String.class);

        // GlobalExceptionHandler map IllegalArgumentException → 400 (verify trong code thực).
        // Nếu chưa map, có thể trả 500 — test sẽ fail và là tín hiệu cần thêm handler.
        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    @Test
    @DisplayName("Audit log có entry sau khi admin disable user")
    void auditLogRecordsDisableAction() throws Exception {
        AuthResponse userAuth = registerAndLogin();
        Long userId = fetchUserIdFromMe(userAuth.accessToken());

        String adminToken = loginAdmin();
        HttpHeaders headers = bearerHeaders(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Disable
        restTemplate.exchange("/api/v1/admin/users/" + userId + "/disable",
                HttpMethod.POST, request, String.class);

        // Query audit log
        ResponseEntity<String> auditResp = restTemplate.exchange(
                "/api/v1/admin/audit-log?targetUserId=" + userId,
                HttpMethod.GET, request, String.class);

        assertThat(auditResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(auditResp.getBody());
        assertThat(body.get("items").isArray()).isTrue();
        assertThat(body.get("items").size()).isGreaterThanOrEqualTo(1);

        JsonNode firstEvent = body.get("items").get(0);
        assertThat(firstEvent.get("actionType").asText()).isEqualTo("DISABLE_USER");
        assertThat(firstEvent.get("outcome").asText()).isEqualTo("SUCCESS");
        assertThat(firstEvent.get("targetUserId").asLong()).isEqualTo(userId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String loginAdmin() {
        LoginRequest req = new LoginRequest("admin", "Admin@123");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> e = new HttpEntity<>(req, h);
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
                "/api/v1/auth/login", e, AuthResponse.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().accessToken();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    private Long fetchUserIdFromMe(String accessToken) throws Exception {
        HttpHeaders h = bearerHeaders(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(h);
        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(resp.getBody()).get("id").asLong();
    }
}
