package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.HoldingsService;

import java.sql.SQLException;
import java.util.List;

/**
 * API Manager for portfolio and holdings operations.
 * Provides a high-level interface for external plugins to interact with player portfolios.
 */
public class PortfolioManager {
    
    private final HoldingsService holdingsService;
    
    public PortfolioManager(HoldingsService holdingsService) {
        this.holdingsService = holdingsService;
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
}
