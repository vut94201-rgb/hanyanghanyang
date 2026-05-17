-- =============================================================================
-- V1: Tạo bảng users, roles, permissions + 2 bảng nối + sequence
-- =============================================================================
-- Lưu ý Oracle:
--   * Identifier KHÔNG quote thì tự uppercase. Ta KHÔNG quote để tự do dùng,
--     nhưng tên column trong Java entity sẽ chỉ định bằng @Column(name = "...").
--   * Boolean dùng NUMBER(1) + CHECK constraint (Oracle không có BOOLEAN trước 23c).
--   * Timestamp dùng TIMESTAMP (không có TIMESTAMPTZ chuẩn).
--   * Sequence INCREMENT BY 50 để khớp với Hibernate allocationSize=50 (batch insert).
-- =============================================================================

-- ----- USERS ----------------------------------------------------------------

CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

CREATE TABLE users (
    id                  NUMBER(19)      NOT NULL,
    username            VARCHAR2(64)    NOT NULL,
    email_address       VARCHAR2(255)   NOT NULL,
    password_hash       VARCHAR2(60)    NOT NULL,                           -- BCrypt hash luôn 60 ký tự
    full_name           VARCHAR2(150),
    account_status      VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,          -- ACTIVE / DISABLED / LOCKED
    is_deleted          NUMBER(1)       DEFAULT 0 NOT NULL,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    version             NUMBER(19)      DEFAULT 0 NOT NULL,                  -- optimistic locking
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email_address),
    CONSTRAINT ck_users_status CHECK (account_status IN ('ACTIVE','DISABLED','LOCKED')),
    CONSTRAINT ck_users_deleted CHECK (is_deleted IN (0,1))
);

-- Index hỗ trợ query phổ biến
CREATE INDEX idx_users_status ON users (account_status);
CREATE INDEX idx_users_deleted ON users (is_deleted);

COMMENT ON TABLE users IS 'Tài khoản người dùng. Soft-delete bằng cờ is_deleted.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, độ dài cố định 60 ký tự.';
COMMENT ON COLUMN users.account_status IS 'ACTIVE | DISABLED | LOCKED';


-- ----- ROLES ----------------------------------------------------------------

CREATE SEQUENCE roles_seq START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

CREATE TABLE roles (
    id                  NUMBER(19)      NOT NULL,
    role_code           VARCHAR2(50)    NOT NULL,                            -- ADMIN / USER / MODERATOR
    role_name           VARCHAR2(100)   NOT NULL,                            -- display name
    description         VARCHAR2(500),
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    version             NUMBER(19)      DEFAULT 0 NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (role_code)
);

COMMENT ON TABLE roles IS 'Vai trò (RBAC). Không soft-delete; xóa role sẽ cascade unassign khỏi user.';
COMMENT ON COLUMN roles.role_code IS 'Mã định danh dùng trong code (ADMIN, USER, MODERATOR...).';


-- ----- PERMISSIONS ----------------------------------------------------------

CREATE SEQUENCE permissions_seq START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

CREATE TABLE permissions (
    id                  NUMBER(19)      NOT NULL,
    permission_code     VARCHAR2(100)   NOT NULL,                            -- vd: user:read, user:write
    description         VARCHAR2(500),
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    version             NUMBER(19)      DEFAULT 0 NOT NULL,
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (permission_code)
);

COMMENT ON TABLE permissions IS 'Quyền chi tiết (RBAC). Format đề xuất: resource:action.';


-- ----- USER_ROLES (many-to-many) --------------------------------------------

CREATE TABLE user_roles (
    user_id             NUMBER(19)      NOT NULL,
    role_id             NUMBER(19)      NOT NULL,
    assigned_at         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- FK index (Oracle KHÔNG tự index cột FK) - cần thiết để DELETE/UPDATE bảng cha không lock toàn bảng
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

COMMENT ON TABLE user_roles IS 'Bảng nối user <-> role.';


-- ----- ROLE_PERMISSIONS (many-to-many) --------------------------------------

CREATE TABLE role_permissions (
    role_id             NUMBER(19)      NOT NULL,
    permission_id       NUMBER(19)      NOT NULL,
    assigned_at         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_perm ON role_permissions (permission_id);

COMMENT ON TABLE role_permissions IS 'Bảng nối role <-> permission.';
