package com.personal.identity.api.exception;

import com.personal.identity.api.dto.ErrorResponse;
import com.personal.identity.core.domain.permission.RoleNotFoundException;
import com.personal.identity.core.domain.session.SessionAccessDeniedException;
import com.personal.identity.core.domain.session.SessionNotFoundException;
import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.token.InvalidRefreshTokenException;
import com.personal.identity.core.domain.token.TokenReuseDetectedException;
import com.personal.identity.core.domain.user.DuplicateEmailException;
import com.personal.identity.core.domain.user.DuplicateUsernameException;
import com.personal.identity.core.domain.user.InvalidCredentialsException;
import com.personal.identity.core.domain.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Maps exceptions to a standard HTTP response (JSON {@link ErrorResponse}).
 *
 * <h2>Mapping Strategy</h2>
 * <table>
 * <tr><th>Exception</th><th>HTTP Status</th><th>Log Level</th></tr>
 * <tr><td>InvalidCredentialsException</td><td>401</td><td>DEBUG</td></tr>
 * <tr><td>InvalidRefreshTokenException</td><td>401</td><td>DEBUG</td></tr>
 * <tr><td>TokenReuseDetectedException</td><td>401</td><td><b>WARN</b> (potential attack indicator)</td></tr>
 * <tr><td>SessionAccessDeniedException</td><td>403</td><td><b>WARN</b> (potential IDOR indicator)</td></tr>
 * <tr><td>UserNotFoundException</td><td>404</td><td>DEBUG</td></tr>
 * <tr><td>SessionNotFoundException</td><td>404</td><td>DEBUG</td></tr>
 * <tr><td>RoleNotFoundException</td><td>500</td><td>ERROR (system configuration error)</td></tr>
 * <tr><td>DuplicateUsername/EmailException</td><td>409</td><td>DEBUG</td></tr>
 * <tr><td>MethodArgumentNotValidException</td><td>400</td><td>DEBUG (field errors)</td></tr>
 * <tr><td>IllegalArgumentException</td><td>400</td><td>DEBUG</td></tr>
 * <tr><td>Other DomainExceptions (catch-all)</td><td>500</td><td>ERROR</td></tr>
 * <tr><td>Exception (ultimate catch-all)</td><td>500</td><td>ERROR</td></tr>
 * </table>
 *
 * <h2>Log Level Philosophy</h2>
 *
 * <p><b>DEBUG for "routine" events:</b> Incorrect passwords, expired tokens, client-side validation
 * failures — these occur thousands of times a day in production. Logging them at INFO/WARN
 * would flood the logs and obscure truly critical events.
 *
 * <p><b>WARN for potential attacks:</b> Exceptions like {@link TokenReuseDetectedException} and
 * {@link SessionAccessDeniedException} indicate that an attacker is actively probing the system — worth
 * keeping for forensic analysis.
 *
 * <p><b>ERROR for system faults:</b> For instance, a {@link RoleNotFoundException} for the default USER
 * role implies missing or incorrect seed data. This is a configuration issue requiring immediate resolution.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final com.personal.identity.api.observability.IdentityMetrics.LoginMetrics loginMetrics;

    @Autowired
    public GlobalExceptionHandler(
            com.personal.identity.api.observability.IdentityMetrics.LoginMetrics loginMetrics) {
        this.loginMetrics = loginMetrics;
    }

    // ============================================================
    // 401 - Authentication errors
    // ============================================================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.debug("Invalid credentials: {}", ex.getMessage());
        if ("/api/v1/auth/login".equals(request.getRequestURI())) {
            loginMetrics.loginFailure().increment();
        }
        return build(HttpStatus.UNAUTHORIZED, ex, request);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest request) {
        log.debug("Invalid refresh token: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex, request);
    }

    @ExceptionHandler(TokenReuseDetectedException.class)
    public ResponseEntity<ErrorResponse> handleTokenReuseDetected(
            TokenReuseDetectedException ex, HttpServletRequest request) {
        // WARN: Indicator of a potential attack - warrants alerting and forensic logging.
        // The endpoint path is included to correlate easily with access logs.
        log.warn("Token reuse detected on path {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex, request);
    }

    // ============================================================
    // 403 - Authorization errors (authenticated but lacking permissions)
    // ============================================================

    /**
     * IDOR attempt: An authenticated user is attempting to access another user's resources.
     * Logged at WARN so monitoring/SIEM systems can trigger alerts upon detecting anomalous
     * patterns (e.g., 1 user transmitting 100 different sessionIds within a minute).
     */
    @ExceptionHandler(SessionAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSessionAccessDenied(
            SessionAccessDeniedException ex, HttpServletRequest request) {
        log.warn("Session access denied (possible IDOR) on path {}: {}",
                request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex, request);
    }

    // ============================================================
    // 404 - Resource not found
    // ============================================================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        log.debug("User not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(
            SessionNotFoundException ex, HttpServletRequest request) {
        log.debug("Session not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        // Route does not exist - provides a cleaner response than Spring's default whitelabel page.
        log.debug("No handler for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse body = ErrorResponse.of("HTTP.NOT_FOUND",
                "Endpoint does not exist: " + ex.getHttpMethod() + " " + ex.getRequestURL(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ============================================================
    // 409 - Conflict
    // ============================================================

    @ExceptionHandler(DuplicateUsernameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUsername(
            DuplicateUsernameException ex, HttpServletRequest request) {
        log.debug("Duplicate username: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        log.debug("Duplicate email: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex, request);
    }

    // ============================================================
    // 400 - Bad request
    // ============================================================

    /**
     * Validation failure (Bean Validation, {@code @Valid}). Returns all field errors
     * consolidated into a single response - allowing the frontend to display them inline beneath each field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        log.debug("Validation failed on {}: {} field errors",
                request.getRequestURI(), fieldErrors.size());

        ErrorResponse body = ErrorResponse.ofValidation(request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("Illegal argument: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                "REQUEST.ILLEGAL_ARGUMENT",
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    // ============================================================
    // 500 - System errors
    // ============================================================

    /**
     * The default USER role was deleted or renamed - this is a system configuration error,
     * not a user-induced error. Logged at ERROR to trigger operational alerts.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(
            RoleNotFoundException ex, HttpServletRequest request) {
        log.error("Role not found - configuration issue: {}", ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    /**
     * Catch-all for DomainExceptions not explicitly mapped above. Defensive approach:
     * if a new DomainException is added in the future and forgotten here, the response
     * will at least contain a structured errorCode (avoiding a fallback to the whitelabel error).
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(
            DomainException ex, HttpServletRequest request) {
        log.error("Unhandled domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    /**
     * Spring Security 6 throws {@link org.springframework.security.authorization.AuthorizationDeniedException}
     * when {@code @PreAuthorize} fails. The default exception resolver treats this as a generic
     * Exception → returning 500. We override it here to return 403.
     *
     * <p>Note: This must be placed BEFORE the {@code Exception.class} fallback so Spring can match
     * this specific exception class first.
     */
    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex,
            HttpServletRequest request
    ) {
        log.debug("Authorization denied on {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                "FORBIDDEN",
                "You do not have permission to perform this action.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * The ultimate catch-all. NEVER leak stacktraces in the HTTP response - only return a
     * generic error message. The stacktrace remains localized in the server logs (at level ERROR) for debugging.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                "INTERNAL.UNEXPECTED",
                "An unexpected error has occurred. Please contact the administrator if the issue persists.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private ResponseEntity<ErrorResponse> build(HttpStatus status, DomainException ex,
                                                HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                ex.getErrorCode().getCode(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (message == null) {
            message = "invalid";
        }
        return new ErrorResponse.FieldError(fieldError.getField(), message);
    }
}