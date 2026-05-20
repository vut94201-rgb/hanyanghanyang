-- =============================================================================
-- V5: admin_audit_log — log mọi action admin (disable user, gán role, ...)
-- =============================================================================
-- Mục đích:
--   Compliance (SOC2, ISO 27001) yêu cầu mọi action ảnh hưởng đến security
--   posture của tài khoản phải được log với 5W: Who (actor), When (timestamp),
--   What (action), Where (IP), Why (reason/payload diff).
--
-- Khác gì với Session/RefreshToken log:
--   - Session/Token track hành vi USER bình thường.
--   - admin_audit_log track hành vi ADMIN tác động lên user khác.
--   Tách bảng → query nhanh, retention policy khác (audit thường giữ lâu hơn).
--
-- KHÔNG dùng cho event business thường (vd: user login). Chỉ admin action.
-- =============================================================================

CREATE SEQUENCE admin_audit_log_seq START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;

CREATE TABLE admin_audit_log (
    id                  NUMBER(19)      NOT NULL,
    -- Actor: admin gây ra action. NOT NULL vì mọi action phải có người gây.
    actor_user_id       NUMBER(19)      NOT NULL,
    actor_username      VARCHAR2(64)    NOT NULL,
    -- Target: user bị tác động. Nullable vì có action không nhắm vào user cụ thể
    -- (vd: query list — nhưng hiện tại ta chỉ log mutation, vẫn để nullable phòng hờ).
    target_user_id      NUMBER(19),
    target_username     VARCHAR2(64),
    -- Loại action — enum trong code (xem AdminAction.java). Lưu dạng string
    -- để dễ đọc khi query trực tiếp Oracle bằng sqlplus.
    action_type         VARCHAR2(64)    NOT NULL,
    -- Payload: JSON serialize. Vd: {"oldStatus":"ACTIVE","newStatus":"DISABLED"}
    -- Dùng CLOB vì payload có thể dài (gán nhiều role một lúc).
    payload_json        CLOB,
    -- IP gây action (từ JWT request).
    ip_address          VARCHAR2(45),     -- IPv6 max 39 ký tự, để 45 phòng hờ
    -- Outcome: SUCCESS/FAILURE. Failure cũng log (vd: thử disable user không tồn tại).
    outcome             VARCHAR2(16)    DEFAULT 'SUCCESS' NOT NULL,
    error_message       VARCHAR2(500),
    -- Timestamp riêng — KHÔNG dùng created_at vì bảng audit không cần soft delete.
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,

    CONSTRAINT pk_admin_audit_log PRIMARY KEY (id),
    CONSTRAINT ck_audit_outcome CHECK (outcome IN ('SUCCESS','FAILURE'))
);

-- Index để query list bằng filter phổ biến:
--   - "Show actions của admin X trong tháng qua" → (actor_user_id, created_at)
--   - "Ai đã đụng vào user Y?" → (target_user_id, created_at)
--   - "List loại action Z" → action_type
CREATE INDEX idx_audit_actor ON admin_audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_target ON admin_audit_log (target_user_id, created_at DESC);
CREATE INDEX idx_audit_action ON admin_audit_log (action_type);
CREATE INDEX idx_audit_created ON admin_audit_log (created_at DESC);

COMMENT ON TABLE admin_audit_log IS 'Audit trail cho admin action. KHÔNG xoá - giữ vĩnh viễn hoặc theo policy.';
COMMENT ON COLUMN admin_audit_log.payload_json IS 'JSON snapshot của thay đổi (old → new). Format do AuditLogger quyết định.';
COMMENT ON COLUMN admin_audit_log.outcome IS 'SUCCESS hoặc FAILURE. Failure cũng log để detect probing.';

-- LƯU Ý:
--   KHÔNG add foreign key tới users(id). Lý do:
--     1) Nếu user bị hard-delete (compliance request từ GDPR), audit log vẫn
--        phải giữ — FK sẽ chặn.
--     2) Audit log có thể tham chiếu user không còn tồn tại — chấp nhận
--        denormalize username vào audit row.
