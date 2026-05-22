package com.personal.identity.api.observability;

import com.personal.identity.core.session.SessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Gauge đếm session đang active toàn hệ thống.
 *
 * <h2>Tại sao class riêng (không inline ở IdentityMetrics)</h2>
 *
 * <p>Gauge của Micrometer cần REFERENCE giữ sống (weak reference, sẽ GC nếu để
 * inline lambda). Class instance này được Spring giữ → gauge không bị GC.
 *
 * <h2>Trade-off load DB mỗi scrape</h2>
 *
 * <p>Prometheus scrape mỗi 15s (mặc định) → query DB COUNT mỗi 15s. Với Oracle
 * có index trên {@code sessions(status, expires_at)}, COUNT này dưới 10ms cho
 * hàng triệu row. KHÔNG cache vì:
 * <ul>
 *   <li>Cache value bị stale → gauge sai → alerting sai.</li>
 *   <li>Cache invalidation phức tạp hơn lợi ích.</li>
 *   <li>Spring đã có Hibernate query cache nếu bật.</li>
 * </ul>
 *
 * <h2>Implementation tạm thời</h2>
 *
 * <p>SessionRepository hiện chưa có {@code countActive()}. Lambda gauge return 0
 * placeholder. Khi nào port thêm method, sửa supplier - không phải đổi metric name
 * (giữ tiếp series Prometheus).
 */
public class ActiveSessionsGauge {

    private final SessionRepository sessionRepository;

    @Autowired
    public ActiveSessionsGauge(MeterRegistry registry, SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        Gauge.builder("identity_sessions_active_count", this, ActiveSessionsGauge::countActive)
                .description("Số session đang ACTIVE toàn hệ thống (snapshot khi Prometheus scrape)")
                .register(registry);
    }

    /**
     * Supplier cho gauge. KHÔNG throw - exception sẽ làm gauge return NaN ở metric.
     * Catch và return 0 (better than crash entire scrape).
     */
    private double countActive() {
        try {
            // TODO Phase 2: thêm port method long countActive() vào SessionRepository
            //   + native query Oracle COUNT(*) WHERE status='ACTIVE' AND expires_at > SYSDATE.
            // Hiện chưa có method, return 0 placeholder.
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
