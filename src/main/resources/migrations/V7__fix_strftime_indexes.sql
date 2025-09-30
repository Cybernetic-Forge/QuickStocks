-- Fix for SQLite strftime() index issue
-- This migration removes any problematic partial indexes that use strftime() functions

-- Drop the problematic partial index that causes [SQLITE_ERROR] non-deterministic use of strftime() in an index
DROP INDEX IF EXISTS idx_price_history_ts_recent;

-- Ensure we have the basic performance index instead
CREATE INDEX IF NOT EXISTS idx_price_history_ts ON instrument_price_history(ts);