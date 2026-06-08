package com.personal.identity.core.domain.shared.exception;

import lombok.Getter;

import java.io.Serial;
import java.util.Objects;

/**
 * Base for ALL exceptions arising from domain business logic.
 *
 * <p>Differs from a standard {@link RuntimeException} in that:
 * <ul>
 * <li>All domain exceptions extend this class -> {@code GlobalExceptionHandler}
 * (in the API module) catching this single base class is sufficient for the entire group.</li>
 * <li>Carries an {@link #errorCode} - a concise, stable code for frontends / clients
 * to use as an i18n translation key or for switch logic. Unlike the message (which can change
 * based on language), the errorCode remains UNCHANGED.</li>
 * </ul>
 *
 * <p><b>Naming convention for errorCode:</b>
 * {@code <DOMAIN>.<KIND>} in uppercase, separated by a dot. For example:
 * <ul>
 * <li>{@code USER.NOT_FOUND}</li>
 * <li>{@code USER.DUPLICATE_USERNAME}</li>
 * <li>{@code AUTH.INVALID_CREDENTIALS}</li>
 * <li>{@code TOKEN.REUSE_DETECTED}</li>
 * </ul>
 */
@Getter
public abstract class DomainException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = requireErrorCode(errorCode);
    }

    protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = requireErrorCode(errorCode);
    }

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = requireErrorCode(errorCode);
    }

    private static ErrorCode requireErrorCode(ErrorCode errorCode) {
        return Objects.requireNonNull(errorCode, "errorCode must not be null");
    }
}
