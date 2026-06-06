package com.personal.identity.api.observability;

import com.personal.identity.core.domain.session.SessionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Custom business metrics for identity service.
 *
 * <h2>Why self-define</h2>
 *
 * <p>Spring Boot Actuator automatically exposes dozens of technical metrics (JVM, HTTP request,
 * DataSource). But business questions like "how many failed logins in 5 minutes" cannot
 * be answered by built-in metrics - a custom counter is needed, which the business code invokes via {@code increment()}.
 *
 * <h2>Naming convention</h2>
 *
 * <p>According to Prometheus convention: lowercase, snake_case, with domain prefix
 * ({@code identity_}), unit suffix when applicable ({@code _total} for
 * counters, {@code _seconds} for timers, no suffix for gauges).
 *
 * <h2>Tags</h2>
 *
 * <p>The login counter differentiates success/failure via the {@code outcome} tag. Using tags
 * instead of 2 separate counters makes Grafana queries easier:
 * {@code rate(identity_auth_login_total{outcome="failure"}[5m])} - pulls the failure curve
 * separately. Tag values must be bounded (high cardinality = slows down Prometheus) -
 * 2 values for success/failure are ok.
 *
 * <h2>Gauge active sessions</h2>
 *
 * <p>Gauges do NOT accumulate like counters - every time Prometheus scrapes, Micrometer
 * invokes the supplier lambda and takes a snapshot. The supplier queries the DB - therefore, it should be cached or
 * the scrape interval should be limited (the default 15s is OK, lightweight).
 */
@Configuration
public class IdentityMetrics {
    /**
     * Login counter. Differentiates outcome:
     * - "success": login pass, JWT issued
     * - "failure": invalid credentials (BadCredentials, UserNotFound, ...)
     * - "blocked": user DISABLED/LOCKED (≠ wrong password)
     *
     * <p>Do NOT add {@code username} tag - infinite cardinality (each username = 1 new series
     * in Prometheus, tens of thousands of users = tens of thousands of series = Prometheus crashes).
     */
    @Bean
    public LoginMetrics loginMetrics(MeterRegistry registry) {
        Counter successCounter = Counter.builder("identity_auth_login_total")
                .description("Total number of logins segmented by outcome")
                .tag("outcome", "success")
                .register(registry);

        Counter failureCounter = Counter.builder("identity_auth_login_total")
                .tag("outcome", "failure")
                .register(registry);

        Counter blockedCounter = Counter.builder("identity_auth_login_total")
                .tag("outcome", "blocked")
                .register(registry);

        Timer loginTimer = Timer.builder("identity_auth_login_duration_seconds")
                .description("Latency of the login flow (includes DB lookup + BCrypt verify)")
                .publishPercentileHistogram() // Allows querying histogram_quantile() in Grafana
                .register(registry);

        return new LoginMetrics(successCounter, failureCounter, blockedCounter, loginTimer);
    }

    /**
     * Gauge active sessions. Lazy update on every Prometheus scrape (~15s).
     *
     * <p>Implementation note: SessionRepository does not have a countActive() method yet.
     * Temporarily using the size of findActiveByUserId – NOT optimal for production (loads everything).
     * Skip this L1 step – the gauge will be wired in phase 2 after adding the countActive() port.
     */
    @Bean
    public ActiveSessionsGauge activeSessionsGauge(MeterRegistry registry,
                                                   SessionRepository sessionRepository) {
        return new ActiveSessionsGauge(registry, sessionRepository);
    }

    /**
     * Holder pattern - inject into controller/use case to increment.
     * Record for immutability, exposes direct fields.
     */
    public record LoginMetrics(
            Counter loginSuccess,
            Counter loginFailure,
            Counter loginBlocked,
            Timer loginTimer
    ) {
    }
}
