package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Service for managing player wallet balances.
 * Provides basic economy functionality with Vault integration placeholder.
 */
public class WalletService {
    
    private final Db database;
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    public WalletService(Db database) {
        this.database = database;
    }
    
    /**
     * Gets a player's current balance.
     * Falls back to internal wallet if Vault is not available.
     */
    public double getBalance(String playerUuid) throws SQLException {
        // TODO: Check for Vault integration first
        return getInternalBalance(playerUuid);
    }
    
    /**
     * Sets a player's balance.
     */
    public void setBalance(String playerUuid, double amount) throws SQLException {
        // TODO: Check for Vault integration first
        setInternalBalance(playerUuid, Math.max(0, amount));
    }
    
    /**
     * Adds money to a player's balance.
     */
    public void addBalance(String playerUuid, double amount) throws SQLException {
        double currentBalance = getBalance(playerUuid);
        setBalance(playerUuid, currentBalance + amount);
    }
    
    /**
     * Removes money from a player's balance.
     * @return true if successful, false if insufficient funds
     */
    public boolean removeBalance(String playerUuid, double amount) throws SQLException {
        double currentBalance = getBalance(playerUuid);
        if (currentBalance >= amount) {
            setBalance(playerUuid, currentBalance - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a player has sufficient balance.
     */
    public boolean hasBalance(String playerUuid, double amount) throws SQLException {
        return getBalance(playerUuid) >= amount;
    }
    
    private double getInternalBalance(String playerUuid) throws SQLException {
        Double balance = database.queryValue(
            "SELECT balance FROM wallets WHERE player_uuid = ?", 
            playerUuid
        );
        return balance != null ? balance : 0.0;
    }
    
    private void setInternalBalance(String playerUuid, double amount) throws SQLException {
        database.execute(
            "INSERT OR REPLACE INTO wallets (player_uuid, balance) VALUES (?, ?)",
            playerUuid, amount
        );
        logger.fine("Set balance for " + playerUuid + " to $" + String.format("%.2f", amount));
    }
    
    // Placeholder methods for Vault integration (future implementation)
    private double getVaultBalance(String playerUuid) {
        // TODO: Integrate with Vault API
        return 0.0;
    }
    
    private void setVaultBalance(String playerUuid, double amount) {
        // TODO: Integrate with Vault API
    }
}