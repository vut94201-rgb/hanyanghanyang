package com.personal.identity.api.observability;

import com.personal.identity.core.session.SessionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom business metrics cho identity service.
 *
 * <h2>Vì sao tự define</h2>
 *
 * <p>Spring Boot Actuator tự expose hàng tá metric kỹ thuật (JVM, HTTP request,
 * DataSource). Nhưng câu hỏi business "bao nhiêu login fail trong 5 phút" không
 * answer được bằng built-in - cần counter riêng mà code business gọi {@code increment()}.
 *
 * <h2>Naming convention</h2>
 *
 * <p>Theo Prometheus convention: lowercase, snake_case, có prefix domain
 * ({@code identity_} ), suffix unit khi áp dụng được ({@code _total} cho
 * counter, {@code _seconds} cho timer, không suffix cho gauge).
 *
 * <h2>Tags</h2>
 *
 * <p>Counter login phân biệt success/failure qua tag {@code outcome}. Dùng tag
 * thay vì 2 counter riêng để query Grafana dễ:
 * {@code rate(identity_auth_login_total{outcome="failure"}[5m])} - kéo curve
 * fail riêng. Tag values phải hữu hạn (cardinality cao = Prometheus chậm) -
 * 2 value success/failure ok.
 *
 * <h2>Gauge active sessions</h2>
 *
 * <p>Gauge KHÔNG accumulate như counter - mỗi lần Prometheus scrape, Micrometer
 * gọi supplier lambda và lấy snapshot. Supplier truy vấn DB - nên cache hoặc
 * giới hạn scrape interval (mặc định 15s là OK, nhẹ).
 */
@Configuration
public class IdentityMetrics {

    /**
     * Counter login. Phân biệt outcome:
     *   - "success": login pass, JWT issued
     *   - "failure": invalid credentials (BadCredentials, UserNotFound, ...)
     *   - "blocked": user DISABLED/LOCKED (≠ wrong password)
     *
     * <p>KHÔNG add tag {@code username} - cardinality vô hạn (mỗi username = 1 series mới
     * trong Prometheus, vài chục nghìn user = vài chục nghìn series = chết Prom).
     */
    @Bean
    public LoginMetrics loginMetrics(MeterRegistry registry) {
        Counter successCounter = Counter.builder("identity_auth_login_total")
                .description("Tổng số lần login phân theo outcome")
                .tag("outcome", "success")
                .register(registry);
        Counter failureCounter = Counter.builder("identity_auth_login_total")
                .tag("outcome", "failure")
                .register(registry);
        Counter blockedCounter = Counter.builder("identity_auth_login_total")
                .tag("outcome", "blocked")
                .register(registry);

        Timer loginTimer = Timer.builder("identity_auth_login_duration_seconds")
                .description("Latency của login flow (gồm DB lookup + BCrypt verify)")
                .publishPercentileHistogram()  // Cho phép query histogram_quantile() ở Grafana
                .register(registry);

        return new LoginMetrics(successCounter, failureCounter, blockedCounter, loginTimer);
    }

    /**
     * Gauge active sessions. Update lazy mỗi lần Prometheus scrape (~15s).
     *
     * <p>Implementation note: SessionRepository hiện chưa có method countActive().
     * Tạm dùng size của findActiveByUserId — KHÔNG ổn cho production (load toàn bộ).
     * Step L1 này skip — gauge sẽ wire ở phase 2 sau khi thêm countActive() port.
     */
    @Bean
    public ActiveSessionsGauge activeSessionsGauge(MeterRegistry registry,
                                                    SessionRepository sessionRepository) {
        return new ActiveSessionsGauge(registry, sessionRepository);
    }

    /**
     * Holder pattern - inject vào controller/use case để increment.
     * Record vì immutable, expose direct field.
     */
    public record LoginMetrics(
            Counter loginSuccess,
            Counter loginFailure,
            Counter loginBlocked,
            Timer loginTimer
    ) {
    }
}
