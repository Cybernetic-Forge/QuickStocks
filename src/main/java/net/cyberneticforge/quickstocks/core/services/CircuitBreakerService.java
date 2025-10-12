package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.config.TradingCfg;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for implementing circuit breaker functionality to halt trading
 * when price movements exceed configured thresholds.
 */
public class CircuitBreakerService {
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final TradingCfg.CircuitBreakerConfig circuitBreakerConfig = QuickStocksPlugin.getTradingCfg().getCircuitBreakersConfig();
    
    /**
     * Checks if trading is currently halted for an instrument.
     * 
     * @param instrumentId The instrument to check
     * @return true if trading is halted, false otherwise
     * @throws SQLException If database operations fail
     */
    public boolean isTradingHalted(String instrumentId) throws SQLException {
        if (!circuitBreakerConfig.isEnable()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check for active halts
        Integer activeHalts = database.queryValue(
            "SELECT COUNT(*) FROM trading_halts WHERE instrument_id = ? AND (end_ts IS NULL OR end_ts > ?)",
            instrumentId, currentTime
        );
        
        return activeHalts != null && activeHalts > 0;
    }
    
    /**
     * Checks price movement and triggers circuit breaker if thresholds are exceeded.
     * 
     * @param instrumentId The instrument to check
     * @param currentPrice The current price
     * @throws SQLException If database operations fail
     */
    public void checkAndTriggerCircuitBreaker(String instrumentId, double currentPrice) throws SQLException {
        if (!circuitBreakerConfig.isEnable() || isTradingHalted(instrumentId)) {
            return;
        }
        
        double sessionOpenPrice = getSessionOpenPrice(instrumentId, currentPrice);
        double priceChangePercent = Math.abs((currentPrice - sessionOpenPrice) / sessionOpenPrice * 100.0);
        
        List<Double> levels = circuitBreakerConfig.getLevels();
        List<Integer> haltMinutes = circuitBreakerConfig.getHaltMinutes();
        
        // Check each level threshold
        for (int i = 0; i < levels.size(); i++) {
            double threshold = levels.get(i);
            if (priceChangePercent >= threshold) {
                // Check if we've already halted at this level today
                if (!hasHaltedAtLevel(instrumentId, i + 1)) {
                    triggerHalt(instrumentId, i + 1, sessionOpenPrice, currentPrice, haltMinutes.get(i));
                }
                break; // Only trigger the first threshold exceeded
            }
        }
    }
    
    /**
     * Gets the session open price for an instrument, creating a new session if needed.
     */
    private double getSessionOpenPrice(String instrumentId, double currentPrice) throws SQLException {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        Double openPrice = database.queryValue(
            "SELECT open_price FROM trading_sessions WHERE instrument_id = ? AND session_date = ?",
            instrumentId, today
        );
        
        if (openPrice == null) {
            // Create new session with current price as open
            database.execute(
                "INSERT INTO trading_sessions (instrument_id, session_date, open_price) VALUES (?, ?, ?)",
                instrumentId, today, currentPrice
            );
            return currentPrice;
        }
        
        return openPrice;
    }
    
    /**
     * Checks if trading has already been halted at a specific level today.
     */
    private boolean hasHaltedAtLevel(String instrumentId, int level) throws SQLException {
        long todayStart = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long todayEnd = todayStart + (24 * 60 * 60 * 1000) - 1;
        
        Integer haltCount = database.queryValue(
            "SELECT COUNT(*) FROM trading_halts WHERE instrument_id = ? AND level = ? AND start_ts BETWEEN ? AND ?",
            instrumentId, level, todayStart, todayEnd
        );
        
        return haltCount != null && haltCount > 0;
    }
    
    /**
     * Triggers a trading halt for an instrument.
     */
    private void triggerHalt(String instrumentId, int level, double sessionOpen, double triggerPrice, int haltMinutes) throws SQLException {
        long currentTime = System.currentTimeMillis();
        Long endTime = null;
        
        if (haltMinutes > 0) {
            endTime = currentTime + (haltMinutes * 60 * 1000L);
        }
        // If haltMinutes is -1, endTime remains null (indefinite halt)
        
        String haltId = UUID.randomUUID().toString();
        database.execute(
            "INSERT INTO trading_halts (id, instrument_id, level, start_ts, end_ts, session_open, trigger_price) VALUES (?, ?, ?, ?, ?, ?, ?)",
            haltId, instrumentId, level, currentTime, endTime, sessionOpen, triggerPrice
        );
    }
    
    /**
     * Gets information about the current halt status for an instrument.
     * 
     * @param instrumentId The instrument to check
     * @return Halt information or null if not halted
     * @throws SQLException If database operations fail
     */
    public HaltInfo getHaltInfo(String instrumentId) throws SQLException {
        long currentTime = System.currentTimeMillis();
        
        Map<String, Object> halt = database.queryRow(
            """
            SELECT level, start_ts, end_ts, session_open, trigger_price\s
            FROM trading_halts\s
            WHERE instrument_id = ? AND (end_ts IS NULL OR end_ts > ?)\s
            ORDER BY start_ts DESC\s
            LIMIT 1
           \s""",
            instrumentId, currentTime
        );
        
        if (halt == null) {
            return null;
        }
        
        int level = ((Number) halt.get("level")).intValue();
        long startTs = ((Number) halt.get("start_ts")).longValue();
        Long endTs = halt.get("end_ts") != null ? ((Number) halt.get("end_ts")).longValue() : null;
        double sessionOpen = ((Number) halt.get("session_open")).doubleValue();
        double triggerPrice = ((Number) halt.get("trigger_price")).doubleValue();
        
        return new HaltInfo(level, startTs, endTs, sessionOpen, triggerPrice);
    }

    /**
         * Information about a trading halt.
         */
        @SuppressWarnings("unused")
        public record HaltInfo(int level, long startTs, Long endTs, double sessionOpen, double triggerPrice) {

        public boolean isIndefinite() {
            return endTs == null;
        }

        public long getRemainingTimeMs() {
                if (endTs == null) return -1;
                return Math.max(0, endTs - System.currentTimeMillis());
            }
        }
}