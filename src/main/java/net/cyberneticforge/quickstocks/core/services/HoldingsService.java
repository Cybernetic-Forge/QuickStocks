package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Manages player holdings (portfolios) including position tracking.
 */
public class HoldingsService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    
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
        
        if (existing == null || existing.qty() == 0) {
            // New holding
            database.execute(
                "INSERT OR REPLACE INTO user_holdings (player_uuid, instrument_id, qty, avg_cost) VALUES (?, ?, ?, ?)",
                playerUuid, instrumentId, qty, price
            );
        } else {
            // Update existing holding with new average cost
            double totalValue = (existing.qty() * existing.avgCost()) + (qty * price);
            double newQty = existing.qty() + qty;
            double newAvgCost = totalValue / newQty;
            
            database.execute(
                "UPDATE user_holdings SET qty = ?, avg_cost = ? WHERE player_uuid = ? AND instrument_id = ?",
                newQty, newAvgCost, playerUuid, instrumentId
            );
        }
        
        logger.debug("Added " + qty + " shares of " + instrumentId + " for " + playerUuid + " at $" + price);
    }
    
    /**
     * Removes shares from a holding (for SELL orders).
     * @return true if successful, false if insufficient shares
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean removeHolding(String playerUuid, String instrumentId, double qty) throws SQLException {
        Holding existing = getHolding(playerUuid, instrumentId);
        
        if (existing == null || existing.qty() < qty) {
            return false; // Insufficient shares
        }
        
        double newQty = existing.qty() - qty;
        
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
        
        logger.debug("Removed " + qty + " shares of " + instrumentId + " for " + playerUuid);
        return true;
    }
    
    /**
     * Gets the total portfolio value for a player.
     */
    public double getPortfolioValue(String playerUuid) throws SQLException {
        List<Holding> holdings = getHoldings(playerUuid);
        return holdings.stream()
            .mapToDouble(h -> h.qty() * h.currentPrice())
            .sum();
    }

    /**
         * Represents a player's holding in an instrument.
         */
        public record Holding(String instrumentId, String symbol, String displayName, double qty, double avgCost,
                              double currentPrice) {

        public double getTotalValue() {
            return qty * currentPrice;
        }

        public double getTotalCost() {
            return qty * avgCost;
        }

        public double getUnrealizedPnL() {
            return getTotalValue() - getTotalCost();
        }

        public double getUnrealizedPnLPercent() {
            return getTotalCost() > 0 ? (getUnrealizedPnL() / getTotalCost()) * 100 : 0;
        }
        }
    
    /**
     * Gets the count of unique players with holdings (for metrics).
     * @return Number of players with at least one holding
     */
    public int getPlayerCountWithHoldings() throws SQLException {
        String result = database.queryValue(
            "SELECT COUNT(DISTINCT player_uuid) FROM user_holdings WHERE qty > 0"
        );
        Bukkit.getConsoleSender().sendMessage("Result: " + result);
        return result != null ? Integer.parseInt(result) : 0;
    }
}