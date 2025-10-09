package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.WalletService;

import java.sql.SQLException;

/**
 * API Manager for wallet and balance operations.
 * Provides a high-level interface for external plugins to interact with the wallet system.
 */
@SuppressWarnings("unused")
public class WalletManager {
    
    private final WalletService walletService;
    
    public WalletManager(WalletService walletService) {
        this.walletService = walletService;
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
}
