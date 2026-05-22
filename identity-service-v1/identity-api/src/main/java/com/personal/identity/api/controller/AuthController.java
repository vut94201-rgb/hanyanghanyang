package com.personal.identity.api.controller;


import com.personal.identity.api.dto.AuthResponse;
import com.personal.identity.api.dto.ChangePasswordRequest;
import com.personal.identity.api.dto.LoginRequest;
import com.personal.identity.api.dto.LogoutAllResponse;
import com.personal.identity.api.dto.RefreshTokenRequest;
import com.personal.identity.api.dto.RegisterRequest;
import com.personal.identity.api.dto.SessionResponse;
import com.personal.identity.api.dto.UserResponse;
import com.personal.identity.api.security.AuthenticatedUser;
import com.personal.identity.api.util.RequestContextExtractor;
import com.personal.identity.core.service.AuthResult;
import com.personal.identity.core.service.ChangePasswordUseCase;
import com.personal.identity.core.service.LoginUseCase;
import com.personal.identity.core.service.LogoutAllUseCase;
import com.personal.identity.core.service.LogoutUseCase;
import com.personal.identity.core.service.RefreshTokenUseCase;
import com.personal.identity.core.service.RegisterUseCase;
import com.personal.identity.core.service.RevokeSessionUseCase;
import com.personal.identity.core.session.RequestContext;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.user.User;
import com.personal.identity.core.user.UserNotFoundException;
import com.personal.identity.core.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
 *   <li><b>GET /me và GET /sessions không qua use case:</b> đây là pure query, không có
 *       business logic. Controller gọi trực tiếp Repository - chấp nhận exception cho
 *       Hexagonal vì không có domain rule nào ở đây. Tránh tạo "AnemicUseCase" wrapper.</li>
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
    private final RevokeSessionUseCase revokeSessionUseCase;
    private final LogoutAllUseCase logoutAllUseCase;
    private final RequestContextExtractor contextExtractor;
    private final com.personal.identity.api.observability.IdentityMetrics.LoginMetrics loginMetrics;
    // Inject port trực tiếp cho read-only query — không tạo use case wrapper rỗng.
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

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
        // Timer.record() đo cả wall-clock time của login flow gồm DB lookup + BCrypt verify.
        // BCrypt cost factor 10 mất ~50-100ms - đáng đo để alert nếu latency tăng bất thường.
        // Outcome counter increment trong try/catch (success path) hoặc GlobalExceptionHandler
        // (failure path qua exception). Đơn giản hoá: chỉ count success ở đây, failure để
        // GlobalExceptionHandler tự pump (xem patch 2).
        io.micrometer.core.instrument.Timer.Sample sample =
                io.micrometer.core.instrument.Timer.start();
        try {
            AuthResult result = loginUseCase.execute(new LoginUseCase.LoginCommand(
                    request.username(),
                    request.password(),
                    context
            ));
            loginMetrics.loginSuccess().increment();
            return ResponseEntity.ok(AuthResponse.from(result));
        } finally {
            sample.stop(loginMetrics.loginTimer());
        }
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

    // ============================================================
    // STEP G - Auxiliary endpoints
    // ============================================================

    /**
     * Lấy thông tin user hiện tại.
     * <p><b>Vì sao gọi {@code userRepository.findById} thay vì dùng info trong JWT?</b>
     * JWT chỉ chứa userId, roles, perms (đã snapshot lúc login). Email/fullName/createdAt
     * không có trong JWT (cố ý giữ payload nhỏ). Hơn nữa user có thể đổi profile sau login,
     * nên fetch từ DB là correctness > performance ở đây.
     * <p><b>Vì sao KHÔNG cache:</b> endpoint này thường được client gọi 1 lần sau login để
     * fill UI - không phải hot path. Nếu cần optimize sau, thêm Redis cache với TTL ngắn.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        User found = userRepository.findById(user.userId())
                .orElseThrow(() -> UserNotFoundException.byId(user.userId()));
        return ResponseEntity.ok(UserResponse.from(found));
    }

    /**
     * List session ACTIVE của user hiện tại - dùng để show trang "Devices signed in".
     * <p><b>Field {@code current}</b> trong response giúp UI hiển thị "This device"
     * và disable nút revoke trên dòng đó (UX best practice: tránh user vô tình kick
     * chính mình; nếu muốn vẫn dùng /logout-all).
     * <p>Chỉ trả ACTIVE - REVOKED/EXPIRED không có giá trị cho user thường (admin
     * có endpoint riêng để xem lịch sử).
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<SessionResponse> sessions = sessionRepository.findActiveByUserId(user.userId())
                .stream()
                .map(s -> SessionResponse.from(s, user.sessionId()))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Revoke 1 session cụ thể của user hiện tại.
     * <p><b>Ownership check ở use case:</b> nếu sessionId thuộc user khác →
     * {@code SessionAccessDeniedException} (403). Đây là chống IDOR vulnerability.
     * <p><b>Allow revoke chính session hiện tại?</b> Có. User có thể truyền sessionId
     * của chính họ - sẽ chạy bình thường. Sau khi revoke, request tiếp theo sẽ fail 403
     * vì session đã REVOKED (filter check). Tuy nhiên request DELETE này vẫn trả 204
     * thành công (đã trong transaction trước khi session bị revoke ghi xuống DB).
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String sessionId) {
        revokeSessionUseCase.execute(sessionId, user.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Logout khỏi TẤT CẢ device (kể cả device hiện tại).
     * <p><b>Khác với /change-password:</b>
     * <ul>
     *   <li>change-password: revoke session KHÁC, GIỮ session hiện tại.</li>
     *   <li>logout-all: revoke TẤT CẢ, kể cả session hiện tại - buộc user login lại
     *       trên mọi thiết bị.</li>
     * </ul>
     * <p><b>Tình huống dùng:</b> user nghi tài khoản bị compromise, muốn kick mọi
     * device kể cả chính mình để đảm bảo an toàn (thường kèm theo đổi password).
     * <p>Trả 200 + count thay vì 204 - để client hiện toast "Đã đăng xuất khỏi N thiết bị".
     */
    @PostMapping("/logout-all")
    public ResponseEntity<LogoutAllResponse> logoutAll(
            @AuthenticationPrincipal AuthenticatedUser user) {
        int count = logoutAllUseCase.execute(user.userId());
        return ResponseEntity.ok(new LogoutAllResponse(count));
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
