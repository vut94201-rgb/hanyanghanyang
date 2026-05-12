package com.personal.auth.application.dto;

/**
 * Input command for the Register User use case.
 *
 * <p>An <b>application-layer DTO</b>, not a domain object. It carries data
 * from the API boundary into {@code RegisterUserUseCase} in a transport-
 * agnostic shape — the controller maps {@code RegisterRequest} (HTTP DTO)
 * into this command before invoking the use case.
 *
 * <p>Why a record (not a class with Bean Validation annotations):
 * <ul>
 *   <li>Validation belongs at the API boundary ({@code RegisterRequest}),
 *       not the application layer. By the time we're here, inputs are
 *       already format-valid.</li>
 *   <li>The use case still asserts non-blank at the start (defence in
 *       depth) — see {@code RegisterUserUseCase}.</li>
 *   <li>Immutable + value-equality come for free with {@code record}.</li>
 * </ul>
 *
 * <p>{@code password} is the <b>plain text</b> password as received from the
 * client. Hashing happens inside the use case via {@code PasswordHasher}
 * — the domain never sees plain text.
 */
public record RegisterUserCommand(String username,
                                  String email,
                                  String password,
                                  String fullName) {
}
