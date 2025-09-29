-- Trading Economy features for QuickStocks
-- This migration adds support for fees, limits, circuit breakers, order types, and slippage

-- Add new columns to orders table for enhanced order tracking
ALTER TABLE orders ADD COLUMN order_type TEXT NOT NULL DEFAULT 'MARKET'; -- MARKET | LIMIT | STOP
ALTER TABLE orders ADD COLUMN limit_price REAL; -- For limit orders
ALTER TABLE orders ADD COLUMN stop_price REAL;  -- For stop orders  
ALTER TABLE orders ADD COLUMN fee_paid REAL NOT NULL DEFAULT 0; -- Trading fee paid
ALTER TABLE orders ADD COLUMN execution_price REAL; -- Actual execution price (with slippage)

-- Trading halts table for circuit breaker tracking
CREATE TABLE IF NOT EXISTS trading_halts (
  id            TEXT PRIMARY KEY,
  instrument_id TEXT NOT NULL,
  level         INTEGER NOT NULL,    -- 1=7%, 2=13%, 3=20%
  start_ts      INTEGER NOT NULL,    -- epoch millis when halt started
  end_ts        INTEGER,             -- epoch millis when halt ends (NULL = indefinite)
  session_open  REAL NOT NULL,       -- price at session open for % calculation
  trigger_price REAL NOT NULL,       -- price that triggered the halt
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_trading_halts_instrument ON trading_halts(instrument_id);
CREATE INDEX IF NOT EXISTS idx_trading_halts_active ON trading_halts(instrument_id, end_ts);

-- Player rate limiting tracking
CREATE TABLE IF NOT EXISTS player_trade_limits (
  player_uuid     TEXT NOT NULL,
  minute_start    INTEGER NOT NULL,   -- epoch millis of minute start
  notional_used   REAL NOT NULL DEFAULT 0, -- notional value used this minute
  last_trade_ts   INTEGER NOT NULL DEFAULT 0, -- last trade timestamp for cooldown
  PRIMARY KEY (player_uuid, minute_start)
);

CREATE INDEX IF NOT EXISTS idx_player_limits_ts ON player_trade_limits(minute_start);

-- Session tracking for circuit breakers (daily open prices)
CREATE TABLE IF NOT EXISTS trading_sessions (
  instrument_id TEXT NOT NULL,
  session_date  TEXT NOT NULL,       -- YYYY-MM-DD format
  open_price    REAL NOT NULL,
  PRIMARY KEY (instrument_id, session_date),
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);