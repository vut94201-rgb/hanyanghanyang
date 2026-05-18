-- =============================================================================
-- V4: Direct permission grant - user_permissions (RBAC + ABAC hybrid)
-- =============================================================================
-- Mô hình quyền:
--   effective_permissions(user) = ∪(roles.permissions) ∪ direct_permissions(user)
--
-- Mục đích: linh hoạt hơn RBAC thuần. Khi 1 user cần thêm 1 quyền đặc biệt
-- mà không có role nào cover sẵn, gán trực tiếp qua bảng này thay vì tạo
-- role rác chỉ để gán cho 1 người.
--
-- Audit-friendly:
--   - granted_by:    user id của admin đã gán (nullable: nullable cho seed data)
--   - grant_reason:  lý do nghiệp vụ (vd "Bob cần export report trong tháng 11")
--   - granted_at:    thời điểm gán
-- =============================================================================

CREATE TABLE user_permissions (
    user_id          NUMBER(19)     NOT NULL,
    permission_id    NUMBER(19)     NOT NULL,
    granted_at       TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    granted_by       NUMBER(19),                                       -- nullable cho hệ thống/seed
    grant_reason     VARCHAR2(500),
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_user_perms_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_perms_perm FOREIGN KEY (permission_id)
        REFERENCES permissions (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_perms_granter FOREIGN KEY (granted_by)
        REFERENCES users (id) ON DELETE SET NULL
);

-- FK reverse-index (Oracle KHÔNG auto-index FK)
CREATE INDEX idx_user_permissions_perm ON user_permissions (permission_id);
CREATE INDEX idx_user_permissions_granter ON user_permissions (granted_by);

COMMENT ON TABLE user_permissions IS
    'Direct permission grant to user, additive to permissions from roles.';
COMMENT ON COLUMN user_permissions.granted_by IS
    'User id of the admin who granted this permission. NULL for system/seed grants.';
COMMENT ON COLUMN user_permissions.grant_reason IS
    'Business reason for the grant, for audit log.';
