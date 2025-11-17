package net.cyberneticforge.quickstocks.core.services.features.portfolio;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.api.events.WalletBalanceChangeEvent;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages player wallet balances with Vault economy integration fallback.
 * If Vault is available, uses it; otherwise uses internal wallet system.
 */
public class WalletService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final boolean useVault;
    private Economy vaultEconomy; // Using Object to avoid compile-time dependency on Vault
    
    public WalletService() {
        this.useVault = setupEconomy();
        
        if (useVault) {
            logger.info("WalletService initialized with Vault economy integration");
        } else {
            logger.info("WalletService initialized with internal wallet system (Vault not available)");
        }
    }
    
    /**
     * Attempts to set up Vault economy integration using reflection to avoid compile-time dependencies.
     * @return true if Vault is available and economy provider found, false otherwise
     */
    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        return true;
    }
    
    /**
     * Gets the balance for a player.
     */
    public double getBalance(String playerUuid) throws SQLException {
        if (useVault) {
            return getVaultBalance(playerUuid);
        } else {
            return getInternalBalance(playerUuid);
        }
    }
    
    /**
     * Sets the balance for a player.
     */
    public void setBalance(String playerUuid, double amount) throws SQLException {
        if (useVault) {
            setVaultBalance(playerUuid, amount);
        } else {
            setInternalBalance(playerUuid, amount);
        }
    }
    
    /**
     * Adds money to a player's balance.
     */
    public void addBalance(String playerUuid, double amount) throws SQLException {
        double oldBalance = getBalance(playerUuid);
        
        if (useVault) {
            addVaultBalance(playerUuid, amount);
        } else {
            double currentBalance = getBalance(playerUuid);
            setBalance(playerUuid, currentBalance + amount);
        }
        
        // Fire WalletBalanceChangeEvent after successful balance change
        fireBalanceChangeEvent(playerUuid, oldBalance, oldBalance + amount, 
            WalletBalanceChangeEvent.ChangeReason.OTHER);
    }
    
    /**
     * Removes money from a player's balance.
     * @return true if successful, false if insufficient funds
     */
    public boolean removeBalance(String playerUuid, double amount) throws SQLException {
        double oldBalance = getBalance(playerUuid);
        boolean success;
        
        if (useVault) {
            success = removeVaultBalance(playerUuid, amount);
        } else {
            double currentBalance = getBalance(playerUuid);
            if (currentBalance >= amount) {
                setBalance(playerUuid, currentBalance - amount);
                success = true;
            } else {
                success = false;
            }
        }
        
        // Fire WalletBalanceChangeEvent after successful balance change
        if (success) {
            fireBalanceChangeEvent(playerUuid, oldBalance, oldBalance - amount, WalletBalanceChangeEvent.ChangeReason.OTHER);
        }
        
        return success;
    }
    
    /**
     * Fires a WalletBalanceChangeEvent.
     */
    private void fireBalanceChangeEvent(String playerUuid, double oldBalance, double newBalance, WalletBalanceChangeEvent.ChangeReason reason) {
        try {
            Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
            if (player != null) {
                WalletBalanceChangeEvent event = new WalletBalanceChangeEvent(player, oldBalance, newBalance, reason);
                Bukkit.getPluginManager().callEvent(event);
            }
        } catch (Exception e) {
            logger.debug("Could not fire WalletBalanceChangeEvent: " + e.getMessage());
        }
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
        logger.debug("Set balance for " + playerUuid + " to $" + String.format("%.2f", amount));
    }
    
    // Vault integration methods using reflection to avoid compile-time dependencies
    private double getVaultBalance(String playerUuid) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            if(offlinePlayer.getName() == null) {
                logger.warning("OfflinePlayer not found for UUID: " + playerUuid);
                return 0.0;
            }
            var balance = vaultEconomy.getBalance(offlinePlayer);
            logger.debug("Retrieved Vault balance for " + playerUuid + ": $" + String.format("%.2f", balance));
            return balance;
        } catch (Exception e) {
            logger.warning("Failed to get Vault balance for " + playerUuid + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private void setVaultBalance(String playerUuid, double amount) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            if(offlinePlayer.getName() == null) {
                logger.warning("OfflinePlayer not found for UUID: " + playerUuid);
                return;
            }
            double currentBalance = vaultEconomy.getBalance(offlinePlayer);
            if (amount > currentBalance) {
                // Need to deposit money
                vaultEconomy.depositPlayer(offlinePlayer, amount - currentBalance);
            } else if (amount < currentBalance) {
                // Need to withdraw money
                vaultEconomy.withdrawPlayer(offlinePlayer, currentBalance - amount);
            }
            logger.debug("Set Vault balance for " + playerUuid + " to $" + String.format("%.2f", amount));
        } catch (Exception e) {
            logger.warning("Failed to set Vault balance for " + playerUuid + ": " + e.getMessage());
        }
    }
    
    private void addVaultBalance(String playerUuid, double amount) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            if(offlinePlayer.getName() == null) {
                logger.warning("OfflinePlayer not found for UUID: " + playerUuid);
                return;
            }
            vaultEconomy.depositPlayer(offlinePlayer, amount);
            logger.debug("Added $" + String.format("%.2f", amount) + " to Vault balance for " + playerUuid);
        } catch (Exception e) {
            logger.warning("Failed to add Vault balance for " + playerUuid + ": " + e.getMessage());
        }
    }
    
    private boolean removeVaultBalance(String playerUuid, double amount) {
        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));
            if(offlinePlayer.getName() == null) {
                logger.warning("OfflinePlayer not found for UUID: " + playerUuid);
                return false;
            }
            double currentBalance = vaultEconomy.getBalance(offlinePlayer);
            
            if (currentBalance >= amount) {
                vaultEconomy.withdrawPlayer(offlinePlayer, amount);
                logger.debug("Removed $" + String.format("%.2f", amount) + " from Vault balance for " + playerUuid);
                return true;
            }
            
            logger.debug("Insufficient Vault balance for " + playerUuid + " to remove $" + String.format("%.2f", amount));
            return false;
        } catch (Exception e) {
            logger.warning("Failed to remove Vault balance for " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if this service is using Vault for economy operations.
     */
    public boolean isUsingVault() {
        return useVault;
    }
}