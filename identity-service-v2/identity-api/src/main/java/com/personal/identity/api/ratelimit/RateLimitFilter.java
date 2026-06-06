package com.personal.identity.api.ratelimit;



import com.personal.identity.api.dto.ErrorResponse;
import com.personal.identity.api.util.RequestContextExtractor;
import com.personal.identity.core.application.ratelimit.RateLimitDecision;
import com.personal.identity.core.application.ratelimit.RateLimiterPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Servlet filter enforcing rate limits for login, register, and refresh endpoints.
 *
 * <h2>Position in the Filter Chain</h2>
 *
 * <p>This filter is registered BEFORE {@code JwtAuthenticationFilter}
 * (see SecurityConfig.addFilterBefore). Reasons:
 * <ul>
 * <li>IP-based rate limiting does not require a parsed JWT.</li>
 * <li>Early blocking saves CPU cycles by avoiding JWT parsing for requests destined to be rejected anyway.</li>
 * <li>Placing it right after the main Spring Security filter chain ensures Spring has already resolved whether
 * the endpoint is public or not, so we do not have to re-evaluate it.</li>
 * </ul>
 *
 * <h2>Why OncePerRequestFilter</h2>
 *
 * <p>Spring can execute a filter multiple times during different request DISPATCH types (e.g., async, error dispatch).
 * {@code OncePerRequestFilter} guarantees that our logic runs exactly once per request - preventing a single physical request
 * from consuming 2 tokens from the bucket.
 *
 * <h2>Response Format</h2>
 *
 * <p>429 Too Many Requests along with a Retry-After header (seconds remaining until 1 token becomes available).
 * The body is formatted as an {@link ErrorResponse} JSON object, mirroring other system errors to ensure
 * consistent frontend handling.
 *
 * <h2>Audit Logging</h2>
 *
 * <p>Logs at WARN level whenever a bucket is depleted - enabling SIEM/Splunk to trigger alerts on rules like
 * "X failed login attempts within Y minutes from the same IP". Does NOT log raw tokens (only logs namespaced hashes or prefixes).
 *
 * <h2>Fail-Open vs Fail-Closed</h2>
 *
 * <p>If Redis goes down, Bucket4j throws an exception. Currently, we fail-open (catching the exception,
 * logging an ERROR, and letting the request pass through). Reason: Redis is considered a soft dependency;
 * a Redis outage should NOT prevent legitimate users from logging in entirely. Trade-off: temporarily loses
 * rate-limiting capabilities during a Redis outage, but isolated alerting handles Redis health monitoring.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(RateLimiterPort.class)
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String PATH_LOGIN = "/api/v1/auth/login";
    private static final String PATH_REGISTER = "/api/v1/auth/register";
    private static final String PATH_REFRESH = "/api/v1/auth/refresh";

    private final RateLimiterPort rateLimiter;
    private final RequestContextExtractor contextExtractor;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        try {
            RateLimitDecision decision = resolveDecision(path, request);

            if (decision == null) {
                chain.doFilter(request, response);
                return;
            }

            if (decision.allowed()) {
                response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingTokens()));
                chain.doFilter(request, response);
                return;
            }

            long retryAfterSeconds = Math.max(1, decision.retryAfter().toSeconds());

            log.warn(
                    "Rate limit exceeded on {} for client {}, retry after {}s",
                    path,
                    resolveClientIp(request),
                    retryAfterSeconds
            );

            writeRateLimitResponse(response, path, retryAfterSeconds);

        } catch (Exception ex) {
            log.error(
                    "Rate limit check failed for {} - failing open and allowing request through",
                    path,
                    ex
            );
            chain.doFilter(request, response);
        }
    }

    private RateLimitDecision resolveDecision(String path, HttpServletRequest request) {
        if (PATH_LOGIN.equals(path)) {
            return rateLimiter.consumeLogin(resolveClientIp(request));
        }

        if (PATH_REGISTER.equals(path)) {
            return rateLimiter.consumeRegister(resolveClientIp(request));
        }

        if (PATH_REFRESH.equals(path)) {
            String refreshKey = extractRefreshTokenHashPrefix(request);
            if (refreshKey == null) {
                refreshKey = resolveClientIp(request);
            }
            return rateLimiter.consumeRefresh(refreshKey);
        }

        return null;
    }

    private String extractRefreshTokenHashPrefix(HttpServletRequest request) {
        /*
         * Do not read the request body here because that would consume the input stream
         * before the controller can parse it.
         *
         * If refresh-token based rate limiting is needed later, add a request body
         * caching wrapper such as ContentCachingRequestWrapper before this filter.
         */
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        return contextExtractor.extract(request).ipAddress();
    }

    private void writeRateLimitResponse(
            HttpServletResponse response,
            String path,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ErrorResponse body = ErrorResponse.of(
                "RATE_LIMIT_EXCEEDED",
                "Too many requests. Please try again after " + retryAfterSeconds + " seconds.",
                path
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}