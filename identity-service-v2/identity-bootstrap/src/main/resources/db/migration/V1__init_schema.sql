-- =============================================================================
-- V1__init_schema.sql
-- Initializes the database schema for identity-service-v2 (Oracle)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Sequences
-- -----------------------------------------------------------------------------
CREATE SEQUENCE roles_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE permissions_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE admin_audit_log_seq START WITH 1 INCREMENT BY 50;

-- -----------------------------------------------------------------------------
-- RBAC Tables (Roles & Permissions)
-- -----------------------------------------------------------------------------
CREATE TABLE roles (
    id NUMBER(19, 0) NOT NULL PRIMARY KEY,
    role_code VARCHAR2(50) NOT NULL UNIQUE,
    role_name VARCHAR2(100) NOT NULL,
    description VARCHAR2(500),
    version NUMBER(19, 0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE permissions (
    id NUMBER(19, 0) NOT NULL PRIMARY KEY,
    permission_code VARCHAR2(100) NOT NULL UNIQUE,
    description VARCHAR2(500),
    version NUMBER(19, 0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE role_permissions (
    role_id NUMBER(19, 0) NOT NULL,
    permission_id NUMBER(19, 0) NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- -----------------------------------------------------------------------------
-- Users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id NUMBER(19, 0) NOT NULL PRIMARY KEY,
    username VARCHAR2(64) NOT NULL UNIQUE,
    email_address VARCHAR2(255) NOT NULL UNIQUE,
    password_hash VARCHAR2(60) NOT NULL,
    full_name VARCHAR2(150),
    account_status VARCHAR2(20) NOT NULL,
    is_deleted NUMBER(1, 0) DEFAULT 0 NOT NULL,
    deleted_at TIMESTAMP,
    version NUMBER(19, 0) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE user_roles (
    user_id NUMBER(19, 0) NOT NULL,
    role_id NUMBER(19, 0) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE user_permissions (
    user_id NUMBER(19, 0) NOT NULL,
    permission_id NUMBER(19, 0) NOT NULL,
    CONSTRAINT pk_user_permissions PRIMARY KEY (user_id, permission_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_up_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

-- -----------------------------------------------------------------------------
-- Sessions & Tokens
-- -----------------------------------------------------------------------------
CREATE TABLE sessions (
    id VARCHAR2(36) NOT NULL PRIMARY KEY,
    user_id NUMBER(19, 0) NOT NULL,
    device_type VARCHAR2(20) NOT NULL,
    device_name VARCHAR2(100),
    os_name VARCHAR2(50),
    os_version VARCHAR2(50),
    browser_name VARCHAR2(50),
    browser_version VARCHAR2(50),
    user_agent VARCHAR2(500),
    ip_address VARCHAR2(45) NOT NULL,
    country_name VARCHAR2(100),
    country_code VARCHAR2(2),
    city_name VARCHAR2(100),
    latitude NUMBER(10, 7),
    longitude NUMBER(10, 7),
    session_status VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoked_reason VARCHAR2(50)
);

CREATE INDEX idx_sessions_user ON sessions(user_id);
CREATE INDEX idx_sessions_user_status ON sessions(user_id, session_status);

CREATE TABLE refresh_tokens (
    id VARCHAR2(36) NOT NULL PRIMARY KEY,
    session_id VARCHAR2(36) NOT NULL,
    token_hash VARCHAR2(128) NOT NULL UNIQUE,
    token_status VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    used_from_ip VARCHAR2(45),
    replaced_by_token_id VARCHAR2(36)
);

CREATE INDEX idx_refresh_tokens_session ON refresh_tokens(session_id);
CREATE INDEX idx_refresh_tokens_status ON refresh_tokens(token_status);

-- -----------------------------------------------------------------------------
-- Audit Log
-- -----------------------------------------------------------------------------
CREATE TABLE admin_audit_log (
    id NUMBER(19, 0) NOT NULL PRIMARY KEY,
    actor_user_id NUMBER(19, 0) NOT NULL,
    actor_username VARCHAR2(64) NOT NULL,
    target_user_id NUMBER(19, 0),
    target_username VARCHAR2(64),
    action_type VARCHAR2(64) NOT NULL,
    payload_json CLOB,
    ip_address VARCHAR2(45),
    outcome VARCHAR2(16) NOT NULL,
    error_message VARCHAR2(500),
    created_at TIMESTAMP NOT NULL
);
