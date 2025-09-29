package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages player holdings (portfolios) including position tracking.
 */
public class HoldingsService {
    
    private static final Logger logger = Logger.getLogger(HoldingsService.class.getName());
    
    private final Db database;
    
    public HoldingsService(Db database) {
        this.database = database;
    }
    
    /**
     * Gets all holdings for a player.
     */
    public List<Holding> getHoldings(String playerUuid) throws SQLException {
        List<Map<String, Object>> results = database.query(
            """
            SELECT h.instrument_id, h.qty, h.avg_cost, i.symbol, i.display_name, s.last_price
            FROM user_holdings h
            JOIN instruments i ON h.instrument_id = i.id
            LEFT JOIN instrument_state s ON h.instrument_id = s.instrument_id
            WHERE h.player_uuid = ? AND h.qty > 0
            ORDER BY i.symbol
            """,
            playerUuid
        );
        
        List<Holding> holdings = new ArrayList<>();
        for (Map<String, Object> row : results) {
            holdings.add(new Holding(
                (String) row.get("instrument_id"),
                (String) row.get("symbol"),
                (String) row.get("display_name"),
                ((Number) row.get("qty")).doubleValue(),
                ((Number) row.get("avg_cost")).doubleValue(),
                row.get("last_price") != null ? ((Number) row.get("last_price")).doubleValue() : 0.0
            ));
        }
        
        return holdings;
    }
    
