package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.config.TradingConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.Map;

/**
 * Service for enforcing trading rate limits and cooldowns per player.
 */
public class RateLimitService {
    
    private final Db database;
    private final TradingConfig.LimitsConfig limitsConfig;
    
    public RateLimitService(Db database, TradingConfig.LimitsConfig limitsConfig) {
        this.database = database;
        this.limitsConfig = limitsConfig;
    }
    
    /**
     * Checks if a trade is allowed based on quantity, notional limits, and cooldown.
     * 
     * @param playerUuid The player attempting to trade
     * @param qty The order quantity
     * @param notionalValue The notional value of the trade
     * @return A validation result indicating if the trade is allowed
     * @throws SQLException If database operations fail
     */
    public ValidationResult validateTrade(String playerUuid, double qty, double notionalValue) throws SQLException {
        // Check quantity limit
        if (qty > limitsConfig.getMaxOrderQty()) {
            return new ValidationResult(false, String.format(
                "Order quantity %.2f exceeds maximum allowed %.2f", 
                qty, limitsConfig.getMaxOrderQty()
            ));
        }
        
        // Check cooldown
        Long lastTradeTs = database.queryValue(
            "SELECT last_trade_ts FROM player_trade_limits WHERE player_uuid = ? ORDER BY minute_start DESC LIMIT 1",
            playerUuid
        );
        
        long currentTime = System.currentTimeMillis();
        if (lastTradeTs != null && (currentTime - lastTradeTs) < limitsConfig.getPerPlayerCooldownMs()) {
            long remainingMs = limitsConfig.getPerPlayerCooldownMs() - (currentTime - lastTradeTs);
            return new ValidationResult(false, String.format(
                "Trading cooldown active. Please wait %.1f seconds", 
                remainingMs / 1000.0
            ));
        }
        
        // Check notional limit per minute
        long currentMinute = getCurrentMinuteStart(currentTime);
        Double currentNotionalUsed = database.queryValue(
            "SELECT notional_used FROM player_trade_limits WHERE player_uuid = ? AND minute_start = ?",
            playerUuid, currentMinute
        );
        
        if (currentNotionalUsed == null) {
            currentNotionalUsed = 0.0;
        }
        
        if (currentNotionalUsed + notionalValue > limitsConfig.getMaxNotionalPerMinute()) {
            return new ValidationResult(false, String.format(
                "Adding this trade (%.2f) would exceed the per-minute notional limit of %.2f. Current usage: %.2f",
                notionalValue, limitsConfig.getMaxNotionalPerMinute(), currentNotionalUsed
            ));
        }
        
        return new ValidationResult(true, "Trade allowed");
    }
    
    /**
     * Records a successful trade for rate limiting purposes.
     * 
     * @param playerUuid The player who made the trade
     * @param notionalValue The notional value of the trade
     * @throws SQLException If database operations fail
     */
    public void recordTrade(String playerUuid, double notionalValue) throws SQLException {
        long currentTime = System.currentTimeMillis();
        long currentMinute = getCurrentMinuteStart(currentTime);
        
        // Update or insert rate limit record
        Map<String, Object> existing = database.queryRow(
            "SELECT notional_used FROM player_trade_limits WHERE player_uuid = ? AND minute_start = ?",
            playerUuid, currentMinute
        );
        
        if (existing != null) {
            // Update existing record
            double newNotional = ((Number) existing.get("notional_used")).doubleValue() + notionalValue;
            database.execute(
                "UPDATE player_trade_limits SET notional_used = ?, last_trade_ts = ? WHERE player_uuid = ? AND minute_start = ?",
                newNotional, currentTime, playerUuid, currentMinute
            );
        } else {
            // Insert new record
            database.execute(
                "INSERT INTO player_trade_limits (player_uuid, minute_start, notional_used, last_trade_ts) VALUES (?, ?, ?, ?)",
                playerUuid, currentMinute, notionalValue, currentTime
            );
        }
        
        // Clean up old records (older than 1 hour)
        long oneHourAgo = currentTime - (60 * 60 * 1000);
        database.execute(
            "DELETE FROM player_trade_limits WHERE minute_start < ?",
            oneHourAgo
        );
    }
    
    /**
     * Gets the start of the current minute in epoch milliseconds.
     */
    private long getCurrentMinuteStart(long currentTime) {
        return (currentTime / 60000) * 60000; // Round down to minute boundary
    }
    
    /**
     * Result of rate limit validation.
     */
    public static class ValidationResult {
        private final boolean allowed;
        private final String message;
        
        public ValidationResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public boolean isAllowed() { return allowed; }
        public String getMessage() { return message; }
    }
}