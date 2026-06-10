package com.personal.identity.api.security.filter;

import com.personal.identity.api.dto.AuthenticatedUser;
import com.personal.identity.core.application.port.out.SessionRepository;
import com.personal.identity.core.domain.session.Session;
import com.personal.identity.core.domain.token.AccessTokenBlacklist;
import com.personal.identity.core.domain.token.TokenClaims;
import com.personal.identity.core.domain.token.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Spring Security filter responsible for verifying the JWT on every authenticated request.
 *
 * <h2>Execution Flow</h2>
 * <ol>
 * <li>Extract the bearer token from the {@code Authorization} header.
 * If absent ⟿ leave the Authentication context as NULL and proceed with the filter chain.
 * The {@link SecurityConfig} will subsequently reject it with a 401 Unauthorized if the endpoint demands authentication, or permit it if the endpoint is public.</li>
 * <li>Verify the JWT via the {@link TokenProvider}. If verification fails ⟿ bypass (treated identically to an absent token).</li>
 * <li>Check if the JTI exists in the Redis blacklist. If present ⟿ bypass (indicates a logged-out state).</li>
 * <li>Load the session and verify its ACTIVE status. If not ACTIVE ⟿ bypass.</li>
 * <li>Construct the {@link AuthenticatedUser} principal along with its granted authorities (ROLE_xxx + mapped permissions),
 * and inject it into the {@code SecurityContext}.</li>
 * <li>Invoke {@code chain.doFilter()} to propagate the request downstream.</li>
 * </ol>
 *
 * <h2>The "Silent Fail" Philosophy</h2>
 *
 * <p>This filter DOES NOT throw exceptions, nor does it explicitly set HTTP response statuses.
 * When any validation check fails, it intentionally leaves the Authentication as NULL and delegates back to the filter chain. Spring Security will inherently handle this by:
 * <ul>
 * <li>For public endpoints ({@code permitAll}): Permitting access (acting as a standard anonymous request).</li>
 * <li>For authenticated endpoints: Throwing an {@code AccessDeniedException} ⟿ which is intercepted by the
 * {@code ExceptionTranslationFilter} to return a strictly standardized 401/403 response.</li>
 * </ul>
 *
 * <p>This architectural approach is significantly cleaner than manually mutating the response within the filter —
 * completely avoiding duplicate logic with the built-in {@code ExceptionTranslationFilter}.
 *
 * <h2>Authority Formatting</h2>
 *
 * <p>{@code roleCodes} ⟿ prefixed with {@code "ROLE_"} (adhering to the Spring convention for {@code hasRole(...)} evaluations).
 * {@code permissionCodes} ⟿ preserved as-is (for {@code hasAuthority("user:read")} evaluations).
 *
 * <p>Example: If a user possesses the {@code ADMIN} role and the {@code user:read} permission ⟿ the generated authorities
 * will be: {@code [ROLE_ADMIN, user:read]}. Thus, both {@code @PreAuthorize("hasRole('ADMIN')")} and
 * {@code hasAuthority('user:read')} will function seamlessly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter  extends OncePerRequestFilter {
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

        // 1. Extract the bearer token
        String token = extractBearerToken(request);
        if (token == null) {
            // Token absent - process as an anonymous request; propagate the chain.
            chain.doFilter(request, response);
            return;
        }

        // 2. Verify the JWT
        Optional<TokenClaims> claimsOpt = tokenProvider.parseAndVerify(token);
        if (claimsOpt.isEmpty()) {
            // Token invalid (bad signature, expired, malformed). DO NOT set
            // Authentication - proceed as an anonymous request.
            log.debug("JWT verify failed for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        TokenClaims claims = claimsOpt.get();

        // 3. Check the access token blacklist (verifying logout state)
        if (claims.tokenId() != null && accessTokenBlacklist.contains(claims.tokenId())) {
            log.debug("JWT in blacklist (logged out) for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 4. Verify the underlying session remains ACTIVE
        Optional<Session> sessionOpt = sessionRepository.findById(claims.sessionId());
        if (sessionOpt.isEmpty() || !sessionOpt.get().isActive()) {
            // The session has been revoked (via logout, password change, or admin action) or has expired.
            // The access token retains a valid cryptographic signature, but the underlying session is dead ⟿ reject.
            // This enforces the "real-time revocation" mechanism - although the access token has a short TTL (15m),
            // we can instantly revoke access centrally via the DB.
            log.debug("Session not active for path {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        // 5. Construct the Authentication principal and inject it into the SecurityContext
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
     * Extracts the token from the {@code Authorization: Bearer xxx} header.
     * Returns {@code null} if the header is missing or improperly formatted.
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
     * Constructs the granted authorities for Spring Security.
     * Roles are prefixed with {@code ROLE_}, while discrete permissions remain unchanged.
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
