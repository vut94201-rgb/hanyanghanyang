-- =============================================================================
-- V2__add_account_status_check.sql
-- Adds CHECK constraint for account_status column.
-- Migrates any existing data from full enum names to short codes (CodeEnum).
-- =============================================================================

-- Migrate existing data from full enum names → short codes (safe if no data exists)
UPDATE users SET account_status = 'A' WHERE account_status = 'ACTIVE';
UPDATE users SET account_status = 'D' WHERE account_status = 'DISABLED';
UPDATE users SET account_status = 'L' WHERE account_status = 'LOCKED';

-- Add CHECK constraint to enforce only valid short codes
ALTER TABLE users ADD CONSTRAINT chk_account_status
    CHECK (account_status IN ('A', 'D', 'L'));
