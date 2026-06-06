package com.personal.identity.api.observability;

import com.personal.identity.core.domain.session.SessionRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Gauge counting active sessions system-wide.
 *
 * <h2>Why a separate class (not inlined in IdentityMetrics)</h2>
 *
 * <p>Micrometer's Gauge requires a REFERENCE to be kept alive (it uses a weak reference,
 * which will be garbage collected if left as an inline lambda). This class instance
 * is managed by Spring -> the gauge will not be GC'd.
 *
 * <h2>Trade-off: DB load per scrape</h2>
 *
 * <p>Prometheus scrapes every 15s (default) -> triggers a DB COUNT query every 15s.
 * With Oracle having an index on {@code sessions(status, expires_at)}, this COUNT takes
 * under 10ms for millions of rows. DO NOT cache because:
 * <ul>
 * <li>Stale cache value -> inaccurate gauge -> incorrect alerting.</li>
 * <li>Cache invalidation complexity outweighs the benefits.</li>
 * <li>Spring already provides Hibernate query caching if enabled.</li>
 * </ul>
 *
 * <h2>Temporary implementation</h2>
 *
 * <p>{@code SessionRepository} currently lacks {@code countActive()}. The gauge lambda
 * returns a 0 placeholder. Once the method is added to the port, update the supplier -
 * without changing the metric name (to maintain the Prometheus series).
 */
public class ActiveSessionsGauge {
    private final SessionRepository sessionRepository;

    @Autowired
    public ActiveSessionsGauge(MeterRegistry registry, SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        Gauge.builder("identity_sessions_active_count", this, ActiveSessionsGauge::countActive)
                // Translated description: Number of ACTIVE sessions system-wide (snapshot during Prometheus scrape)
                .description("Total ACTIVE sessions system-wide (snapshot during Prometheus scrape)")
                .register(registry);
    }

    /**
     * Supplier for the gauge. DO NOT throw - an exception will cause the gauge
     * to return NaN in the metric.
     * Catch and return 0 (better than crashing the entire scrape).
     */
    private double countActive() {
        try {
            // TODO Phase 2: add port method long countActive() to SessionRepository
            // + native Oracle query COUNT(*) WHERE status='ACTIVE' AND expires_at > SYSDATE.
            // Currently lacks the method, returning 0 as a placeholder.
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
