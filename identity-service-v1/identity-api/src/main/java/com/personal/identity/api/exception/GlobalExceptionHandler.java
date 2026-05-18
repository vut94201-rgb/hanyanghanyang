package com.personal.identity.api.exception;


import com.personal.identity.api.dto.ErrorResponse;
import com.personal.identity.core.role.RoleNotFoundException;
import com.personal.identity.core.session.SessionNotFoundException;
import com.personal.identity.core.shared.exception.DomainException;
import com.personal.identity.core.token.InvalidRefreshTokenException;
import com.personal.identity.core.token.TokenReuseDetectedException;
import com.personal.identity.core.user.DuplicateEmailException;
import com.personal.identity.core.user.DuplicateUsernameException;
import com.personal.identity.core.user.InvalidCredentialsException;
import com.personal.identity.core.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
/**
 * Map exception → HTTP response chuẩn (JSON {@link ErrorResponse}).
 *
 * <h2>Strategy mapping</h2>
 * <table>
 *   <tr><th>Exception</th><th>HTTP</th><th>Log level</th></tr>
 *   <tr><td>InvalidCredentialsException</td><td>401</td><td>DEBUG</td></tr>
 *   <tr><td>InvalidRefreshTokenException</td><td>401</td><td>DEBUG</td></tr>
 *   <tr><td>TokenReuseDetectedException</td><td>401</td><td><b>WARN</b> (dấu hiệu tấn công)</td></tr>
 *   <tr><td>UserNotFoundException</td><td>404</td><td>DEBUG</td></tr>
 *   <tr><td>SessionNotFoundException</td><td>404</td><td>DEBUG</td></tr>
 *   <tr><td>RoleNotFoundException</td><td>500</td><td>ERROR (lỗi cấu hình hệ thống)</td></tr>
 *   <tr><td>DuplicateUsername/EmailException</td><td>409</td><td>DEBUG</td></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>DEBUG (field errors)</td></tr>
 *   <tr><td>IllegalArgumentException</td><td>400</td><td>DEBUG</td></tr>
 *   <tr><td>DomainException khác (catch-all)</td><td>500</td><td>ERROR</td></tr>
 *   <tr><td>Exception (catch-all cuối)</td><td>500</td><td>ERROR</td></tr>
 * </table>
 *
 * <h2>Triết lý log level</h2>
 *
 * <p><b>DEBUG cho event "bình thường":</b> login sai password, token expired, validation
 * fail từ client - những thứ xảy ra hàng nghìn lần/ngày ở production. Log INFO/WARN
 * sẽ làm ngập log và che mất event thực sự quan trọng.
 *
 * <p><b>WARN cho dấu hiệu tấn công:</b> {@link TokenReuseDetectedException} là 1
 * trong số ít event đáng để log WARN - đây là dấu hiệu attacker đã có refresh token
 * và đang dùng. Worth keeping for forensic.
 *
 * <p><b>ERROR cho lỗi hệ thống:</b> {@link RoleNotFoundException} với role USER
 * mặc định bị thiếu = seed data sai. Đây là configuration issue cần fix ngay.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 401 - Authentication errors
    // ============================================================

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        log.debug("Invalid credentials: {}", ex.getMessage());
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
        // WARN: dấu hiệu tấn công - đáng để alert/forensic.
        // Path đi kèm để correlate với access log.
        log.warn("Token reuse detected on path {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex, request);
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
        // Route không tồn tại - response sạch hơn Spring default whitelabel page.
        log.debug("No handler for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse body = ErrorResponse.of("HTTP.NOT_FOUND",
                "Endpoint không tồn tại: " + ex.getHttpMethod() + " " + ex.getRequestURL(),
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
     * Validation fail (Bean Validation, {@code @Valid}). Trả tất cả field errors
     * trong 1 response - frontend hiển thị inline ngay dưới mỗi field.
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
     * Role mặc định USER bị xóa hoặc rename - lỗi cấu hình hệ thống, không phải
     * lỗi user. Log ERROR để alert.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(
            RoleNotFoundException ex, HttpServletRequest request) {
        log.error("Role not found - configuration issue: {}", ex.getMessage());
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    /**
     * Catch-all cho DomainException chưa explicit map ở trên. Defensive: nếu
     * tương lai thêm DomainException mới mà quên handle, ít nhất response vẫn
     * có errorCode (không bị fallback về whitelabel error).
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(
            DomainException ex, HttpServletRequest request) {
        log.error("Unhandled domain exception [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    /**
     * Catch-all cuối cùng. KHÔNG để stacktrace leak ra response - chỉ trả message
     * generic. Stacktrace ở server log (level ERROR) để debug.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse body = ErrorResponse.of(
                "INTERNAL.UNEXPECTED",
                "Đã có lỗi bất ngờ xảy ra. Liên hệ admin nếu vẫn lặp lại.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private ResponseEntity<ErrorResponse> build(HttpStatus status, DomainException ex,
                                                HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private ErrorResponse.FieldError toFieldError(ErrorResponse.FieldError fieldError) {
        String message = fieldError.getDefaultMessage();
        if (message == null) {
            message = "invalid";
        }
        return new ErrorResponse.FieldError(fieldError.getField(), message);
    }
}