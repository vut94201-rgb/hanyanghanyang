package com.personal.identity.api.support;

import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;


/**
 * Base class cho mọi integration test.
 *
 * <h3>Trách nhiệm</h3>
 * <ol>
 *   <li>Boot full Spring context với port random qua {@code @SpringBootTest}.</li>
 *   <li>Inject datasource Oracle Testcontainer + Redis Testcontainer vào Spring qua
 *       {@code @DynamicPropertySource} (chạy TRƯỚC khi context khởi tạo, override config).</li>
 *   <li>Cleanup DB + Redis trước mỗi test method qua {@code @BeforeEach}.</li>
 *   <li>Cung cấp helper {@code registerAndLogin()} để các subclass tái dùng.</li>
 * </ol>
 *
 * <h3>Vì sao dùng @DynamicPropertySource thay vì application-test.yml</h3>
 * <p>Container port ngẫu nhiên (Testcontainers map cổng nội bộ container ra cổng host
 * random để tránh xung đột). Port chỉ biết được lúc runtime, không thể hardcode trong
 * .yml. {@code @DynamicPropertySource} chạy SAU khi container start, TRƯỚC khi Spring
 * load datasource bean - đúng thời điểm cần.
 *
 * <h3>Vì sao cleanup ở @BeforeEach chứ không @AfterEach</h3>
 * <p>Nếu test fail giữa chừng để lại junk data, @AfterEach vẫn chạy → ok. Nhưng nếu
 * JVM crash, @AfterEach không chạy → test sau bị ô nhiễm. Cleanup ở @BeforeEach defensive
 * hơn: trạng thái khởi đầu luôn sạch, không phụ thuộc test trước có cleanup thành công không.
 *
 * <h3>Profile</h3>
 * KHÔNG dùng profile "dev" - profile dev hardcode datasource localhost:1521. Test không
 * có profile active mặc định → đọc application.yml gốc + override bởi {@code @DynamicPropertySource}.
 * JWT secret được override bởi {@link TestSecretsConfig} bên dưới.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected RedisConnectionFactory redisConnectionFactory;

    /**
     * Inject container connection info vào Spring properties NGAY TRƯỚC khi
     * context khởi tạo. Methods static để Spring gọi được mà chưa cần instance.
     *
     * <p>Cũng set {@code JWT_SECRET} ở đây vì application.yml gốc dùng
     * {@code ${JWT_SECRET}} không default - test phải cung cấp.
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (useExternalServices()) {
            registerExternalServiceProperties(registry);
        } else {
            registerContainerProperties(registry);
        }

        // JWT secret cho test - khác với prod, vẫn 384 bit base64.
        registry.add("app.jwt.secret",
                () -> "dGVzdC1zZWNyZXQtZm9yLWludGVncmF0aW9uLXRlc3RzLW9ubHktbm90LWZvci1wcm9k");
        registry.add("app.geoip.fail-on-missing-database", () -> false);
    }

    private static boolean useExternalServices() {
        return Boolean.parseBoolean(propertyOrEnv(
                "identity.test.external-services",
                "IDENTITY_TEST_EXTERNAL_SERVICES",
                "false"));
    }

    private static String propertyOrEnv(String propertyName, String envName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static void registerExternalServiceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> propertyOrEnv(
                "spring.datasource.url",
                "SPRING_DATASOURCE_URL",
                "jdbc:oracle:thin:@localhost:1521/FREEPDB1"));
        registry.add("spring.datasource.username", () -> propertyOrEnv(
                "spring.datasource.username",
                "SPRING_DATASOURCE_USERNAME",
                "identity"));
        registry.add("spring.datasource.password", () -> propertyOrEnv(
                "spring.datasource.password",
                "SPRING_DATASOURCE_PASSWORD",
                "identity123"));
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
        registry.add("spring.data.redis.host", () -> propertyOrEnv(
                "spring.data.redis.host",
                "SPRING_DATA_REDIS_HOST",
                "localhost"));
        registry.add("spring.data.redis.port", () -> propertyOrEnv(
                "spring.data.redis.port",
                "SPRING_DATA_REDIS_PORT",
                "6379"));
    }

    private static void registerContainerProperties(DynamicPropertyRegistry registry) {
        var oracle = OracleTestContainer.getInstance();
        registry.add("spring.datasource.url", oracle::getJdbcUrl);
        registry.add("spring.datasource.username", oracle::getUsername);
        registry.add("spring.datasource.password", oracle::getPassword);
        registry.add("spring.datasource.driver-class-name", oracle::getDriverClassName);

        registry.add("spring.data.redis.host", RedisTestContainer::getHost);
        registry.add("spring.data.redis.port", RedisTestContainer::getPort);
    }

    /**
     * Sạch state trước mỗi test method để test độc lập.
     *
     * <p><b>Thứ tự xóa quan trọng:</b> phải xóa các bảng có FK trước (refresh_tokens
     * → sessions, user_permissions → users), nếu không sẽ vi phạm constraint.
     *
     * <p><b>Không TRUNCATE users vì có seed admin (id=1):</b> chỉ xóa user có id > 1
     * (admin do V3 seed phải giữ để test login admin). User tạo trong test (id ≥ 50)
     * sẽ xóa hết.
     */
    @BeforeEach
    void cleanupState() {
        // Redis: xóa toàn bộ JTI blacklist
        redisConnectionFactory.getConnection().serverCommands().flushAll();

        // DB: xóa theo thứ tự FK
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM sessions");
        jdbcTemplate.execute("DELETE FROM user_permissions WHERE user_id > 1");
        jdbcTemplate.execute("DELETE FROM user_roles WHERE user_id > 1");
        jdbcTemplate.execute("DELETE FROM users WHERE id > 1");
    }

    // ============================================================
    // Helpers cho subclass
    // ============================================================

    /**
     * Build URL với port runtime.
     */
    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Tạo HttpHeaders với Bearer token + JSON content type.
     */
    protected HttpHeaders authHeaders(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (accessToken != null) {
            h.setBearerAuth(accessToken);
        }
        return h;
    }

    /**
     * Register user random (username + email unique) rồi login luôn. Trả AuthResponse
     * gồm access/refresh token + sessionId. Phần lớn test cần "user đã login sẵn".
     */
    protected AuthResponse registerAndLogin() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "user_" + suffix;
        String password = "Password@123";

        // Register
        RegisterRequest reg = new RegisterRequest(
                username,
                username + "@example.com",
                password,
                "Test User " + suffix
        );
        var regResp = restTemplate.postForEntity(
                url("/api/v1/auth/register"),
                new HttpEntity<>(reg, authHeaders(null)),
                Object.class);
        if (!regResp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Register failed: " + regResp);
        }

        // Login
        LoginRequest login = new LoginRequest(username, password);
        var loginResp = restTemplate.postForEntity(
                url("/api/v1/auth/login"),
                new HttpEntity<>(login, authHeaders(null)),
                AuthResponse.class);
        if (!loginResp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Login failed: " + loginResp);
        }
        return loginResp.getBody();
    }

    /**
     * Helper gọi endpoint kèm token. Tránh boilerplate ở subclass.
     */
    protected <T> org.springframework.http.ResponseEntity<T> exchange(
            String path, HttpMethod method, String accessToken, Object body, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(body, authHeaders(accessToken));
        return restTemplate.exchange(url(path), method, entity, responseType);
    }

    /**
     * Inner configuration để inject TestRestTemplate KHÔNG follow redirect (tránh
     * 302 redirect tới /login HTML khi gọi endpoint protected mà thiếu token - ta
     * muốn nhận trực tiếp 401/403).
     */
    @Configuration
    static class TestSecretsConfig {
        // Reserved cho future test config (vd: clock cho test time-based logic)
        @Bean
        public Object testMarker() {
            return new Object();
        }
    }
}
