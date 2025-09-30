-- Company Market Feature
-- Allows companies to sell shares to players

-- Add market-related columns to companies table
ALTER TABLE companies ADD COLUMN symbol TEXT;
ALTER TABLE companies ADD COLUMN on_market INTEGER NOT NULL DEFAULT 0;
ALTER TABLE companies ADD COLUMN market_percentage REAL NOT NULL DEFAULT 70.0;
ALTER TABLE companies ADD COLUMN allow_buyout INTEGER NOT NULL DEFAULT 0;

-- Create index for symbol lookups
CREATE INDEX IF NOT EXISTS idx_companies_symbol ON companies(symbol);

-- Company shareholders table
CREATE TABLE IF NOT EXISTS company_shareholders (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  shares        REAL NOT NULL DEFAULT 0,
  avg_cost      REAL NOT NULL DEFAULT 0,
  purchased_at  INTEGER NOT NULL,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE,
  UNIQUE(company_id, player_uuid)
);

CREATE INDEX IF NOT EXISTS idx_company_shareholders_company ON company_shareholders(company_id);
CREATE INDEX IF NOT EXISTS idx_company_shareholders_player ON company_shareholders(player_uuid);

-- Company share transactions table (audit trail)
CREATE TABLE IF NOT EXISTS company_share_tx (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  player_uuid   TEXT NOT NULL,
  type          TEXT NOT NULL,  -- BUY | SELL
  shares        REAL NOT NULL,
  price         REAL NOT NULL,
  ts            INTEGER NOT NULL,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_company_share_tx_company ON company_share_tx(company_id);
CREATE INDEX IF NOT EXISTS idx_company_share_tx_player ON company_share_tx(player_uuid);
CREATE INDEX IF NOT EXISTS idx_company_share_tx_ts ON company_share_tx(ts);

-- Offline notifications table for market events
CREATE TABLE IF NOT EXISTS player_notifications (
  id            TEXT PRIMARY KEY,
  player_uuid   TEXT NOT NULL,
  type          TEXT NOT NULL,  -- MARKET_ENABLED | MARKET_DISABLED | BUYOUT | SHARE_SALE
  company_id    TEXT,
  message       TEXT NOT NULL,
  created_at    INTEGER NOT NULL,
  read_at       INTEGER,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_player_notifications_player ON player_notifications(player_uuid);
CREATE INDEX IF NOT EXISTS idx_player_notifications_read ON player_notifications(read_at);
