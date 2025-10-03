package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.hooks.HookType;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for ChestShop integration to allow companies to own chest shops.
 * This listener only activates if ChestShop plugin is detected.
 */
public class ChestShopListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(ChestShopListener.class.getName());
    
    private final QuickStocksPlugin plugin;
    private final CompanyService companyService;
    private final CompanyConfig companyConfig;
    
    public ChestShopListener(QuickStocksPlugin plugin, CompanyService companyService, CompanyConfig companyConfig) {
        this.plugin = plugin;
        this.companyService = companyService;
        this.companyConfig = companyConfig;
    }
    
    /**
     * Handles sign placement to validate company ownership of chest shops.
     * ChestShop signs have the format:
     * Line 0: Player name or company name
     * Line 1: Quantity
     * Line 2: Price (B price:S price)
     * Line 3: Item name
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!plugin.getHookManager().isHooked(HookType.ChestShop)) {
            return;
        }
        
        if (!companyConfig.isChestShopEnabled()) {
            return;
        }
        
        // Get the first line which should be the owner name
        String line0 = event.getLine(0);
        if (line0 == null || line0.trim().isEmpty()) {
            return;
        }
        
        // Check if line 0 is a company name (not a player name)
        // This is done by trying to find a company with that name
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(line0.trim());
            
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                
                // Check if company has sufficient balance
                if (company.getBalance() < companyConfig.getChestShopCompanyMinBalance()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Company '" + company.getName() + 
                        "' does not have sufficient balance to create chest shops.");
                    event.getPlayer().sendMessage(ChatColor.GRAY + "Required: $" + 
                        String.format("%.2f", companyConfig.getChestShopCompanyMinBalance()));
                    event.getPlayer().sendMessage(ChatColor.GRAY + "Current: $" + 
                        String.format("%.2f", company.getBalance()));
                    event.setCancelled(true);
                    return;
                }
                
                // Check if player is an employee with chestshop permission
                String playerUuid = event.getPlayer().getUniqueId().toString();
                var jobOpt = companyService.getPlayerJob(company.getId(), playerUuid);
                
                if (jobOpt.isEmpty() || !jobOpt.get().canManageChestShop()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to create chest shops for company '" + 
                        company.getName() + "'");
                    event.getPlayer().sendMessage(ChatColor.GRAY + "Required permission: chestshop");
                    event.setCancelled(true);
                    return;
                }
                
                // Allow the sign creation and log it
                logger.info("ChestShop sign created for company '" + company.getName() + 
                    "' by player " + event.getPlayer().getName());
                event.getPlayer().sendMessage(ChatColor.GREEN + "ChestShop created for company '" + 
                    company.getName() + "'");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error validating company for ChestShop sign", e);
            event.getPlayer().sendMessage(ChatColor.RED + "An error occurred while validating the company.");
            event.setCancelled(true);
        }
    }
}
