package com.personal.identity.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/refresh}.
 *
 * <p><b>Raw refresh token mechanics:</b> Generated as a Base64 URL-safe string derived from
 * a 32-byte secure random generator ⟿ producing a clean 43-character string.
 * we deliberately set the maximum validation ceiling to 256 characters to provide a comfortable
 * architectural buffer in case the downstream token format evolves in the future.
 *
 * @param refreshToken The raw token string submitted by the client to rotate their session.
 */
public record RefreshTokenRequest(
        @NotBlank
        @Size(min = 1, max = 256)
        String refreshToken
) {
}
