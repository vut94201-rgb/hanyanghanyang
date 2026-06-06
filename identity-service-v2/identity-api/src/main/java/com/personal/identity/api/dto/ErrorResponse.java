package com.personal.identity.api.dto;

import java.time.Instant;
import java.util.List;

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