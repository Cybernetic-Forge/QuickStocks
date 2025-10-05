package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.WatchlistService;

import java.sql.SQLException;
import java.util.List;

/**
 * API Manager for watchlist operations.
 * Provides a high-level interface for external plugins to interact with the watchlist system.
 */
public class WatchlistManager {
    
    private final WatchlistService watchlistService;
    
    public WatchlistManager(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }
    
    /**
     * Adds an instrument to a player's watchlist.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument to add
     * @return true if added successfully, false if already exists
     * @throws SQLException if database error occurs
     */
    public boolean addToWatchlist(String playerUuid, String instrumentId) throws SQLException {
        return watchlistService.addToWatchlist(playerUuid, instrumentId);
    }
    
    /**
     * Removes an instrument from a player's watchlist.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument to remove
     * @return true if removed successfully, false if not in watchlist
     * @throws SQLException if database error occurs
     */
    public boolean removeFromWatchlist(String playerUuid, String instrumentId) throws SQLException {
        return watchlistService.removeFromWatchlist(playerUuid, instrumentId);
    }
    
    /**
     * Gets all instruments in a player's watchlist.
     * 
     * @param playerUuid UUID of the player
     * @return List of watchlist entries
     * @throws SQLException if database error occurs
     */
    public List<WatchlistService.WatchlistItem> getWatchlist(String playerUuid) throws SQLException {
        return watchlistService.getWatchlist(playerUuid);
    }
    
    /**
     * Checks if an instrument is in a player's watchlist.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument
     * @return true if in watchlist, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean isInWatchlist(String playerUuid, String instrumentId) throws SQLException {
        return watchlistService.isInWatchlist(playerUuid, instrumentId);
    }
    
    /**
     * Clears all instruments from a player's watchlist.
     * 
     * @param playerUuid UUID of the player
     * @throws SQLException if database error occurs
     */
    public void clearWatchlist(String playerUuid) throws SQLException {
        watchlistService.clearWatchlist(playerUuid);
    }
}
