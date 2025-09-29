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

-- Portfolio performance summary view
CREATE VIEW IF NOT EXISTS portfolio_performance AS
SELECT 
    ph.player_uuid,
    COUNT(*) as data_points,
    MIN(ph.ts) as start_time,
    MAX(ph.ts) as end_time,
    -- Performance metrics
    FIRST_VALUE(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts ASC) as initial_value,
    LAST_VALUE(ph.total_value) OVER (PARTITION BY ph.player_uuid ORDER BY ph.ts ASC 
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) as final_value,
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

-- Sharpe ratio calculation helper view (for players with sufficient data)
CREATE VIEW IF NOT EXISTS sharpe_ratio_data AS
SELECT 
    pp.player_uuid,
    COUNT(pp.daily_return) as return_count,
    AVG(pp.daily_return) as avg_return,
    -- Standard deviation of returns
    CASE 
        WHEN COUNT(pp.daily_return) > 1 THEN
            SQRT(SUM((pp.daily_return - AVG(pp.daily_return)) * (pp.daily_return - AVG(pp.daily_return))) / (COUNT(pp.daily_return) - 1))
        ELSE 0.0
    END as return_std_dev,
    -- Total return
    CASE 
        WHEN pp.initial_value > 0 THEN
            (pp.final_value - pp.initial_value) / pp.initial_value
        ELSE 0.0
    END as total_return
FROM portfolio_performance pp
WHERE pp.daily_return IS NOT NULL
GROUP BY pp.player_uuid
HAVING COUNT(pp.daily_return) >= 5; -- Need at least 5 data points for meaningful Sharpe ratio