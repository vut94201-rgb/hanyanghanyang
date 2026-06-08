package com.personal.identity.core.domain.session;

import com.personal.identity.core.domain.shared.exception.DomainException;
import com.personal.identity.core.domain.shared.exception.ErrorCode;

/**
 * Thrown when a user attempts to revoke or access a session that does NOT belong to them.
 *
 * <p><b>Scenario:</b> {@code DELETE /api/v1/auth/sessions/{id}} - user A is logged in,
 * but sends the sessionId belonging to user B -> the server must reject the request. It should not return a 404
 * (as a 404 would leak information, differentiating "this session ID exists but does not belong to you"
 * from "this session ID does not exist").
 *
 * <p><b>Why separate this from {@link SessionNotFoundException}:</b>
 * <ul>
 * <li>Different semantics: "not found" vs "access denied".</li>
 * <li>Different HTTP status codes: 404 vs 403.</li>
 * <li>Different log levels can be applied: SessionNotFound = DEBUG, AccessDenied = WARN
 * (an indicator of a potential IDOR attempt - Insecure Direct Object Reference).</li>
 * </ul>
 *
 * <p><b>Security Note:</b> Some systems choose to return a 404 for both cases to completely
 * avoid leaking whether "this session ID exists". That is a valid design choice. Here, we use 403 because
 * the sessionId is a UUID v4 (122-bit entropy) - making brute-force attacks unfeasible.
 * Therefore, leaking that a session "exists but does not belong to you" is not a significant attack vector.
 */
public class SessionAccessDeniedException extends DomainException {
    private static final ErrorCode ERROR_CODE = ErrorCode.SESSION_ACCESS_DENIED;

    public SessionAccessDeniedException() {
        super(ERROR_CODE);
    }

    public SessionAccessDeniedException(String message) {
        super(ERROR_CODE, message);
    }
}
