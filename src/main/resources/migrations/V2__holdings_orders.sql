-- Holdings and Orders tables for QuickStocks
-- This migration adds player portfolio management and order tracking

-- Player holdings for instruments (portfolio)
CREATE TABLE IF NOT EXISTS user_holdings (
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  qty           REAL NOT NULL DEFAULT 0,
  avg_cost      REAL NOT NULL DEFAULT 0,
  PRIMARY KEY (player_uuid, instrument_id),
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

-- Order history and tracking
CREATE TABLE IF NOT EXISTS orders (
  id            TEXT PRIMARY KEY,
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  side          TEXT NOT NULL,     -- BUY | SELL
  qty           REAL NOT NULL,
  price         REAL NOT NULL,
  ts            INTEGER NOT NULL,
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_player_ts ON orders(player_uuid, ts);

-- Internal wallet system for economy (fallback if Vault not available)
CREATE TABLE IF NOT EXISTS wallets (
  player_uuid TEXT PRIMARY KEY,
  balance     REAL NOT NULL DEFAULT 0
);