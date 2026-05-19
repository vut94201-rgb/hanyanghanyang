package com.personal.identity.api.support;


import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Oracle 23 Free container - dùng chung cho TẤT CẢ integration test.
 *
 * <h3>Vì sao Singleton thay vì 1 container/test class</h3>
 * <ul>
 *   <li>Oracle Free khởi động chậm (60-90s) - mỗi test class tự spin container sẽ
 *       tốn hàng phút cho 1 lần run. Singleton chỉ trả 1 lần phí.</li>
 *   <li>JVM kết thúc → Docker daemon tự dọn container (Testcontainers Ryuk sidecar).</li>
 *   <li>Sạch dữ liệu giữa các test bằng Flyway clean + migrate, KHÔNG drop container.</li>
 * </ul>
 *
 * <h3>Cảnh báo & trade-off</h3>
 * <ul>
 *   <li><b>Test KHÔNG được chạy song song (parallel)</b> vì share state DB. Đã giả
 *       định Surefire chạy tuần tự (Spring Boot 3 default).</li>
 *   <li>Test PHẢI cleanup data ở {@code @BeforeEach} (xem {@link IntegrationTestBase}).</li>
 * </ul>
 *
 * <h3>Image dùng</h3>
 * {@code gvenzl/oracle-free:23-slim-faststart} - image community phổ biến nhất cho
 * Oracle 23 Free, có flag {@code -faststart} bỏ qua wait checks → khởi động ~30s thay
 * vì 90s. Vẫn là Oracle thật (không phải XE).
 */
public final class OracleTestContainer {

    private static final OracleContainer CONTAINER;

    static {
        // withReuse(true): nếu chạy local nhiều lần liên tiếp, Testcontainers sẽ
        // KHÔNG tắt container giữa các lần - tái sử dụng. Phải bật flag
        // testcontainers.reuse.enable=true ở ~/.testcontainers.properties.
        // CI thường tắt reuse - mỗi run có container riêng.
        CONTAINER = new OracleContainer(
                DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart"))
                .withUsername("identity")
                .withPassword("identity123")
                .withReuse(true);
        CONTAINER.start();
    }

    private OracleTestContainer() {
        // static-only
    }

    public static OracleContainer getInstance() {
        return CONTAINER;
    }
}
