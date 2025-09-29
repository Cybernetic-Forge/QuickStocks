-- Security, Concurrency & Robustness enhancements for QuickStocks
-- This migration adds idempotency keys, optimistic versioning, and audit support

-- Add idempotency key to orders table for preventing duplicate orders
ALTER TABLE orders ADD COLUMN client_idempotency TEXT;

-- Create unique index on idempotency key to enforce uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency ON orders(client_idempotency) WHERE client_idempotency IS NOT NULL;

-- Add version column to user_holdings for optimistic locking
ALTER TABLE user_holdings ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- Add audit log table for tracking integrity checks and repairs
CREATE TABLE IF NOT EXISTS audit_log (
  id            TEXT PRIMARY KEY,
  audit_type    TEXT NOT NULL,     -- INTEGRITY_CHECK | REPAIR | BACKUP
  player_uuid   TEXT,              -- nullable, for player-specific audits
  instrument_id TEXT,              -- nullable, for instrument-specific audits
  details       TEXT NOT NULL,     -- JSON details of the audit/repair
  timestamp     INTEGER NOT NULL,  -- epoch millis
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_type_ts ON audit_log(audit_type, timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_player_ts ON audit_log(player_uuid, timestamp);

-- Add backup metadata table for tracking automated backups
CREATE TABLE IF NOT EXISTS backup_metadata (
  id            TEXT PRIMARY KEY,
  backup_date   TEXT NOT NULL,     -- YYYY-MM-DD format
  backup_type   TEXT NOT NULL,     -- DAILY | MANUAL | EMERGENCY
  file_count    INTEGER NOT NULL DEFAULT 0,
  total_size    INTEGER NOT NULL DEFAULT 0,  -- bytes
  status        TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING | COMPLETED | FAILED
  created_at    INTEGER NOT NULL,  -- epoch millis
  completed_at  INTEGER            -- epoch millis, nullable
);

CREATE INDEX IF NOT EXISTS idx_backup_date ON backup_metadata(backup_date);
CREATE INDEX IF NOT EXISTS idx_backup_status ON backup_metadata(status);