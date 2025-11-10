package net.cyberneticforge.quickstocks.core.services.features.portfolio;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Manages player wallet balances with Vault economy integration fallback.
 * If Vault is available, uses it; otherwise uses internal wallet system.
 */
@SuppressWarnings({"JavaReflectionInvocation", "unused"})
public class WalletService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final boolean useVault;
    private Object vaultEconomy; // Using Object to avoid compile-time dependency on Vault
    
    public WalletService() {
        this.useVault = setupVaultEconomy();
        
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
    private boolean setupVaultEconomy() {
        try {
            // Use reflection to check for Bukkit and Vault
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object server = bukkitClass.getMethod("getServer").invoke(null);
            Object pluginManager = server.getClass().getMethod("getPluginManager").invoke(server);
            Object vaultPlugin = pluginManager.getClass().getMethod("getPlugin", String.class).invoke(pluginManager, "Vault");
            
            if (vaultPlugin == null) {
                logger.info("Vault plugin not found, using internal wallet system");
                return false;
            }
            
            // Get the services manager and economy service
            Object servicesManager = server.getClass().getMethod("getServicesManager").invoke(server);
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = servicesManager.getClass().getMethod("getRegistration", Class.class).invoke(servicesManager, economyClass);
            
            if (registration == null) {
                logger.warning("Vault found but no economy provider registered, using internal wallet system");
                return false;
            }
            
            vaultEconomy = registration.getClass().getMethod("getProvider").invoke(registration);
            String economyName = (String) vaultEconomy.getClass().getMethod("getName").invoke(vaultEconomy);
            logger.info("Vault economy provider found: " + economyName);
            return true;
            
        } catch (Exception e) {
            // Bukkit/Vault not available - this is normal in non-Bukkit environments like tests
            logger.debug("Bukkit/Vault not available: " + e.getMessage() + ". Using internal wallet system.");
            return false;
        }
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
        if (useVault) {
            addVaultBalance(playerUuid, amount);
        } else {
            double currentBalance = getBalance(playerUuid);
            setBalance(playerUuid, currentBalance + amount);
        }
    }
    
    /**
     * Removes money from a player's balance.
     * @return true if successful, false if insufficient funds
     */
    public boolean removeBalance(String playerUuid, double amount) throws SQLException {
        if (useVault) {
            return removeVaultBalance(playerUuid, amount);
        } else {
            double currentBalance = getBalance(playerUuid);
            if (currentBalance >= amount) {
                setBalance(playerUuid, currentBalance - amount);
                return true;
            }
            return false;
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
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object offlinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class)
                    .invoke(null, UUID.fromString(playerUuid));
            
            double balance = (Double) vaultEconomy.getClass().getMethod("getBalance", 
                    Class.forName("org.bukkit.OfflinePlayer"))
                    .invoke(vaultEconomy, offlinePlayer);
            
            logger.debug("Retrieved Vault balance for " + playerUuid + ": $" + String.format("%.2f", balance));
            return balance;
        } catch (Exception e) {
            logger.warning("Failed to get Vault balance for " + playerUuid + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    private void setVaultBalance(String playerUuid, double amount) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object offlinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class)
                    .invoke(null, UUID.fromString(playerUuid));
            
            // Get current balance first
            double currentBalance = (Double) vaultEconomy.getClass().getMethod("getBalance", 
                    Class.forName("org.bukkit.OfflinePlayer"))
                    .invoke(vaultEconomy, offlinePlayer);
            
            if (amount > currentBalance) {
                // Need to deposit money
                vaultEconomy.getClass().getMethod("depositPlayer", 
                        Class.forName("org.bukkit.OfflinePlayer"), double.class)
                        .invoke(vaultEconomy, offlinePlayer, amount - currentBalance);
            } else if (amount < currentBalance) {
                // Need to withdraw money
                vaultEconomy.getClass().getMethod("withdrawPlayer", 
                        Class.forName("org.bukkit.OfflinePlayer"), double.class)
                        .invoke(vaultEconomy, offlinePlayer, currentBalance - amount);
            }
            
            logger.debug("Set Vault balance for " + playerUuid + " to $" + String.format("%.2f", amount));
        } catch (Exception e) {
            logger.warning("Failed to set Vault balance for " + playerUuid + ": " + e.getMessage());
        }
    }
    
    private void addVaultBalance(String playerUuid, double amount) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object offlinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class)
                    .invoke(null, UUID.fromString(playerUuid));
            
            vaultEconomy.getClass().getMethod("depositPlayer", 
                    Class.forName("org.bukkit.OfflinePlayer"), double.class)
                    .invoke(vaultEconomy, offlinePlayer, amount);
            
            logger.debug("Added $" + String.format("%.2f", amount) + " to Vault balance for " + playerUuid);
        } catch (Exception e) {
            logger.warning("Failed to add Vault balance for " + playerUuid + ": " + e.getMessage());
        }
    }
    
    private boolean removeVaultBalance(String playerUuid, double amount) {
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object offlinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class)
                    .invoke(null, UUID.fromString(playerUuid));
            
            // Check balance first
            double currentBalance = (Double) vaultEconomy.getClass().getMethod("getBalance", 
                    Class.forName("org.bukkit.OfflinePlayer"))
                    .invoke(vaultEconomy, offlinePlayer);
            
            if (currentBalance >= amount) {
                Object result = vaultEconomy.getClass().getMethod("withdrawPlayer", 
                        Class.forName("org.bukkit.OfflinePlayer"), double.class)
                        .invoke(vaultEconomy, offlinePlayer, amount);
                
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
    
    /**
     * Gets the name of the economy provider being used.
     */
    public String getEconomyProviderName() {
        if (useVault && vaultEconomy != null) {
            try {
                return (String) vaultEconomy.getClass().getMethod("getName").invoke(vaultEconomy);
            } catch (Exception e) {
                logger.warning("Failed to get economy provider name: " + e.getMessage());
                return "Vault (Unknown Provider)";
            }
        }
        return "Internal Wallet System";
    }
}