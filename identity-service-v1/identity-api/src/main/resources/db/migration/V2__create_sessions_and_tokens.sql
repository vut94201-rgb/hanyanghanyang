-- =============================================================================
-- V2: sessions + refresh_tokens (UUID-based PK)
-- =============================================================================
-- Lý do tách bảng:
--   * sessions: 1 record / 1 lần login. Track device, location, lifecycle.
--                  ID cũng là "family ID" cho refresh token rotation.
--   * refresh_tokens: nhiều record / 1 session (chain rotation).
--                  Lưu hash, KHÔNG lưu plain token.
--
-- Quyết định kỹ thuật:
--   * PK dùng VARCHAR2(36) chứa UUID (Oracle không có UUID type chuẩn trước 23ai
--     và Hibernate tương thích tốt nhất với chuỗi UUID).
--   * ID generate ở application layer (UUID.randomUUID().toString()).
--   * Không soft delete: session revoke đã có cột status; token expire/used
--     cleanup bằng job dọn rác sau.
-- =============================================================================

-- ----- SESSIONS -------------------------------------------------------------

CREATE TABLE sessions (
    id                  VARCHAR2(36)    NOT NULL,                            -- UUID, cũng là family ID
    user_id             NUMBER(19)      NOT NULL,
    -- Device info (parsed từ User-Agent qua yauaa)
    device_type         VARCHAR2(20)    DEFAULT 'UNKNOWN' NOT NULL,          -- DESKTOP / MOBILE / TABLET / UNKNOWN
    device_name         VARCHAR2(100),                                       -- vd "Chrome 120 on macOS"
    os_name             VARCHAR2(50),
    os_version          VARCHAR2(50),
    browser_name        VARCHAR2(50),
    browser_version     VARCHAR2(50),
    user_agent          VARCHAR2(500),                                       -- raw UA string, fallback debug
    -- Network + geolocation (qua MaxMind GeoLite2-City offline DB)
    ip_address          VARCHAR2(45)    NOT NULL,                            -- 45 ký tự đủ cho IPv6
    country_name        VARCHAR2(100),
    country_code        VARCHAR2(2),                                         -- ISO 3166-1 alpha-2
    city_name           VARCHAR2(100),
    latitude            NUMBER(10,7),                                        -- 7 chữ số phần lẻ ~ độ chính xác 1cm
    longitude           NUMBER(10,7),
    -- Lifecycle
    session_status      VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,           -- ACTIVE / REVOKED / EXPIRED
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    last_active_at      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,       -- update mỗi request authenticated
    expires_at          TIMESTAMP       NOT NULL,
    revoked_at          TIMESTAMP,
    revoked_reason      VARCHAR2(50),                                        -- LOGOUT / TOKEN_REUSE / USER_ACTION / EXPIRED
    CONSTRAINT pk_sessions PRIMARY KEY (id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_sessions_status CHECK (session_status IN ('ACTIVE','REVOKED','EXPIRED')),
    CONSTRAINT ck_sessions_device_type CHECK (device_type IN ('DESKTOP','MOBILE','TABLET','UNKNOWN')),
    CONSTRAINT ck_sessions_revoked_reason CHECK (
        revoked_reason IS NULL
        OR revoked_reason IN ('LOGOUT','TOKEN_REUSE','USER_ACTION','EXPIRED')
    )
);

-- Index theo các query phổ biến:
CREATE INDEX idx_sessions_user ON sessions (user_id);                        -- "tất cả session của user X"
CREATE INDEX idx_sessions_user_status ON sessions (user_id, session_status); -- "session ACTIVE của user X"
CREATE INDEX idx_sessions_expires ON sessions (expires_at);                  -- cleanup job

COMMENT ON TABLE sessions IS 'Phiên đăng nhập. ID dùng làm family ID cho refresh token chain.';
COMMENT ON COLUMN sessions.id IS 'UUID, được generate tại application layer.';
COMMENT ON COLUMN sessions.last_active_at IS 'Được update mỗi request authenticated qua JwtAuthenticationFilter.';
COMMENT ON COLUMN sessions.revoked_reason IS 'LOGOUT | TOKEN_REUSE | USER_ACTION | EXPIRED';


-- ----- REFRESH_TOKENS -------------------------------------------------------

CREATE TABLE refresh_tokens (
    id                      VARCHAR2(36)    NOT NULL,                        -- UUID
    session_id              VARCHAR2(36)    NOT NULL,
    token_hash              VARCHAR2(128)   NOT NULL,                        -- SHA-256 hex = 64; để dư cho thuật toán hash mạnh hơn
    token_status            VARCHAR2(20)    DEFAULT 'ACTIVE' NOT NULL,       -- ACTIVE / USED / REVOKED
    created_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    expires_at              TIMESTAMP       NOT NULL,
    used_at                 TIMESTAMP,
    used_from_ip            VARCHAR2(45),
    replaced_by_token_id    VARCHAR2(36),                                    -- self-FK, chain rotation
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),                   -- lookup phải nhanh + duy nhất
    CONSTRAINT fk_refresh_tokens_session FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id),
    CONSTRAINT ck_refresh_tokens_status CHECK (token_status IN ('ACTIVE','USED','REVOKED'))
);

-- Index:
CREATE INDEX idx_refresh_tokens_session ON refresh_tokens (session_id);      -- "all tokens of session X" (revoke family)
CREATE INDEX idx_refresh_tokens_status ON refresh_tokens (token_status);
CREATE INDEX idx_refresh_tokens_replaced ON refresh_tokens (replaced_by_token_id);

COMMENT ON TABLE refresh_tokens IS 'Refresh token chain (rotation + reuse detection). Lưu hash, không bao giờ lưu plain.';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hex của raw token. Lookup bằng hash, không bao giờ so sánh plain.';
COMMENT ON COLUMN refresh_tokens.replaced_by_token_id IS 'Trỏ tới token kế tiếp trong chain. NULL nếu là token mới nhất.';
