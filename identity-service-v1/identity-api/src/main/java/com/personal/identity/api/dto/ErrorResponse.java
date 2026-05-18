package com.personal.identity.api.dto;


import java.time.Instant;
import java.util.List;

/**
 * Response thống nhất cho mọi error - dùng bởi GlobalExceptionHandler.
 *
 * <p><b>Format:</b>
 * <pre>
 * {
 *   "errorCode": "USER.DUPLICATE_USERNAME",
 *   "message": "Username already exists: admin",
 *   "path": "/api/v1/auth/register",
 *   "timestamp": "2025-01-15T10:30:00Z",
 *   "fieldErrors": [
 *     { "field": "password", "message": "must not be blank" }
 *   ]
 * }
 * </pre>
 *
 * <p>{@code fieldErrors} chỉ có khi validation lỗi ({@code @Valid} fail). Cho
 * domain exception khác là null - frontend phải null-check.
 *
 * <p>{@code errorCode} stable không đổi theo i18n - frontend dùng làm key dịch
 * và switch logic. {@code message} có thể đổi theo locale sau (chưa làm cho MVP).
 */
public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {

    /**
     * Builder ngắn cho case không có field errors.
     */
    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, path, Instant.now(), null);
    }

    /**
     * Builder cho validation errors.
     */
    public static ErrorResponse ofValidation(String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                "VALIDATION.FAILED",
                "Request body validation failed",
                path,
                Instant.now(),
                fieldErrors
        );
    }

    /**
     * Chi tiết lỗi cho 1 field cụ thể.
     */
    public record FieldError(String field, String message) {
    }
}