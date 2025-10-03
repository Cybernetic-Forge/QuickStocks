package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.config.TradingConfig;
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
    
    private final Db database;
    private final TradingConfig.CircuitBreakerConfig circuitBreakerConfig;
    
    public CircuitBreakerService(Db database, TradingConfig.CircuitBreakerConfig circuitBreakerConfig) {
        this.database = database;
        this.circuitBreakerConfig = circuitBreakerConfig;
    }
    
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
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
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
            SELECT level, start_ts, end_ts, session_open, trigger_price 
            FROM trading_halts 
            WHERE instrument_id = ? AND (end_ts IS NULL OR end_ts > ?) 
            ORDER BY start_ts DESC 
            LIMIT 1
            """,
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
    public static class HaltInfo {
        private final int level;
        private final long startTs;
        private final Long endTs;
        private final double sessionOpen;
        private final double triggerPrice;
        
        public HaltInfo(int level, long startTs, Long endTs, double sessionOpen, double triggerPrice) {
            this.level = level;
            this.startTs = startTs;
            this.endTs = endTs;
            this.sessionOpen = sessionOpen;
            this.triggerPrice = triggerPrice;
        }
        
        public int getLevel() { return level; }
        public long getStartTs() { return startTs; }
        public Long getEndTs() { return endTs; }
        public double getSessionOpen() { return sessionOpen; }
        public double getTriggerPrice() { return triggerPrice; }
        
        public boolean isIndefinite() { return endTs == null; }
        
        public long getRemainingTimeMs() {
            if (endTs == null) return -1;
            return Math.max(0, endTs - System.currentTimeMillis());
        }
    }
}