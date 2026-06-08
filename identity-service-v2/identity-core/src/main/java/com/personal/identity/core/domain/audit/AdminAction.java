package com.personal.identity.core.domain.audit;

/**
 * Lists the types of admin actions being tracked. Stored as a String in the DB
 * (see migration V5) -> this enum defines the source of truth; renaming an enum value requires
 * modifying the migration script to backfill data if necessary.
 *
 * <p><b>Why not combine this with HTTP action verbs (GET/POST):</b> Actions here
 * are business-level, not HTTP-level. A POST request could correspond to DISABLE_USER
 * or LOCK_USER depending on the endpoint. Audit logs are read by the compliance/security team —
 * they do not care about the HTTP method.
 *
 * <p><b>Naming convention:</b> {@code VERB_OBJECT} - easy to read when grepping logs.
 */
public enum AdminAction {

    /** Admin lists users. NOT logged to avoid noise (read-only). */
    // LIST_USERS — intentionally not tracked

    /** Disable: changes status to DISABLED + revokes session. */
    DISABLE_USER,

    /** Lock: changes status to LOCKED (usually auto-triggered by the system; admins can trigger manually). */
    LOCK_USER,

    /** Activate: changes status from DISABLED/LOCKED to ACTIVE. */
    ACTIVATE_USER,

    /** Assigns/removes roles. Payload contains the diff (added, removed). */
    UPDATE_USER_ROLES,

    /** Admin views user details (only logged if the user is sensitive — placeholder). */
    // VIEW_USER_DETAIL — TODO when a "sensitive" user requires read access logging

    /** Views audit logs. NOT logged to avoid meta-noise. */
    // VIEW_AUDIT_LOG — intentionally not tracked
}