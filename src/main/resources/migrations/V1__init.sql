-- Initial schema for QuickStocks
-- Generic instrument schema supporting items, crypto, stocks, etc.

-- instruments are the single source of truth
CREATE TABLE IF NOT EXISTS instruments (
  id            TEXT PRIMARY KEY,              -- UUID string
  type          TEXT NOT NULL,                 -- ITEM | CRYPTO | EQUITY | INDEX | FUND | CUSTOM_CRYPTO
  symbol        TEXT NOT NULL UNIQUE,          -- e.g., MC_STONE, BTC, AAPL
  display_name  TEXT NOT NULL,                 -- human readable
  mc_material   TEXT,                          -- Bukkit Material name (nullable)
  decimals      INTEGER NOT NULL DEFAULT 0,    -- 0 for items, 8 for crypto, 2 for equities (configurable)
  created_by    TEXT,                          -- UUID of player if CUSTOM_CRYPTO
  created_at    INTEGER NOT NULL               -- epoch millis
);

CREATE INDEX IF NOT EXISTS idx_instruments_type ON instruments(type);

-- latest state for fast reads
CREATE TABLE IF NOT EXISTS instrument_state (
  instrument_id TEXT PRIMARY KEY,
  last_price    REAL NOT NULL,
  last_volume   REAL NOT NULL DEFAULT 0,
  change_1h     REAL NOT NULL DEFAULT 0,       -- percent
  change_24h    REAL NOT NULL DEFAULT 0,       -- percent
  volatility_24h REAL NOT NULL DEFAULT 0,      -- std dev or ATR-like metric
  market_cap    REAL NOT NULL DEFAULT 0,
  updated_at    INTEGER NOT NULL,              -- epoch millis
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

-- append-only price history for analytics
CREATE TABLE IF NOT EXISTS instrument_price_history (
  id            TEXT PRIMARY KEY,
  instrument_id TEXT NOT NULL,
  ts            INTEGER NOT NULL,              -- epoch millis
  price         REAL NOT NULL,
  volume        REAL NOT NULL DEFAULT 0,
  reason        TEXT,                           -- enum factor name(s) behind move
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_hist_instrument_ts ON instrument_price_history(instrument_id, ts);