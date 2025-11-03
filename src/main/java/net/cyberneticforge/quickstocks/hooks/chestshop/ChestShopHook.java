package net.cyberneticforge.quickstocks.hooks.chestshop;

import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Integration hook for ChestShop plugin to handle company-owned shops.
 * This class provides utility methods for ChestShop integration.
 */
@SuppressWarnings("unused")
public class ChestShopHook {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final CompanyService companyService;
    
    public ChestShopHook(CompanyService companyService) {
        this.companyService = companyService;
    }
    
    /**
     * Checks if a player has permission to manage a chest shop for a company.
     * 
     * @param companyName The name of the company
     * @param player The player to check
     * @return true if the player has permission, false otherwise
     */
    public boolean canManageShop(String companyName, Player player) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isEmpty()) return false;
            
            Company company = companyOpt.get();
            String playerUuid = player.getUniqueId().toString();
            
            Optional<CompanyJob> jobOpt = companyService.getPlayerJob(company.getId(), playerUuid);
            return jobOpt.isPresent() && jobOpt.get().canManageChestShop();
        } catch (SQLException e) {
            logger.warning("Error checking ChestShop management permission", e);
            return false;
        }
    }
    
    /**
     * Gets a company by name if it exists.
     * 
     * @param companyName The name of the company
     * @return Optional containing the company if found
     */
    public Optional<Company> getCompany(String companyName) {
        try {
            return companyService.getCompanyByName(companyName);
        } catch (SQLException e) {
            logger.warning("Error getting company", e);
            return Optional.empty();
        }
    }
    
    /**
     * Adds funds to a company balance (for shop sales).
     * This method is intended to be used by ChestShop transaction events.
     * 
     * @param companyName The name of the company
     * @param amount The amount to add
     * @return true if successful, false otherwise
     */
    public boolean addFunds(String companyName, double amount) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isEmpty()) {
                return false;
            }
            
            Company company = companyOpt.get();
            // Directly add to company balance without wallet interaction
            companyService.addDirectToBalance(company.getId(), amount, "ChestShop sale");
            return true;
            
        } catch (Exception e) {
            logger.warning("Error adding funds to company", e);
            return false;
        }
    }
    
    /**
     * Removes funds from a company balance (for shop purchases).
     * This method is intended to be used by ChestShop transaction events.
     * 
     * @param companyName The name of the company
     * @param amount The amount to remove
     * @return true if successful, false otherwise
     */
    public boolean removeFunds(String companyName, double amount) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            if (companyOpt.isEmpty()) {
                return false;
            }
            
            Company company = companyOpt.get();
            
            // Check if company has sufficient balance
            if (company.getBalance() < amount) {
                return false;
            }
            
            // Directly remove from company balance without wallet interaction
            companyService.removeDirectFromBalance(company.getId(), amount, "ChestShop purchase");
            return true;
            
        } catch (Exception e) {
            logger.warning("Error removing funds from company", e);
            return false;
        }
    }
    
    /**
     * Gets the balance of a company.
     * 
     * @param companyName The name of the company
     * @return The company balance, or 0 if not found
     */
    public double getBalance(String companyName) {
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(companyName);
            return companyOpt.map(Company::getBalance).orElse(0.0);
        } catch (SQLException e) {
            logger.warning("Error getting company balance", e);
            return 0.0;
        }
    }

    /**
     * Gets the account of an uuid
     *
     * @param uuid The uuid of the account
     * @return The account instance, or null if not found
     */
    public Company getCompanyByAccountId(UUID uuid) {
        Account account = NameManager.getAccount(uuid);
        if(account == null) return null;
        return getCompany(account.getName()).orElse(null);
    }
}
