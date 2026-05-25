package com.personal.identity.api.util;


import com.personal.identity.core.session.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Convert {@link HttpServletRequest} → {@link RequestContext} của core.
 *
 * <p><b>Vì sao tách thành component:</b> 2 controller method (login, refresh)
 * đều cần extract IP + UA. Tách helper để khỏi duplicate, và để test được riêng
 * logic parse X-Forwarded-For mà không cần MockMvc.
 *
 * <p>Core KHÔNG biết về {@code HttpServletRequest} - đó là chi tiết HTTP. Helper
 * này thuộc tầng api, làm "adapter" từ servlet API → domain value object.
 *
 * <h2>Xử lý X-Forwarded-For</h2>
 *
 * <p>Khi service đứng sau proxy/load balancer (nginx, AWS ALB, Cloudflare),
 * {@code request.getRemoteAddr()} trả IP của proxy, KHÔNG phải IP thật của client.
 * Proxy gắn IP client vào header {@code X-Forwarded-For}.
 *
 * <p><b>Format X-Forwarded-For:</b> {@code "client_ip, proxy1_ip, proxy2_ip"}.
 * IP đầu tiên LUÔN là IP client. Các IP sau là chuỗi proxy trung gian.
 *
 * <p><b>Bẫy security: TRUST nguồn header.</b> Nếu chấp nhận X-Forwarded-For từ
 * MỌI request, attacker có thể spoof IP bằng cách tự gửi header này. Phải:
 * <ul>
 *   <li>Chỉ trust header nếu request đến từ proxy đã biết (whitelist proxy IP), HOẶC</li>
 *   <li>Cấu hình ở proxy "strip & re-set X-Forwarded-For" để header từ client bị xóa.</li>
 * </ul>
 *
 * <p>Spring Boot có {@code ForwardedHeaderFilter} làm chuyện này chuẩn. Bật bằng
 * {@code server.forward-headers-strategy=native} trong yml khi deploy sau proxy.
 * Hiện tại dev local không có proxy → header này không xuất hiện → fallback về
 * {@code getRemoteAddr()}. KHÔNG hard-code logic strip trong code - đó là việc
 * của infrastructure (proxy/Spring filter).
 *
 * <p><b>Note:</b> Cách trong code này (tự parse header) thực ra KHÔNG SAFE khi
 * deploy sau proxy chưa cấu hình strip. Để giải quyết đúng:
 * <ol>
 *   <li>Production: set {@code server.forward-headers-strategy=framework} trong yml,
 *       Spring sẽ tự dùng {@code ForwardedHeaderFilter} - lúc đó {@code getRemoteAddr()}
 *       trả đúng IP client.</li>
 *   <li>Code này dùng làm fallback cho dev (không có proxy).</li>
 * </ol>
 */
@Component
public class RequestContextExtractor {

    /**
     * Header trỏ IP gốc khi đi qua HTTP proxy/CDN. De facto standard.
     */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /**
     * Cloudflare specific. Một số deployment dùng header này thay X-Forwarded-For.
     */
    private static final String CF_CONNECTING_IP = "CF-Connecting-IP";

    /**
     * Tạo {@link RequestContext} từ servlet request.
     */
    public RequestContext extract(HttpServletRequest request) {
        String ipAddress = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        // RequestContext yêu cầu ipAddress không null/blank - đảm bảo có default
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = "0.0.0.0";  // Defensive: getRemoteAddr() hiếm khi null nhưng phòng test
        }
        return new RequestContext(ipAddress, userAgent);
    }

    /**
     * Resolve IP client thực sự. Ưu tiên CF-Connecting-IP → X-Forwarded-For → getRemoteAddr.
     */
    private String resolveClientIp(HttpServletRequest request) {
        // 1. Cloudflare header (nếu dùng CF)
        String cfIp = request.getHeader(CF_CONNECTING_IP);
        if (isValidIp(cfIp)) {
            return cfIp.trim();
        }

        // 2. X-Forwarded-For: lấy IP đầu tiên (IP client gốc)
        String xff = request.getHeader(X_FORWARDED_FOR);
        if (xff != null && !xff.isBlank()) {
            // "client_ip, proxy1, proxy2" → lấy phần trước dấu phẩy
            int commaIdx = xff.indexOf(',');
            String firstIp = (commaIdx > 0) ? xff.substring(0, commaIdx) : xff;
            firstIp = firstIp.trim();
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }

        // 3. Fallback: remote address của TCP connection
        return request.getRemoteAddr();
    }

    /**
     * Sanity check rất nhẹ - KHÔNG validate format IPv4/IPv6 chặt vì
     * {@code RequestContext} không cần. Chỉ check non-blank và không phải "unknown"
     * (một số proxy điền "unknown" khi không xác định được).
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip.trim());
    }
}