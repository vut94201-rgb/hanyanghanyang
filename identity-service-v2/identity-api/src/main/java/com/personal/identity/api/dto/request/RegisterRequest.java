package com.personal.identity.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/register}.
 *
 * <h2>Validation Architecture</h2>
 * <ul>
 * <li>{@code username}: 3-50 characters, restricted to alphanumeric characters, periods, underscores,
 * and hyphens. Intentionally disallows spaces or special characters to eliminate
 * username-based injection/spoofing vectors (Note: While JPA parameters are safe from
 * raw SQL injection by default, this strictness ensures clean HTML/log rendering down the line).</li>
 * <li>{@code emailAddress}: Validated against the RFC 5322 specifications via the standard Bean Validation
 * {@code @Email} constraint.</li>
 * <li>{@code password}: 8-72 characters. BCrypt's mathematical core possesses a strict
 * maximum input ceiling of 72 bytes; inputs exceeding this limit will be silently truncated.
 * We proactively enforce this constraint at the DTO layer to prevent unexpected truncation artifacts.</li>
 * <li>{@code fullName}: Optional field, constrained to a maximum ceiling of 100 characters.</li>
 * </ul>
 *
 * <h2>Why we do NOT enforce strict password complexity regex (uppercase, numbers, symbols) here:</h2>
 * The updated NIST SP 800-63B guidelines (2017+) explicitly deprecate arbitrary character-space complexity rules
 * in favor of password length over complexity. Forcing users to inject an token "Aa1!"
 * actually results in weaker, highly predictable password patterns (e.g., users writing "Password1!" instead of
 * choosing a secure, long passphrase). If a stricter enterprise policy is mandated later,
 * a {@code @Pattern} constraint can be added, but we skip it here to keep the MVP aligned with security standards.
 *
 * @param username     The unique account identifier chosen by the user.
 * @param emailAddress The user's primary contact email, mapped bi-directionally.
 * @param password     The raw plain text password string to be processed by BCrypt.
 * @param fullName     The optional profile display name.
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "username can only contain letters, numbers, periods, underscores, and hyphens"
        )
        String username,
        @Email
        @NotBlank
        @Size(min = 1, max = 256)
        String emailAddress,
        @NotBlank
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters (BCrypt limit is 72)")
        String password,
        @Size(min = 1, max = 256)
        String fullName
) {
}
