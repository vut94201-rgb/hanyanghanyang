package com.personal.identity.core.session;

/**
 * Bundle thông tin từ HTTP request mà domain layer cần biết khi tạo session
 * hoặc rotate refresh token.
 *
 * <p>API layer extract từ {@code HttpServletRequest} (qua {@code RequestContextExtractor}
 * sẽ viết ở bước sau), rồi truyền xuống dưới như value object. Nhờ vậy core service
 * KHÔNG biết về {@code HttpServletRequest} - dễ test (chỉ cần new RequestContext).
 *
 * @param ipAddress       IP đã xử lý X-Forwarded-For nếu có proxy. Không null.
 * @param rawUserAgent    Raw User-Agent header. Null nếu client không gửi (curl bare).
 */
public record RequestContext(
        String ipAddress,
        String rawUserAgent
) {
    public RequestContext {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("ipAddress is required");
        }
    }
}
