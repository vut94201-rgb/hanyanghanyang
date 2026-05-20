package com.personal.identity.api.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.identity.api.dto.ErrorResponse;
import com.personal.identity.api.util.RequestContextExtractor;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter check rate limit cho login/register/refresh.
 *
 * <h2>Vị trí trong chain</h2>
 *
 * <p>Filter này đăng ký TRƯỚC {@code JwtAuthenticationFilter} (xem
 * SecurityConfig.addFilterBefore). Lý do:
 * <ul>
 *   <li>Rate limit theo IP - không cần JWT đã parse.</li>
 *   <li>Block sớm = đỡ load CPU parse JWT cho request sắp bị reject.</li>
 *   <li>Đặt sau Spring Security filter chain main thì Spring đã decide endpoint
 *       public hay không, mình không cần redo.</li>
 * </ul>
 *
 * <h2>Tại sao OncePerRequestFilter</h2>
 *
 * <p>Spring có thể chain filter vào request DISPATCH (vd async, error dispatch).
 * {@code OncePerRequestFilter} đảm bảo logic chỉ chạy 1 lần/request - bucket
 * không bị consume 2 token cho 1 request thật.
 *
 * <h2>Response format</h2>
 *
 * <p>429 Too Many Requests + Retry-After header (giây tới khi đủ 1 token).
 * Body là {@link ErrorResponse} JSON giống như các error khác - frontend xử
 * lý nhất quán.
 *
 * <h2>Audit log</h2>
 *
 * <p>WARN level mỗi lần bucket cạn - SIEM/Splunk có thể alert "X login fail
 * trong Y phút từ cùng IP". KHÔNG log token (chỉ prefix hash).
 *
 * <h2>Fail-open vs fail-closed</h2>
 *
 * <p>Nếu Redis down → bucket4j throw exception. Hiện tại ta fail-open (catch
 * exception, log ERROR, cho request đi qua). Lý do: Redis là dependency mềm,
 * Redis down KHÔNG nên làm cả service không login được. Trade-off: ngắn hạn
 * mất rate limit nếu Redis down, nhưng có alerting riêng cho Redis health.
 */
@Component
@ConditionalOnBean(RateLimiterRegistry.class)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String PATH_LOGIN = "/api/v1/auth/login";
    private static final String PATH_REGISTER = "/api/v1/auth/register";
    private static final String PATH_REFRESH = "/api/v1/auth/refresh";

    private final RateLimiterRegistry registry;
    private final RequestContextExtractor contextExtractor;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        // Chỉ POST mới check rate limit. GET (vd: actuator) bỏ qua.
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        BucketProxy bucket = resolveBucket(path, request);

        // Endpoint không nằm trong scope rate limit → pass through.
        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                // Còn token → cho đi qua. Có thể thêm header X-RateLimit-Remaining
                // để client biết trước khi bị 429.
                response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
                chain.doFilter(request, response);
                return;
            }

            // Hết token → block.
            long waitMillis = probe.getNanosToWaitForRefill() / 1_000_000;
            long retryAfterSeconds = Math.max(1, waitMillis / 1000);

            log.warn("Rate limit exceeded on {} for client {}: bucket empty, retry after {}s",
                    path, resolveClientIp(request), retryAfterSeconds);

            writeRateLimitResponse(response, path, retryAfterSeconds);

        } catch (Exception ex) {
            // Fail-open: Redis hỏng, không block user login. Log ERROR để alert.
            log.error("Rate limit check failed for {} - failing open (allowing request through)",
                    path, ex);
            chain.doFilter(request, response);
        }
    }

    /**
     * Chọn bucket dựa vào path. Trả null nếu path không cần rate limit.
     */
    private BucketProxy resolveBucket(String path, HttpServletRequest request) {
        if (PATH_LOGIN.equals(path)) {
            return registry.forLogin(resolveClientIp(request));
        }
        if (PATH_REGISTER.equals(path)) {
            return registry.forRegister(resolveClientIp(request));
        }
        if (PATH_REFRESH.equals(path)) {
            // Refresh key: lấy 8 ký tự đầu hash của refresh token thay vì IP.
            // Lý do: refresh được gửi từ nhiều device cùng tài khoản, IP có thể
            // đổi liên tục (mobile network). Key theo token thì rate-limit chính
            // xác hơn (1 token = 1 session). Nếu không lấy được token, fallback IP.
            String tokenHashPrefix = extractRefreshTokenHashPrefix(request);
            if (tokenHashPrefix != null) {
                return registry.forRefresh(tokenHashPrefix);
            }
            return registry.forRefresh(resolveClientIp(request));
        }
        return null;
    }

    /**
     * Lấy 8 ký tự đầu hash của refresh token từ body request.
     *
     * <p>KHÔNG đọc body trực tiếp (sẽ consume input stream, controller không
     * còn body để parse). Đơn giản fallback IP cho refresh - production muốn
     * chính xác hơn thì cần caching wrapper (ContentCachingRequestWrapper).
     */
    private String extractRefreshTokenHashPrefix(HttpServletRequest request) {
        // Đơn giản hoá: dùng IP cho refresh. Nâng cấp sau nếu cần.
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
                "Quá nhiều yêu cầu. Vui lòng thử lại sau " + retryAfterSeconds + " giây.",
                path
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
