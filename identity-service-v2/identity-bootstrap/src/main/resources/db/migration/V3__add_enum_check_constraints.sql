-- =============================================================================
-- V3__add_enum_check_constraints.sql
-- Migrates existing data from full enum names to short codes (CodeEnum)
-- and adds CHECK constraints for all enum columns.
-- =============================================================================

-- ======================= sessions.session_status =======================
UPDATE sessions SET session_status = 'A'  WHERE session_status = 'ACTIVE';
UPDATE sessions SET session_status = 'R'  WHERE session_status = 'REVOKED';
UPDATE sessions SET session_status = 'E'  WHERE session_status = 'EXPIRED';

ALTER TABLE sessions ADD CONSTRAINT chk_session_status
    CHECK (session_status IN ('A', 'R', 'E'));

-- ======================= sessions.device_type ==========================
UPDATE sessions SET device_type = 'D' WHERE device_type = 'DESKTOP';
UPDATE sessions SET device_type = 'M' WHERE device_type = 'MOBILE';
UPDATE sessions SET device_type = 'T' WHERE device_type = 'TABLET';
UPDATE sessions SET device_type = 'U' WHERE device_type = 'UNKNOWN';

ALTER TABLE sessions ADD CONSTRAINT chk_device_type
    CHECK (device_type IN ('D', 'M', 'T', 'U'));

-- ======================= sessions.revoked_reason =======================
UPDATE sessions SET revoked_reason = 'AR' WHERE revoked_reason = 'ADMIN_REVOKED';
UPDATE sessions SET revoked_reason = 'L'  WHERE revoked_reason = 'LOGOUT';
UPDATE sessions SET revoked_reason = 'UA' WHERE revoked_reason = 'USER_ACTION';
UPDATE sessions SET revoked_reason = 'E'  WHERE revoked_reason = 'EXPIRED';
UPDATE sessions SET revoked_reason = 'TR' WHERE revoked_reason = 'TOKEN_REUSE';

ALTER TABLE sessions ADD CONSTRAINT chk_revoked_reason
    CHECK (revoked_reason IN ('AR', 'L', 'UA', 'E', 'TR'));

-- ======================= refresh_tokens.token_status ===================
UPDATE refresh_tokens SET token_status = 'A' WHERE token_status = 'ACTIVE';
UPDATE refresh_tokens SET token_status = 'R' WHERE token_status = 'REVOKED';
UPDATE refresh_tokens SET token_status = 'U' WHERE token_status = 'USED';

ALTER TABLE refresh_tokens ADD CONSTRAINT chk_token_status
    CHECK (token_status IN ('A', 'R', 'U'));

-- ======================= admin_audit_log.action_type ===================
UPDATE admin_audit_log SET action_type = 'DU'  WHERE action_type = 'DISABLE_USER';
UPDATE admin_audit_log SET action_type = 'LU'  WHERE action_type = 'LOCK_USER';
UPDATE admin_audit_log SET action_type = 'AU'  WHERE action_type = 'ACTIVATE_USER';
UPDATE admin_audit_log SET action_type = 'UUR' WHERE action_type = 'UPDATE_USER_ROLES';

ALTER TABLE admin_audit_log ADD CONSTRAINT chk_action_type
    CHECK (action_type IN ('DU', 'LU', 'AU', 'UUR'));

-- ======================= admin_audit_log.outcome =======================
UPDATE admin_audit_log SET outcome = 'S' WHERE outcome = 'SUCCESS';
UPDATE admin_audit_log SET outcome = 'F' WHERE outcome = 'FAILURE';

ALTER TABLE admin_audit_log ADD CONSTRAINT chk_outcome
    CHECK (outcome IN ('S', 'F'));
