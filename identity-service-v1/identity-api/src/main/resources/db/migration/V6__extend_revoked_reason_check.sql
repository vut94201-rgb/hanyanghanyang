-- =============================================================================
-- V6: mở rộng CK_SESSIONS_REVOKED_REASON để chấp nhận ADMIN_REVOKED
-- =============================================================================
-- Bối cảnh:
--   V2 tạo constraint cho phép {LOGOUT, TOKEN_REUSE, USER_ACTION, EXPIRED}.
--   Step L3 thêm enum value ADMIN_REVOKED (cho action admin disable/lock user)
--   nhưng QUÊN update constraint → INSERT fail với ORA-02290.
--
-- Quy ước:
--   Oracle không cho ALTER CONSTRAINT trực tiếp - phải DROP + ADD lại.
--   Đây là DDL non-destructive (data hiện có vẫn match).
-- =============================================================================

ALTER TABLE sessions DROP CONSTRAINT ck_sessions_revoked_reason;

ALTER TABLE sessions ADD CONSTRAINT ck_sessions_revoked_reason CHECK (
    revoked_reason IS NULL
    OR revoked_reason IN ('LOGOUT','TOKEN_REUSE','USER_ACTION','EXPIRED','ADMIN_REVOKED')
);

-- Update column comment để doc khớp.
COMMENT ON COLUMN sessions.revoked_reason IS
    'LOGOUT | TOKEN_REUSE | USER_ACTION | EXPIRED | ADMIN_REVOKED';
