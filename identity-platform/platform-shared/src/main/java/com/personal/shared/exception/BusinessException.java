package com.personal.shared.exception;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
        super(detailMessage, cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    /**
     * Convenience accessor for the stable error code string,
     * e.g. {@code "AUTH-101"}.
     */
    public String code() {
        return errorCode.code();
    }
}
