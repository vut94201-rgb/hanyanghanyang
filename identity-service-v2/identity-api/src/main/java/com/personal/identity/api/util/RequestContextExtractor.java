package com.personal.identity.api.util;

import com.personal.identity.core.domain.session.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;


@Component
public class RequestContextExtractor {

    private static final String UNKNOWN = "unknown";

    public RequestContext extract(HttpServletRequest request) {
        String ipAddress = extractClientIp(request);
        String userAgent = normalize(request.getHeader("User-Agent"));

        return new RequestContext(ipAddress, userAgent);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = normalize(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return firstIp(forwardedFor);
        }

        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String remoteAddress = normalize(request.getRemoteAddr());
        return remoteAddress != null ? remoteAddress : UNKNOWN;
    }

    private String firstIp(String forwardedFor) {
        int commaIndex = forwardedFor.indexOf(',');
        if (commaIndex < 0) {
            return forwardedFor.trim();
        }

        return forwardedFor.substring(0, commaIndex).trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();
        if (UNKNOWN.equalsIgnoreCase(trimmed)) {
            return null;
        }

        return trimmed;
    }
}