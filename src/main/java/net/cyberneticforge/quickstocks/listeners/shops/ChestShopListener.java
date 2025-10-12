package net.cyberneticforge.quickstocks.listeners.shops;

import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.hooks.ChestShopAccountProvider;
import net.cyberneticforge.quickstocks.hooks.HookType;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Level;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Listener for ChestShop integration to allow companies to own chest shops.
 * This listener only activates if ChestShop plugin is detected.
 */
public class ChestShopListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final CompanyService companyService = QuickStocksPlugin.getCompanyService();
    private final CompanyCfg companyConfig = QuickStocksPlugin.getCompanyService().getConfig();
    private final ChestShopAccountProvider accountProvider;
    
    public ChestShopListener(ChestShopAccountProvider accountProvider) {
        this.accountProvider = accountProvider;
    }
    
    /**
     * Handles sign placement to validate company ownership of chest shops.
     * Uses LOWEST priority to run before ChestShop's validation.
     * ChestShop signs have the format:
     * Line 0: Player name or company name
     * Line 1: Quantity
     * Line 2: Price (B price:S price)
     * Line 3: Item name
     */

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onSignChange(SignChangeEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;
        
        // Get the first line which should be the owner name
        String line0 = event.getLine(0);
        if (line0 == null || line0.trim().isEmpty()) return;
        
        // Check if line 0 is a company name (not a player name)
        // This is done by trying to find a company with that name
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(line0.trim());
            
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                
                // Check if company has sufficient balance
                if (company.getBalance() < companyConfig.getChestShopCompanyMinBalance()) {
                    Translation.ChestShop_InsufficientBalance.sendMessage(event.getPlayer(),
                        new Replaceable("%company%", company.getName()),
                        new Replaceable("%required%", String.format("%.2f", companyConfig.getChestShopCompanyMinBalance())),
                        new Replaceable("%current%", String.format("%.2f", company.getBalance())));
                    event.setCancelled(true);
                    return;
                }
                
                // Check if player is an employee with chestshop permission
                String playerUuid = event.getPlayer().getUniqueId().toString();
                var jobOpt = companyService.getPlayerJob(company.getId(), playerUuid);
                
                if (jobOpt.isEmpty() || !jobOpt.get().canManageChestShop()) {
                    Translation.ChestShop_NoPermission.sendMessage(event.getPlayer(),
                        new Replaceable("%company%", company.getName()));
                    event.setCancelled(true);
                    return;
                }
                
                // Company validation passed - Register company with ChestShop
                accountProvider.registerCompany(company);
                logger.info("Company '" + company.getName() + "' validated and registered for ChestShop by player " + event.getPlayer().getName());
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error validating company for ChestShop sign", e);
            Translation.ChestShop_Error.sendMessage(event.getPlayer());
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles successful shop creation to provide feedback to the player.
     * This event fires after ChestShop has validated and created the shop.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("deprecation")
    public void onShopCreated(ShopCreatedEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;
        
        // Check if this is a company shop
        String ownerName = event.getSign().getLine(0);
        if (ownerName.trim().isEmpty()) return;
        
        try {
            Optional<Company> companyOpt = companyService.getCompanyByName(ownerName.trim());
            if (companyOpt.isPresent()) {
                // Ensure company is registered (in case registration didn't happen during sign creation)
                accountProvider.registerCompany(companyOpt.get());
                
                Translation.ChestShop_Created.sendMessage(event.getPlayer(),
                    new Replaceable("%company%", companyOpt.get().getName()));
                logger.info("ChestShop successfully created for company '" + companyOpt.get().getName() + 
                    "' by player " + event.getPlayer().getName());
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error checking company on shop creation", e);
        }
    }
}
