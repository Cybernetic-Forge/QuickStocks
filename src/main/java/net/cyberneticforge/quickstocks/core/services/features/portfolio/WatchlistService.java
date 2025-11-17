package net.cyberneticforge.quickstocks.core.services.features.portfolio;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.api.events.WatchlistAddEvent;
import net.cyberneticforge.quickstocks.api.events.WatchlistRemoveEvent;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseManager;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * Service for managing player watchlists.
 * Handles adding/removing instruments from watchlists and retrieving watchlist data.
 */
public class WatchlistService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final DatabaseManager databaseManager = QuickStocksPlugin.getDatabaseManager();
    
    /**
     * Adds an instrument to a player's watchlist.
     * 
     * @param playerUuid The player's UUID
     * @param instrumentId The instrument ID to add
     * @return true if added successfully, false if already exists
     * @throws SQLException if database error occurs
     */
    public boolean addToWatchlist(String playerUuid, String instrumentId) throws SQLException {
        if (isInWatchlist(playerUuid, instrumentId)) {
            return false; // Already in watchlist
        }
        
        // Get instrument symbol for event
        String symbol = getInstrumentSymbol(instrumentId);
        
        // Fire cancellable event before adding to watchlist
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
            if (player != null) {
                WatchlistAddEvent event =
                    new WatchlistAddEvent(
                        player, instrumentId, symbol != null ? symbol : instrumentId
                    );
                Bukkit.getPluginManager().callEvent(event);
                
                if (event.isCancelled()) {
                    logger.debug("WatchlistAddEvent cancelled for player " + playerUuid);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not fire WatchlistAddEvent: " + e.getMessage());
        }
        
        String sql = "INSERT INTO user_watchlists (player_uuid, instrument_id, added_at) VALUES (?, ?, ?)";
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            stmt.setString(2, instrumentId);
            stmt.setLong(3, System.currentTimeMillis());
            
            int rows = stmt.executeUpdate();
            logger.info("Added instrument " + instrumentId + " to watchlist for player " + playerUuid);
            return rows > 0;
        }
    }
    
    /**
     * Removes an instrument from a player's watchlist.
     * 
     * @param playerUuid The player's UUID
     * @param instrumentId The instrument ID to remove
     * @return true if removed successfully, false if not found
     * @throws SQLException if database error occurs
     */
    public boolean removeFromWatchlist(String playerUuid, String instrumentId) throws SQLException {
        // Get instrument symbol for event
        String symbol = getInstrumentSymbol(instrumentId);
        
        // Fire cancellable event before removing from watchlist
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
            if (player != null) {
                WatchlistRemoveEvent event =
                    new WatchlistRemoveEvent(
                        player, instrumentId, symbol != null ? symbol : instrumentId
                    );
                Bukkit.getPluginManager().callEvent(event);
                
                if (event.isCancelled()) {
                    logger.debug("WatchlistRemoveEvent cancelled for player " + playerUuid);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not fire WatchlistRemoveEvent: " + e.getMessage());
        }
        
        String sql = "DELETE FROM user_watchlists WHERE player_uuid = ? AND instrument_id = ?";
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            stmt.setString(2, instrumentId);
            
            int rows = stmt.executeUpdate();
            logger.info("Removed instrument " + instrumentId + " from watchlist for player " + playerUuid);
            return rows > 0;
        }
    }
    
    /**
     * Helper method to get instrument symbol.
     */
    private String getInstrumentSymbol(String instrumentId) {
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT symbol FROM instruments WHERE id = ?")) {
            stmt.setString(1, instrumentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("symbol") : null;
            }
        } catch (Exception e) {
            logger.debug("Could not get instrument symbol: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Checks if an instrument is in a player's watchlist.
     * 
     * @param playerUuid The player's UUID
     * @param instrumentId The instrument ID to check
     * @return true if in watchlist, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean isInWatchlist(String playerUuid, String instrumentId) throws SQLException {
        String sql = "SELECT 1 FROM user_watchlists WHERE player_uuid = ? AND instrument_id = ?";
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            stmt.setString(2, instrumentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Gets all instruments in a player's watchlist with their current data.
     * 
     * @param playerUuid The player's UUID
     * @return List of watchlist items with instrument and state data
     * @throws SQLException if database error occurs
     */
    public List<WatchlistItem> getWatchlist(String playerUuid) throws SQLException {
        String sql = """
            SELECT\s
                w.instrument_id,
                w.added_at,
                i.symbol,
                i.display_name,
                i.type,
                s.last_price,
                s.change_24h,
                s.change_1h,
                s.volatility_24h
            FROM user_watchlists w
            JOIN instruments i ON w.instrument_id = i.id
            LEFT JOIN instrument_state s ON w.instrument_id = s.instrument_id
            WHERE w.player_uuid = ?
            ORDER BY w.added_at DESC
           \s""";
        
        List<WatchlistItem> items = new ArrayList<>();
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WatchlistItem item = new WatchlistItem(
                        rs.getString("instrument_id"),
                        rs.getString("symbol"),
                        rs.getString("display_name"),
                        rs.getString("type"),
                        rs.getLong("added_at"),
                        rs.getDouble("last_price"),
                        rs.getDouble("change_24h"),
                        rs.getDouble("change_1h"),
                        rs.getDouble("volatility_24h")
                    );
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    /**
     * Gets the count of items in a player's watchlist.
     * 
     * @param playerUuid The player's UUID
     * @return Number of items in watchlist
     * @throws SQLException if database error occurs
     */
    public int getWatchlistCount(String playerUuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_watchlists WHERE player_uuid = ?";
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    
    /**
     * Clears all items from a player's watchlist.
     * 
     * @param playerUuid The player's UUID
     * @return Number of items removed
     * @throws SQLException if database error occurs
     */
    public int clearWatchlist(String playerUuid) throws SQLException {
        String sql = "DELETE FROM user_watchlists WHERE player_uuid = ?";
        
        try (Connection conn = databaseManager.getDb().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUuid);
            int rows = stmt.executeUpdate();
            logger.info("Cleared watchlist for player " + playerUuid + " (" + rows + " items removed)");
            return rows;
        }
    }

    /**
     * Represents an item in a player's watchlist with current market data.
     *
     * @param instrumentId Getters
     */
        public record WatchlistItem(String instrumentId, String symbol, String displayName, String type, long addedAt,
                                    double lastPrice, double change24h, double change1h, double volatility24h) {
    }
}