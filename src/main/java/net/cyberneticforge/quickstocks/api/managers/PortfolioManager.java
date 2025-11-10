package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.features.portfolio.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.features.portfolio.WalletService;
import net.cyberneticforge.quickstocks.core.services.features.portfolio.WatchlistService;

import java.sql.SQLException;
import java.util.List;

/**
 * API Manager for portfolio and holdings operations.
 * Provides a high-level interface for external plugins to interact with player portfolios.
 */
@SuppressWarnings("unused")
public class PortfolioManager {
    
    private final WalletService walletService;
    private final HoldingsService holdingsService;
    private final WatchlistService watchlistService;

    public PortfolioManager(WalletService walletService, HoldingsService holdingsService, WatchlistService watchlistService) {
        this.walletService = walletService;
        this.holdingsService = holdingsService;
        this.watchlistService = watchlistService;
    }

    /**
     * Gets a player's current balance.
     *
     * @param playerUuid UUID of the player
     * @return Current balance
     * @throws SQLException if database error occurs
     */
    public double getBalance(String playerUuid) throws SQLException {
        return walletService.getBalance(playerUuid);
    }

    /**
     * Adds funds to a player's wallet.
     *
     * @param playerUuid UUID of the player
     * @param amount Amount to add
     * @throws SQLException if database error occurs
     */
    public void addBalance(String playerUuid, double amount) throws SQLException {
        walletService.addBalance(playerUuid, amount);
    }

    /**
     * Subtracts funds from a player's wallet.
     *
     * @param playerUuid UUID of the player
     * @param amount Amount to subtract
     * @throws SQLException if database error occurs
     */
    public void subtractBalance(String playerUuid, double amount) throws SQLException {
        walletService.removeBalance(playerUuid, amount);
    }

    /**
     * Sets a player's balance to a specific amount.
     *
     * @param playerUuid UUID of the player
     * @param amount New balance amount
     * @throws SQLException if database error occurs
     */
    public void setBalance(String playerUuid, double amount) throws SQLException {
        walletService.setBalance(playerUuid, amount);
    }

    /**
     * Checks if a player has at least the specified amount.
     *
     * @param playerUuid UUID of the player
     * @param amount Amount to check
     * @return true if player has at least the amount, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean hasBalance(String playerUuid, double amount) throws SQLException {
        return walletService.hasBalance(playerUuid, amount);
    }

    /**
     * Gets a player's holding for a specific instrument.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument
     * @return Holding data or null if not found
     */
    public HoldingsService.Holding getHolding(String playerUuid, String instrumentId) {
        return holdingsService.getHolding(playerUuid, instrumentId);
    }
    
    /**
     * Gets all holdings for a player.
     * 
     * @param playerUuid UUID of the player
     * @return List of all holdings
     * @throws SQLException if database error occurs
     */
    public List<HoldingsService.Holding> getAllHoldings(String playerUuid) throws SQLException {
        return holdingsService.getHoldings(playerUuid);
    }
    
    /**
     * Gets the quantity of an instrument held by a player.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument
     * @return Quantity held
     */
    public double getHoldingQuantity(String playerUuid, String instrumentId) {
        return holdingsService.getHolding(playerUuid, instrumentId).qty();
    }
    
    /**
     * Gets the total portfolio value for a player.
     * 
     * @param playerUuid UUID of the player
     * @return Total portfolio value
     * @throws SQLException if database error occurs
     */
    public double getPortfolioValue(String playerUuid) throws SQLException {
        return holdingsService.getPortfolioValue(playerUuid);
    }
    
    /**
     * Gets profit/loss for a specific holding.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument
     * @return Profit/loss amount
     */
    public double getHoldingProfitLoss(String playerUuid, String instrumentId) {
        HoldingsService.Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
        if (holding == null) {
            return 0.0;
        }
        
        double currentValue = holding.currentPrice();
        double cost = holding.getTotalCost();
        return currentValue - cost;
    }
    
    /**
     * Gets total profit/loss for a player's entire portfolio.
     * 
     * @param playerUuid UUID of the player
     * @return Total profit/loss amount
     * @throws SQLException if database error occurs
     */
    public double getTotalProfitLoss(String playerUuid) throws SQLException {
        return holdingsService.getPortfolioValue(playerUuid);
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
