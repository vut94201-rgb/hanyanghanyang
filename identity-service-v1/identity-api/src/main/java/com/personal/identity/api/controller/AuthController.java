package com.personal.identity.api.controller;


import com.personal.identity.api.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints. Endpoint prefix: {@code /api/v1/auth}.
 *
 * <p><b>Versioning:</b> Đặt {@code /v1} trong URL ngay từ đầu để khi breaking
 * change ở v2 sau này, frontend cũ vẫn dùng /v1 song song. Cost = thấp, benefit
 * = lớn.
 *
 * <p><b>Endpoint list:</b>
 * <ul>
 *   <li>{@code POST /register} - public, tạo user mới</li>
 *   <li>{@code POST /login} - public, trả access + refresh token</li>
 *   <li>{@code POST /refresh} - public, rotate refresh token</li>
 *   <li>{@code POST /logout} - authenticated, revoke session</li>
 *   <li>{@code POST /change-password} - authenticated, đổi password</li>
 * </ul>
 *
 * <p><b>Phân biệt public vs authenticated:</b> Sẽ cấu hình ở {@code SecurityConfig}
 * bước F5. Register/login/refresh là PUBLIC (chưa có token). Logout/change-password
 * yêu cầu đã authenticated (filter trích userId, sessionId, tokenId từ JWT).
 *
 * <p><b>Skeleton hiện tại:</b> Body trả 501 NOT_IMPLEMENTED - sẽ wire use case
 * ở bước F6 sau khi đã có UseCaseConfig + RequestContextExtractor + filter.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    // Use case sẽ inject ở F6:
    // private final RegisterUseCase registerUseCase;
    // private final LoginUseCase loginUseCase;
    // private final RefreshTokenUseCase refreshTokenUseCase;
    // private final LogoutUseCase logoutUseCase;
    // private final ChangePasswordUseCase changePasswordUseCase;
    // private final RequestContextExtractor contextExtractor;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        // F6: User user = registerUseCase.execute(new RegisterCommand(...));
        //     return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        // F6: AuthResult result = loginUseCase.execute(new LoginCommand(...));
        //     return ResponseEntity.ok(AuthResponse.from(result));
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        // F6: AuthResult result = refreshTokenUseCase.execute(new RefreshCommand(...));
        //     return ResponseEntity.ok(AuthResponse.from(result));
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // F6: String token = extractBearerToken(authHeader);
        //     logoutUseCase.execute(token);
        //     return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
            // F6: thêm @AuthenticationPrincipal hoặc custom resolver để lấy userId+sessionId
    ) {
        // F6: changePasswordUseCase.execute(new ChangePasswordCommand(...));
        //     return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}