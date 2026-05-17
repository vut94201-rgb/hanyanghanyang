-- =============================================================================
-- V3: Seed data mặc định
-- =============================================================================
-- Mục đích: app khởi động xong là login thử được ngay với tài khoản admin.
--
-- Tài khoản mặc định:
--     username:  admin
--     password:  Admin@123
--     email:     admin@example.com
--     role:      ADMIN (có toàn bộ permission)
--
-- CẢNH BÁO: tài khoản này CHỈ dành cho dev/demo. Production PHẢI:
--     1) Đổi password ngay sau lần đăng nhập đầu, hoặc
--     2) Disable user này và tạo admin riêng từ profile production.
--
-- BCrypt hash dưới đây được sinh bằng thuật toán $2a$ cost 10 - tương thích
-- 100% với BCryptPasswordEncoder mặc định của Spring Security 6.
-- =============================================================================

-- ----- ROLES ---------------------------------------------------------------

INSERT INTO roles (id, role_code, role_name, description)
VALUES (roles_seq.NEXTVAL, 'ADMIN', 'Administrator', 'Toàn quyền hệ thống');

INSERT INTO roles (id, role_code, role_name, description)
VALUES (roles_seq.NEXTVAL, 'USER', 'Normal User', 'Người dùng thông thường');

INSERT INTO roles (id, role_code, role_name, description)
VALUES (roles_seq.NEXTVAL, 'MODERATOR', 'Moderator', 'Người kiểm duyệt nội dung');


-- ----- PERMISSIONS ---------------------------------------------------------

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'user:read', 'Đọc thông tin user');

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'user:write', 'Tạo / sửa user');

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'user:delete', 'Xóa user');

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'role:read', 'Đọc thông tin role');

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'role:write', 'Tạo / sửa role và gán permission');

INSERT INTO permissions (id, permission_code, description)
VALUES (permissions_seq.NEXTVAL, 'session:manage', 'Xem và revoke session của user khác');


-- ----- ROLE_PERMISSIONS ----------------------------------------------------
-- ADMIN: tất cả permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'ADMIN';

-- MODERATOR: chỉ đọc user + quản lý session
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'MODERATOR'
  AND p.permission_code IN ('user:read', 'session:manage');

-- USER: chỉ đọc user (xem profile của chính mình - logic check ownership ở service layer)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.role_code = 'USER'
  AND p.permission_code = 'user:read';


-- ----- USERS ---------------------------------------------------------------
-- Hash của password "Admin@123" sinh bằng BCrypt $2a$ cost 10.
-- Đã verify khớp với BCryptPasswordEncoder của Spring Security 6.
INSERT INTO users (
    id, username, email_address, password_hash, full_name, account_status
)
VALUES (
    users_seq.NEXTVAL,
    'admin',
    'admin@example.com',
    '$2a$10$3OWnOUGuZz25pxc/BDg1R.S8fok0HIjrhlfjGEQI2JZohoMb8l8ha',
    'Default Administrator',
    'ACTIVE'
);


-- ----- USER_ROLES ----------------------------------------------------------
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u CROSS JOIN roles r
WHERE u.username = 'admin' AND r.role_code = 'ADMIN';


COMMIT;