    /**
     * Gets a specific holding for a player and instrument.
     */
    public Holding getHolding(String playerUuid, String instrumentId) {
        try {
            Map<String, Object> result = database.queryRow(
                """
                SELECT h.instrument_id, h.qty, h.avg_cost, i.symbol, i.display_name, s.last_price
                FROM user_holdings h
                JOIN instruments i ON h.instrument_id = i.id
                LEFT JOIN instrument_state s ON h.instrument_id = s.instrument_id
                WHERE h.player_uuid = ? AND h.instrument_id = ?
                """,
                playerUuid, instrumentId
            );
            
            if (result == null) {
                return null;
            }
            
            return new Holding(
                (String) result.get("instrument_id"),
                (String) result.get("symbol"),
                (String) result.get("display_name"),
                ((Number) result.get("qty")).doubleValue(),
                ((Number) result.get("avg_cost")).doubleValue(),
                result.get("last_price") != null ? ((Number) result.get("last_price")).doubleValue() : 0.0
            );
        } catch (SQLException e) {
            logger.warning("Error getting holding for player " + playerUuid + " and instrument " + instrumentId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Adds shares to a holding (for BUY orders).
     */
    public void addHolding(String playerUuid, String instrumentId, double qty, double price) throws SQLException {
        Holding existing = getHolding(playerUuid, instrumentId);
        
        if (existing == null || existing.getQty() == 0) {
            // New holding
            database.execute(
                "INSERT OR REPLACE INTO user_holdings (player_uuid, instrument_id, qty, avg_cost) VALUES (?, ?, ?, ?)",
                playerUuid, instrumentId, qty, price
            );
        } else {
            // Update existing holding with new average cost
            double totalValue = (existing.getQty() * existing.getAvgCost()) + (qty * price);
            double newQty = existing.getQty() + qty;
            double newAvgCost = totalValue / newQty;
            
            database.execute(
                "UPDATE user_holdings SET qty = ?, avg_cost = ? WHERE player_uuid = ? AND instrument_id = ?",
                newQty, newAvgCost, playerUuid, instrumentId
            );
        }
        
        logger.fine("Added " + qty + " shares of " + instrumentId + " for " + playerUuid + " at $" + price);
    }
    
    /**
     * Removes shares from a holding (for SELL orders).
     * @return true if successful, false if insufficient shares
     */
    public boolean removeHolding(String playerUuid, String instrumentId, double qty) throws SQLException {
        Holding existing = getHolding(playerUuid, instrumentId);
        
        if (existing == null || existing.getQty() < qty) {
            return false; // Insufficient shares
        }
        
        double newQty = existing.getQty() - qty;
        
        if (newQty <= 0) {
            // Remove holding entirely
            database.execute(
                "DELETE FROM user_holdings WHERE player_uuid = ? AND instrument_id = ?",
                playerUuid, instrumentId
            );
        } else {
            // Update quantity (keep same average cost)
            database.execute(
                "UPDATE user_holdings SET qty = ? WHERE player_uuid = ? AND instrument_id = ?",
                newQty, playerUuid, instrumentId
            );
        }
        
        logger.fine("Removed " + qty + " shares of " + instrumentId + " for " + playerUuid);
        return true;
    }
    
    /**
     * Gets the total portfolio value for a player.
     */
    public double getPortfolioValue(String playerUuid) throws SQLException {
        List<Holding> holdings = getHoldings(playerUuid);
        return holdings.stream()
            .mapToDouble(h -> h.getQty() * h.getCurrentPrice())
            .sum();
    }
    
    /**
     * Gets a specific holding for a player and instrument with version for locking.
     */
    public Holding getHoldingWithLock(String playerUuid, String instrumentId) {
        try {
            Map<String, Object> result = database.queryRow(
                """
                SELECT h.instrument_id, h.qty, h.avg_cost, h.version, i.symbol, i.display_name, s.last_price
                FROM user_holdings h
                JOIN instruments i ON h.instrument_id = i.id
                LEFT JOIN instrument_state s ON h.instrument_id = s.instrument_id
                WHERE h.player_uuid = ? AND h.instrument_id = ?
                """,
                playerUuid, instrumentId
            );
            
            if (result == null) {
                return null;
            }
            
            return new Holding(
                (String) result.get("instrument_id"),
                (String) result.get("symbol"),
                (String) result.get("display_name"),
                ((Number) result.get("qty")).doubleValue(),
                ((Number) result.get("avg_cost")).doubleValue(),
                result.get("last_price") != null ? ((Number) result.get("last_price")).doubleValue() : 0.0,
                ((Number) result.get("version")).intValue()
            );
        } catch (SQLException e) {
            logger.warning("Failed to get holding with lock for " + playerUuid + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Adds shares to a holding with optimistic versioning (for use within transactions).
     */
    public boolean addHoldingWithVersioning(Db.TransactionDb db, String playerUuid, String instrumentId, double qty, double price) throws SQLException {
        // Get existing holding
        Holding existing = getHolding(playerUuid, instrumentId);
        
        int retries = 3;
        while (retries > 0) {
            try {
                if (existing == null || existing.getQty() == 0) {
                    // New holding
                    int rowsAffected = db.execute(
                        "INSERT OR REPLACE INTO user_holdings (player_uuid, instrument_id, qty, avg_cost, version) VALUES (?, ?, ?, ?, ?)",
                        playerUuid, instrumentId, qty, price, 1
                    );
                    return rowsAffected > 0;
                } else {
                    // Update existing holding with new average cost and increment version
                    double totalValue = (existing.getQty() * existing.getAvgCost()) + (qty * price);
                    double newQty = existing.getQty() + qty;
                    double newAvgCost = totalValue / newQty;
                    
                    int rowsAffected = db.execute(
                        "UPDATE user_holdings SET qty = ?, avg_cost = ?, version = version + 1 WHERE player_uuid = ? AND instrument_id = ? AND version = ?",
                        newQty, newAvgCost, playerUuid, instrumentId, existing.getVersion()
                    );
                    
                    if (rowsAffected == 0) {
                        // Version conflict, retry
                        logger.fine("Version conflict when adding holding, retrying... (" + retries + " retries left)");
                        existing = getHolding(playerUuid, instrumentId);
                        retries--;
                        continue;
                    }
                    
                    return true;
                }
            } catch (SQLException e) {
                logger.warning("Failed to add holding with versioning: " + e.getMessage());
                throw e;
            }
        }
        
        logger.warning("Failed to add holding after retries due to version conflicts");
        return false;
    }
    
    /**
     * Removes shares from a holding with optimistic versioning (for use within transactions).
     */
    public boolean removeHoldingWithVersioning(Db.TransactionDb db, String playerUuid, String instrumentId, double qty, int expectedVersion) throws SQLException {
        int retries = 3;
        while (retries > 0) {
            try {
                Holding existing = getHolding(playerUuid, instrumentId);
                
                if (existing == null || existing.getQty() < qty) {
                    return false; // Insufficient shares
                }
                
                double newQty = existing.getQty() - qty;
                
                if (newQty <= 0) {
                    // Remove holding entirely
                    int rowsAffected = db.execute(
                        "DELETE FROM user_holdings WHERE player_uuid = ? AND instrument_id = ? AND version = ?",
                        playerUuid, instrumentId, expectedVersion
                    );
                    
                    if (rowsAffected == 0) {
                        // Version conflict, retry
                        logger.fine("Version conflict when removing holding, retrying... (" + retries + " retries left)");
                        retries--;
                        continue;
                    }
                } else {
                    // Update quantity and increment version (keep same average cost)
                    int rowsAffected = db.execute(
                        "UPDATE user_holdings SET qty = ?, version = version + 1 WHERE player_uuid = ? AND instrument_id = ? AND version = ?",
                        newQty, playerUuid, instrumentId, expectedVersion
                    );
                    
                    if (rowsAffected == 0) {
                        // Version conflict, retry
                        logger.fine("Version conflict when updating holding, retrying... (" + retries + " retries left)");
                        retries--;
                        continue;
                    }
                }
                
                return true;
                
            } catch (SQLException e) {
                logger.warning("Failed to remove holding with versioning: " + e.getMessage());
                throw e;
            }
        }
        
        logger.warning("Failed to remove holding after retries due to version conflicts");
        return false;
    }
    
    /**
     * Represents a player's holding in an instrument.
     */
    public static class Holding {
        private final String instrumentId;
        private final String symbol;
        private final String displayName;
        private final double qty;
        private final double avgCost;
        private final double currentPrice;
        private final int version;
        
        public Holding(String instrumentId, String symbol, String displayName,
                      double qty, double avgCost, double currentPrice) {
            this(instrumentId, symbol, displayName, qty, avgCost, currentPrice, 1);
        }
        
        public Holding(String instrumentId, String symbol, String displayName,
                      double qty, double avgCost, double currentPrice, int version) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.displayName = displayName;
            this.qty = qty;
            this.avgCost = avgCost;
            this.currentPrice = currentPrice;
            this.version = version;
        }
        
        public String getInstrumentId() { return instrumentId; }
        public String getSymbol() { return symbol; }
        public String getDisplayName() { return displayName; }
        public double getQty() { return qty; }
        public double getAvgCost() { return avgCost; }
        public double getCurrentPrice() { return currentPrice; }
        public int getVersion() { return version; }
        
        public double getTotalValue() { return qty * currentPrice; }
        public double getTotalCost() { return qty * avgCost; }
        public double getUnrealizedPnL() { return getTotalValue() - getTotalCost(); }
        public double getUnrealizedPnLPercent() { 
            return getTotalCost() > 0 ? (getUnrealizedPnL() / getTotalCost()) * 100 : 0; 
        }
    }
}