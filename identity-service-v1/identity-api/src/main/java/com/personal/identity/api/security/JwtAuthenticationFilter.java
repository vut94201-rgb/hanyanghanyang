package com.personal.identity.api.security;

import com.personal.identity.core.session.Session;
import com.personal.identity.core.session.SessionRepository;
import com.personal.identity.core.token.AccessTokenBlacklist;
import com.personal.identity.core.token.TokenClaims;
import com.personal.identity.core.token.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Filter Spring Security verify JWT cho mọi authenticated request.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Extract bearer token từ header {@code Authorization}.
 *       Nếu không có → để Authentication NULL, chain tiếp tục - {@link SecurityConfig}
 *       sẽ reject 401 nếu endpoint yêu cầu auth, hoặc cho qua nếu public.</li>
 *   <li>Verify JWT qua {@link TokenProvider}. Nếu fail → bỏ qua (như case không có token).</li>
 *   <li>Check JTI có trong blacklist Redis không. Nếu có → bỏ qua (logged out).</li>
 *   <li>Load session, check ACTIVE. Nếu không ACTIVE → bỏ qua.</li>
 *   <li>Build {@link AuthenticatedUser} + authorities (ROLE_xxx + permissions),
 *       set vào {@code SecurityContext}.</li>
 *   <li>chain.doFilter() để request đi tiếp.</li>
 * </ol>
 *
 * <h2>Triết lý "silent fail"</h2>
 *
 * <p>Filter này KHÔNG throw, KHÔNG set response status. Khi mọi check fail, để
 * Authentication NULL và để chain đi tiếp. Spring Security sẽ:
 * <ul>
 *   <li>Endpoint public ({@code permitAll}): cho qua (anonymous request).</li>
 *   <li>Endpoint authenticated: throw {@code AccessDeniedException} →
 *       {@code ExceptionTranslationFilter} bắt và trả 401/403.</li>
 * </ul>
 *
 * <p>Cách này gọn hơn filter tự xử lý response - không trùng logic với
 * {@code ExceptionTranslationFilter}.
 *
 * <h2>Authority format</h2>
 *
 * <p>{@code roleCodes} → prefix {@code "ROLE_"} (Spring convention cho
 * {@code hasRole(...)}). {@code permissionCodes} giữ nguyên (cho
 * {@code hasAuthority("user:read")}).
 *
 * <p>Ví dụ: user có role {@code ADMIN}, permission {@code user:read} → authorities:
 * {@code [ROLE_ADMIN, user:read]}. {@code @PreAuthorize("hasRole('ADMIN')")} hoặc
 * {@code hasAuthority('user:read')} đều hoạt động.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        // 1. Extract bearer token
        String token = extractBearerToken(request);
        if (token == null) {
            // Không có token - request anonymous, chain tiếp tục.
            chain.doFilter(request, response);
            return;
        }

        // 2. Verify JWT
        Optional<TokenClaims> claimsOpt = tokenProvider.parseAndVerify(token);
        if (claimsOpt.isEmpty()) {
            // Token invalid (sai signature, expired, malformed). KHÔNG set
            // Authentication - tiếp tục như request anonymous.
            log.debug("JWT verify failed for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        TokenClaims claims = claimsOpt.get();

        // 3. Check blacklist (logged out)
        if (claims.tokenId() != null && accessTokenBlacklist.contains(claims.tokenId())) {
            log.debug("JWT in blacklist (logged out) for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 4. Check session ACTIVE
        Optional<Session> sessionOpt = sessionRepository.findById(claims.sessionId());
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive()) {
            // Session đã revoke (logout/change-password/admin revoke) hoặc expire.
            // Access token còn valid signature nhưng session không còn → reject.
            // Đây là cơ chế "real-time logout" - access token TTL ngắn (15p), nhưng
            // có thể revoke ngay lập tức qua DB.
            log.debug("Session not active for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 5. Build Authentication + set vào context
        AuthenticatedUser principal = new AuthenticatedUser(
                claims.userId(), claims.sessionId(), claims.tokenId());

        List<SimpleGrantedAuthority> authorities = buildAuthorities(claims);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }

    /**
     * Extract token từ header {@code Authorization: Bearer xxx}. Trả {@code null}
     * nếu không có header hoặc format sai.
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /**
     * Build authorities cho Spring Security. Roles thêm prefix {@code ROLE_},
     * permissions giữ nguyên.
     */
    private List<SimpleGrantedAuthority> buildAuthorities(TokenClaims claims) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        if (claims.roleCodes() != null) {
            for (String role : claims.roleCodes()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        if (claims.permissionCodes() != null) {
            for (String permission : claims.permissionCodes()) {
                authorities.add(new SimpleGrantedAuthority(permission));
            }
        }

        return authorities;
    }
}