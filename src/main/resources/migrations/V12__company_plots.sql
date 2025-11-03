-- Company plots / land ownership system

-- Company plots table
CREATE TABLE IF NOT EXISTS company_plots (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  world_name    TEXT NOT NULL,
  chunk_x       INTEGER NOT NULL,
  chunk_z       INTEGER NOT NULL,
  buy_price     REAL NOT NULL,           -- Original purchase price
  purchased_at  INTEGER NOT NULL,        -- Timestamp of purchase
  rent_amount   REAL NOT NULL DEFAULT -1, -- -1 means free (no rent)
  rent_interval TEXT NOT NULL DEFAULT 'monthly', -- hourly, daily, weekly, monthly
  last_rent_payment INTEGER,             -- Last time rent was paid
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE,
  UNIQUE(world_name, chunk_x, chunk_z)  -- One owner per chunk
);
CREATE INDEX IF NOT EXISTS idx_company_plots_company ON company_plots(company_id);
CREATE INDEX IF NOT EXISTS idx_company_plots_location ON company_plots(world_name, chunk_x, chunk_z);
CREATE INDEX IF NOT EXISTS idx_company_plots_rent_payment ON company_plots(last_rent_payment);

-- Player auto-buy mode tracking
CREATE TABLE IF NOT EXISTS player_auto_buy_mode (
  player_uuid   TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  enabled       INTEGER NOT NULL DEFAULT 0,
  FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_player_auto_buy_company ON player_auto_buy_mode(company_id);
