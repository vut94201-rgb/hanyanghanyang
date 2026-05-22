package com.personal.identity.api;

import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test observability endpoint + custom metric.
 *
 * <h2>Setup</h2>
 *
 * <p>{@code IntegrationTestBase} dùng profile "test" → actuator endpoint default
 * available (profile test không tắt actuator). Test verify:
 * <ul>
 *   <li>{@code /actuator/health} trả 200 + JSON có "status":"UP"</li>
 *   <li>{@code /actuator/prometheus} trả 200 + content có metric custom</li>
 *   <li>Counter login increment sau khi /login được gọi</li>
 * </ul>
 *
 * <h2>Lưu ý management port</h2>
 *
 * <p>Profile test KHÔNG có management.server.port override (chỉ prod profile mới
 * dùng port 9090). Vậy actuator endpoint vẫn ở cùng port 8080 với app → {@code restTemplate}
 * gọi base URL như endpoint thường được.
 */
class ObservabilityTest extends IntegrationTestBase {

    @Test
    @DisplayName("/actuator/health trả 200 + status UP")
    void healthEndpointUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("/actuator/prometheus expose metric format Prometheus")
    void prometheusEndpointExposeMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        // Prometheus format: # HELP, # TYPE, sau đó là metric_name{labels} value
        assertThat(body).contains("# TYPE");
        // JVM metric tự có
        assertThat(body).contains("jvm_memory_used_bytes");
        // HTTP metric tự có (Spring Boot Actuator)
        assertThat(body).contains("http_server_requests_seconds");
    }

    @Test
    @DisplayName("Sau khi login fail, counter identity_auth_login_total{outcome=\"failure\"} tăng")
    void loginFailureIncrementsCounter() {
        // Lấy baseline counter trước
        long baseline = extractCounter("identity_auth_login_total", "outcome=\"failure\"");

        // Gọi /login với credential sai → counter phải tăng
        LoginRequest fail = new LoginRequest("nonexistent_user", "wrong_password");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(fail, headers);
        restTemplate.postForEntity("/api/v1/auth/login", request, String.class);

        long after = extractCounter("identity_auth_login_total", "outcome=\"failure\"");
        assertThat(after).isGreaterThan(baseline);
    }

    @Test
    @DisplayName("Sau khi login success, counter identity_auth_login_total{outcome=\"success\"} tăng")
    void loginSuccessIncrementsCounter() {
        long baseline = extractCounter("identity_auth_login_total", "outcome=\"success\"");

        // Login admin (luôn có sẵn từ seed)
        LoginRequest req = new LoginRequest("admin", "Admin@123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> request = new HttpEntity<>(req, headers);
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login", request, AuthResponse.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        long after = extractCounter("identity_auth_login_total", "outcome=\"success\"");
        assertThat(after).isGreaterThan(baseline);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Parse Prometheus exposition format và trả về value của metric khớp
     * tên + label substring.
     *
     * <p>Format Prometheus line: {@code metric_name{label1="x",label2="y"} 42.0}
     * Tham số {@code labelSubstring} = một label fragment để filter
     * (vd: {@code outcome="failure"}).
     *
     * @return value (long), 0 nếu không tìm thấy
     */
    private long extractCounter(String metricName, String labelSubstring) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/actuator/prometheus", String.class);
        if (response.getBody() == null) return 0L;

        for (String line : response.getBody().split("\n")) {
            if (line.startsWith(metricName)
                    && line.contains(labelSubstring)) {
                // Lấy value sau khoảng trắng cuối
                int lastSpace = line.lastIndexOf(' ');
                if (lastSpace > 0 && lastSpace < line.length() - 1) {
                    try {
                        return (long) Double.parseDouble(line.substring(lastSpace + 1).trim());
                    } catch (NumberFormatException e) {
                        // Bỏ qua line "# HELP" / "# TYPE"
                    }
                }
            }
        }
        return 0L;
    }
}
