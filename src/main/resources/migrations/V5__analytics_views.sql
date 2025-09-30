-- Analytics and rolling window views for QuickStocks
-- These views optimize common analytics queries

-- Rolling window statistics view (1h, 24h, 7d)
CREATE VIEW IF NOT EXISTS rolling_window_stats AS
SELECT 
    iph.instrument_id,
    COUNT(*) as data_points,
    AVG(iph.price) as avg_price,
    MIN(iph.price) as min_price,
    MAX(iph.price) as max_price,
    CASE 
        WHEN COUNT(*) >= 2 THEN 
            (MAX(iph.price) - MIN(iph.price)) / MIN(iph.price)
        ELSE 0.0 
    END as price_range_pct,
    -- Time window categorization
    CASE 
        WHEN iph.ts >= (strftime('%s', 'now') * 1000 - 60 * 60 * 1000) THEN '1h'
        WHEN iph.ts >= (strftime('%s', 'now') * 1000 - 24 * 60 * 60 * 1000) THEN '24h'  
        WHEN iph.ts >= (strftime('%s', 'now') * 1000 - 7 * 24 * 60 * 60 * 1000) THEN '7d'
        ELSE 'older'
    END as time_window
FROM instrument_price_history iph
WHERE iph.ts >= (strftime('%s', 'now') * 1000 - 7 * 24 * 60 * 60 * 1000) -- Last 7 days
GROUP BY iph.instrument_id, time_window;

-- Recent price movements view for EWMA calculations
CREATE VIEW IF NOT EXISTS price_movements AS
SELECT 
    iph.instrument_id,
    i.symbol,
    i.display_name,
    iph.ts,
    iph.price,
    LAG(iph.price) OVER (PARTITION BY iph.instrument_id ORDER BY iph.ts) as prev_price,
    CASE 
        WHEN LAG(iph.price) OVER (PARTITION BY iph.instrument_id ORDER BY iph.ts) > 0 THEN
            (iph.price - LAG(iph.price) OVER (PARTITION BY iph.instrument_id ORDER BY iph.ts)) / 
            LAG(iph.price) OVER (PARTITION BY iph.instrument_id ORDER BY iph.ts)
        ELSE 0.0
    END as return_pct
FROM instrument_price_history iph
JOIN instruments i ON iph.instrument_id = i.id
WHERE iph.ts >= (strftime('%s', 'now') * 1000 - 24 * 60 * 60 * 1000); -- Last 24 hours

-- Analytics summary view combining state and rolling metrics
CREATE VIEW IF NOT EXISTS analytics_summary AS
SELECT 
    i.id as instrument_id,
    i.symbol,
    i.display_name,
    i.type,
    ist.last_price,
    ist.change_1h,
    ist.change_24h,
    ist.volatility_24h,
    ist.market_cap,
    ist.updated_at,
    -- Add rolling window counts for data quality
    COALESCE(rws_1h.data_points, 0) as data_points_1h,
    COALESCE(rws_24h.data_points, 0) as data_points_24h,
    COALESCE(rws_7d.data_points, 0) as data_points_7d
FROM instruments i
LEFT JOIN instrument_state ist ON i.id = ist.instrument_id
LEFT JOIN rolling_window_stats rws_1h ON i.id = rws_1h.instrument_id AND rws_1h.time_window = '1h'
LEFT JOIN rolling_window_stats rws_24h ON i.id = rws_24h.instrument_id AND rws_24h.time_window = '24h'
LEFT JOIN rolling_window_stats rws_7d ON i.id = rws_7d.instrument_id AND rws_7d.time_window = '7d';

-- Create indexes for better performance on analytics queries
CREATE INDEX IF NOT EXISTS idx_price_history_instrument_ts_price ON instrument_price_history(instrument_id, ts, price);
-- Drop the problematic partial index if it exists
DROP INDEX IF EXISTS idx_price_history_ts_recent;
-- Create a simple index instead (removed partial index with strftime() due to SQLite non-deterministic function restriction)
CREATE INDEX IF NOT EXISTS idx_price_history_ts ON instrument_price_history(ts);