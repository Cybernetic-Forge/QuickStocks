-- Portfolio tracking for Sharpe ratio calculations

-- Portfolio value history for tracking player performance over time
CREATE TABLE IF NOT EXISTS portfolio_history (
  id            TEXT PRIMARY KEY,              -- UUID string
  player_uuid   TEXT NOT NULL,                 -- Player UUID
  ts            INTEGER NOT NULL,              -- Timestamp (epoch millis)
  total_value   REAL NOT NULL,                 -- Total portfolio value at this point
  cash_balance  REAL NOT NULL DEFAULT 0,       -- Cash balance
  holdings_value REAL NOT NULL DEFAULT 0,      -- Value of all holdings
  positions     TEXT,                          -- JSON of position details (optional)
  created_at    INTEGER NOT NULL               -- Record creation time
);

CREATE INDEX IF NOT EXISTS idx_portfolio_history_player_ts ON portfolio_history(player_uuid, ts);

-- Portfolio daily returns view (separated from aggregations for SQLite compatibility)
CREATE VIEW IF NOT EXISTS portfolio_daily_returns AS
SELECT
    ph.player_uuid,
    ph.ts,
    ph.total_value,
    -- Calculate daily returns (approximation)
    LAG(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts) as prev_value,
    CASE
        WHEN LAG(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts) > 0 THEN
            (ph.total_value - LAG(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts)) /
            LAG(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts)
        ELSE 0.0
    END as daily_return
FROM portfolio_history ph
WHERE ph.ts >= (strftime('%s', 'now') * 1000 - 30 * 24 * 60 * 60 * 1000) -- Last 30 days
ORDER BY ph.player_uuid, ph.ts;

-- Portfolio performance summary view (aggregated metrics)
CREATE VIEW IF NOT EXISTS portfolio_performance AS
SELECT
    pdr.player_uuid,
    COUNT(*) as data_points,
    MIN(pdr.ts) as start_time,
    MAX(pdr.ts) as end_time,
    -- Get initial and final values using subqueries for better SQLite compatibility
    (SELECT pdr2.total_value FROM portfolio_daily_returns pdr2
     WHERE pdr2.player_uuid = pdr.player_uuid
     ORDER BY pdr2.ts ASC LIMIT 1) as initial_value,
    (SELECT pdr2.total_value FROM portfolio_daily_returns pdr2
     WHERE pdr2.player_uuid = pdr.player_uuid
     ORDER BY pdr2.ts DESC LIMIT 1) as final_value,
    AVG(pdr.daily_return) as avg_daily_return
FROM portfolio_daily_returns pdr
GROUP BY pdr.player_uuid;

-- Sharpe ratio calculation helper view (for players with sufficient data)
CREATE VIEW IF NOT EXISTS sharpe_ratio_data AS
SELECT
    pdr.player_uuid,
    COUNT(pdr.daily_return) as return_count,
    AVG(pdr.daily_return) as avg_return,
    -- Standard deviation of returns using the mathematical identity: Var(X) = E[X²] - E[X]²
    CASE
        WHEN COUNT(pdr.daily_return) > 1 THEN
            SQRT(
                (SUM(pdr.daily_return * pdr.daily_return) - 
                 COUNT(pdr.daily_return) * AVG(pdr.daily_return) * AVG(pdr.daily_return)) 
                / (COUNT(pdr.daily_return) - 1)
            )
        ELSE 0.0
    END as return_std_dev,
    -- Total return calculated from performance view
    CASE
        WHEN pp.initial_value > 0 THEN
            (pp.final_value - pp.initial_value) / pp.initial_value
        ELSE 0.0
    END as total_return
FROM portfolio_daily_returns pdr
JOIN portfolio_performance pp ON pdr.player_uuid = pp.player_uuid
WHERE pdr.daily_return IS NOT NULL
GROUP BY pdr.player_uuid, pp.initial_value, pp.final_value
HAVING COUNT(pdr.daily_return) >= 5; -- Need at least 5 data points for meaningful Sharpe ratio