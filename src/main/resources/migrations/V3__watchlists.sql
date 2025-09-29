-- Watchlists table for QuickStocks
-- This migration adds player watchlist functionality

-- Player watchlists for instruments
CREATE TABLE IF NOT EXISTS user_watchlists (
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  added_at      INTEGER NOT NULL,              -- epoch millis when added
  PRIMARY KEY (player_uuid, instrument_id),
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_watchlists_player ON user_watchlists(player_uuid);
CREATE INDEX IF NOT EXISTS idx_watchlists_added ON user_watchlists(player_uuid, added_at);