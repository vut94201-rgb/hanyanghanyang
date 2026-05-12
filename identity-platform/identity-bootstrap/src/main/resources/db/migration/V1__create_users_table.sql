-- ============================================================================
-- V1: create users table
--
-- Bảng nghiệp vụ đầu tiên — tài khoản người dùng.
-- Inherits audit columns (version, created_by, updated_by, created_at,
-- updated_at, active, deleted) from BaseJpaAuditEntity.
-- ============================================================================

CREATE SEQUENCE seq_users START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE users (
                       id          NUMBER(19)    NOT NULL,
                       username    VARCHAR2(50)  NOT NULL,
                       email       VARCHAR2(255) NOT NULL,
                       password    VARCHAR2(255) NOT NULL,            -- BCrypt hash
                       full_name   VARCHAR2(100),
                       status      VARCHAR2(20)  NOT NULL,            -- PENDING | ACTIVE | LOCKED

    -- Audit columns (BaseJpaAuditEntity)
                       version     NUMBER(19),
                       created_by  NUMBER(19),
                       updated_by  NUMBER(19),
                       created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
                       active      NUMBER(1,0)   DEFAULT 1 NOT NULL,
                       deleted     NUMBER(1,0)   DEFAULT 0 NOT NULL,

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uk_users_username UNIQUE (username),
                       CONSTRAINT uk_users_email    UNIQUE (email),
                       CONSTRAINT ck_users_status   CHECK (status IN ('PENDING', 'ACTIVE', 'LOCKED')),
                       CONSTRAINT ck_users_active   CHECK (active  IN (0, 1)),
                       CONSTRAINT ck_users_deleted  CHECK (deleted IN (0, 1))
);

-- Filter index for soft-delete queries:
-- most queries look up active, non-deleted users.
CREATE INDEX idx_users_active ON users (active, deleted);

COMMENT ON TABLE  users             IS 'User accounts (auth context)';
COMMENT ON COLUMN users.status      IS 'PENDING (just registered), ACTIVE (verified), LOCKED (admin lock)';
COMMENT ON COLUMN users.password    IS 'BCrypt hash — never store plain text';
COMMENT ON COLUMN users.deleted     IS 'Soft delete flag; 0=alive, 1=deleted';