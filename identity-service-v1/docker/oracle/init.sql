-- docker/oracle/init.sql
-- File này chạy 1 LẦN khi container Oracle khởi tạo DB lần đầu.
-- Script nằm trong /opt/oracle/scripts/startup/ sẽ tự động được Oracle Free image chạy.
--
-- Mục đích:
--   1. Kết nối vào PDB FREEPDB1 (Pluggable Database mặc định của Oracle Free)
--   2. Tạo user "identity" để app dùng (KHÔNG dùng system trong app code)
--   3. Grant quyền vừa đủ để app chạy + Flyway migrate
--
-- Sau khi container chạy lần đầu, file này sẽ KHÔNG chạy lại.
-- Nếu muốn reset, phải xóa volume oracle-free-data:
--   podman volume rm <project>_oracle-free-data

ALTER SESSION SET CONTAINER = FREEPDB1;

-- Tạo user identity với password identity123
-- Trong Oracle 12c+, username trong PDB không cần prefix C##
CREATE USER identity IDENTIFIED BY identity123
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;

-- Quyền cơ bản để connect và làm việc với schema
GRANT CREATE SESSION TO identity;
GRANT CREATE TABLE TO identity;
GRANT CREATE SEQUENCE TO identity;
GRANT CREATE VIEW TO identity;
GRANT CREATE PROCEDURE TO identity;
GRANT CREATE TRIGGER TO identity;
GRANT CREATE TYPE TO identity;
GRANT CREATE SYNONYM TO identity;

-- Cho phép Flyway tạo bảng schema_history
GRANT UNLIMITED TABLESPACE TO identity;

-- (Tùy chọn) cho phép xem các session đang chạy - hữu ích khi debug
-- GRANT SELECT ON v_$session TO identity;

COMMIT;

-- Xác nhận
SELECT 'User identity created successfully' AS message FROM dual;
