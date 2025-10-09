package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.TradingService;

import java.sql.SQLException;
import java.util.List;

/**
 * API Manager for trading operations.
 * Provides a high-level interface for external plugins to interact with the trading system.
 */
@SuppressWarnings("unused")
public class TradingManager {
    
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    
    public TradingManager(TradingService tradingService, HoldingsService holdingsService) {
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
    }
    
    /**
     * Executes a buy order for an instrument.
     * 
     * @param playerUuid UUID of the player buying
     * @param instrumentId ID of the instrument to buy
     * @param quantity Quantity to buy
     * @return true if successful, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean buy(String playerUuid, String instrumentId, int quantity) throws SQLException {
        return tradingService.executeBuyOrder(playerUuid, instrumentId, quantity).success();
    }
    
    /**
     * Executes a sell order for an instrument.
     * 
     * @param playerUuid UUID of the player selling
     * @param instrumentId ID of the instrument to sell
     * @param quantity Quantity to sell
     * @return true if successful, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean sell(String playerUuid, String instrumentId, int quantity) throws SQLException {
        return tradingService.executeSellOrder(playerUuid, instrumentId, quantity).success();
    }
    
    /**
     * Gets a player's holdings for a specific instrument.
     * 
     * @param playerUuid UUID of the player
     * @param instrumentId ID of the instrument
     * @return Holdings data or null if not found
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
     * Gets trade history for a player.
     * 
     * @param playerUuid UUID of the player
     * @param limit Maximum number of trades to return
     * @return List of trade history
     * @throws SQLException if database error occurs
     */
    public List<TradingService.Order> getTradeHistory(String playerUuid, int limit) throws SQLException {
        return tradingService.getOrderHistory(playerUuid, limit);
    }
}
