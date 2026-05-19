package com.personal.identity.api.controller;


import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.ChangePasswordRequest;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.RefreshTokenRequest;
import com.personal.identity.api.dto.RegisterRequest;
import com.personal.identity.api.dto.UserResponse;
import com.personal.identity.api.security.AuthenticatedUser;
import com.personal.identity.api.util.RequestContextExtractor;
import com.personal.identity.core.service.AuthResult;
import com.personal.identity.core.service.ChangePasswordUseCase;
import com.personal.identity.core.service.LoginUseCase;
import com.personal.identity.core.service.LogoutUseCase;
import com.personal.identity.core.service.RefreshTokenUseCase;
import com.personal.identity.core.service.RegisterUseCase;
import com.personal.identity.core.session.RequestContext;
import com.personal.identity.core.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController — entrypoint HTTP cho luồng authentication.
 *
 * <h3>Triết lý thiết kế (cho phỏng vấn)</h3>
 * <ul>
 *   <li><b>Controller mỏng (thin controller):</b> chỉ chịu trách nhiệm parse HTTP →
 *       gọi use case → map result sang DTO. Không chứa business logic. Logic nằm trong
 *       {@code identity-core} (framework-free, dễ unit test).</li>
 *   <li><b>Dependency injection qua constructor</b> (Lombok {@code @RequiredArgsConstructor}):
 *       immutable field, không cần field injection, dễ test bằng new constructor call.</li>
 *   <li><b>Không catch exception ở đây:</b> mọi domain exception ném ra sẽ được
 *       {@code GlobalExceptionHandler} map sang HTTP status + ErrorResponse. Controller
 *       không biết về cấu trúc lỗi.</li>
 *   <li><b>RequestContext build ở controller layer:</b> vì chỉ Spring biết về
 *       {@code HttpServletRequest}. Core layer không được phụ thuộc servlet API.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final RequestContextExtractor contextExtractor;

    /**
     * Đăng ký user mới.
     * <p>Default role = USER (gán trong RegisterUseCase). Trả 201 CREATED kèm thông tin user
     * (KHÔNG kèm token — buộc user phải login để lấy token, audit trail rõ ràng hơn).
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        // DTO field 'password' → Command field 'rawPassword' (map theo vị trí positional record)
        User user = registerUseCase.execute(new RegisterUseCase.RegisterCommand(
                request.username(),
                request.emailAddress(),
                request.password(),
                request.fullName()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    /**
     * Login bằng username + password.
     * <p>RequestContext (IP, UA, GeoIP) build từ HttpServletRequest và truyền vào use case
     * để lưu session metadata. Không log username/password ở đây — đã log ở use case nếu cần.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = contextExtractor.extract(httpRequest);
        AuthResult result = loginUseCase.execute(new LoginUseCase.LoginCommand(
                request.username(),
                request.password(),
                context
        ));
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    /**
     * Refresh access token bằng refresh token.
     * <p>Reuse detection: nếu token đã USED → revoke cả family + session (xem RefreshTokenUseCase).
     * Cũng cần RequestContext để track session activity (cập nhật last-seen IP/UA nếu cần).
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        RequestContext context = contextExtractor.extract(httpRequest);
        AuthResult result = refreshTokenUseCase.execute(new RefreshTokenUseCase.RefreshCommand(
                request.refreshToken(),
                context
        ));
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    /**
     * Logout — blacklist access token + revoke session hiện tại.
     * <p><b>Vì sao extract token từ header thay vì dùng JTI từ {@code @AuthenticationPrincipal}?</b>
     * {@code AuthenticatedUser} có chứa {@code tokenId} (JTI) nhưng KHÔNG chứa {@code expiresAt}.
     * Để blacklist JTI vào Redis với TTL = thời gian còn lại, ta cần parse JWT lấy {@code exp}.
     * LogoutUseCase tự parse JWT từ raw token — tránh bloat principal với mọi field JWT.
     * <p>Strip prefix "Bearer " — header chuẩn format: {@code Authorization: Bearer xxx}.
     * Nếu thiếu prefix → header invalid → ném {@code IllegalArgumentException} (mapped 400).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = stripBearer(authHeader);
        logoutUseCase.execute(token);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đổi password — yêu cầu authenticated, cần currentPassword để xác thực.
     * <p><b>Vì sao cần currentPassword dù đã login?</b> Defense in depth. Nếu attacker chiếm
     * được access token (XSS, token leak), không thể đổi password mà không biết password cũ.
     * Đây là pattern chuẩn của Google, GitHub, AWS.
     * <p>Use case sẽ revoke tất cả session KHÁC (giữ session hiện tại) — cắt access của
     * attacker trên device khác mà không buộc user phải login lại trên thiết bị hiện tại.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordUseCase.execute(new ChangePasswordUseCase.ChangePasswordCommand(
                user.userId(),
                user.sessionId(),
                request.currentPassword(),
                request.newPassword()
        ));
        return ResponseEntity.noContent().build();
    }

    /**
     * Strip "Bearer " prefix khỏi Authorization header.
     * <p>Trả raw token. Nếu format sai → throw IllegalArgumentException → GlobalExceptionHandler
     * map sang 400. Không tự ý trả 401 ở đây vì controller không nên biết về HTTP status code
     * cho domain error.
     */
    private static String stripBearer(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Bearer token is empty");
        }
        return token;
    }
}