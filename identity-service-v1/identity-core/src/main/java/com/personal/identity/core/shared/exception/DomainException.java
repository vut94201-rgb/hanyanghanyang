package com.personal.identity.core.shared.exception;

/**
 * Base cho TẤT CẢ exception phát sinh từ business logic của domain.
 *
 * <p>Khác với {@link RuntimeException} thông thường ở chỗ:
 * <ul>
 *   <li>Tất cả exception domain đều extends class này → {@code GlobalExceptionHandler}
 *       (ở module api) bắt 1 lần cho cả nhóm là đủ.</li>
 *   <li>Mang theo {@link #errorCode} - mã ngắn gọn, ổn định để frontend / client
 *       dùng làm key dịch i18n hoặc switch logic. Khác với message (có thể đổi
 *       theo ngôn ngữ), errorCode KHÔNG đổi.</li>
 * </ul>
 *
 * <p><b>Quy ước đặt errorCode:</b>
 * {@code <DOMAIN>.<KIND>} viết hoa, dùng dấu chấm phân tách. Ví dụ:
 * <ul>
 *   <li>{@code USER.NOT_FOUND}</li>
 *   <li>{@code USER.DUPLICATE_USERNAME}</li>
 *   <li>{@code AUTH.INVALID_CREDENTIALS}</li>
 *   <li>{@code TOKEN.REUSE_DETECTED}</li>
 * </ul>
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
