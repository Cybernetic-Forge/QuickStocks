package net.cyberneticforge.quickstocks.listeners;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.hooks.ChestShopHook;
import net.cyberneticforge.quickstocks.infrastructure.hooks.HookType;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for ChestShop transaction events to handle company-owned shop transactions.
 * This listener handles the money flow for chest shops owned by companies.
 */
public class ChestShopTransactionListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(ChestShopTransactionListener.class.getName());
    
    private final QuickStocksPlugin plugin;
    private final ChestShopHook chestShopHook;
    private final CompanyConfig companyConfig;
    
    public ChestShopTransactionListener(QuickStocksPlugin plugin, ChestShopHook chestShopHook, CompanyConfig companyConfig) {
        this.plugin = plugin;
        this.chestShopHook = chestShopHook;
        this.companyConfig = companyConfig;
    }
    
    /**
     * Handles pre-transaction validation for company-owned shops.
     * Checks if company has sufficient balance before allowing the transaction.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPreTransaction(PreTransactionEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!plugin.getHookManager().isHooked(HookType.ChestShop)) {
            return;
        }
        
        if (!companyConfig.isChestShopEnabled()) {
            return;
        }
        
        try {
            String ownerName = event.getOwnerAccount().getName();
            Optional<Company> companyOpt = chestShopHook.getCompany(ownerName);
            
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                
                // For SELL transactions (customer selling to shop), check if company has funds
                if (event.getTransactionType() == PreTransactionEvent.TransactionType.SELL) {
                    double price = event.getExactPrice().doubleValue();
                    
                    // Check if company has sufficient balance
                    if (company.getBalance() < companyConfig.getChestShopCompanyMinBalance()) {
                        event.setCancelled(PreTransactionEvent.TransactionOutcome.SHOP_DOES_NOT_HAVE_ENOUGH_MONEY);
                        event.getClient().sendMessage(ChatColor.RED + "Company shop does not have sufficient balance.");
                        logger.fine("Transaction cancelled: Company '" + ownerName + "' has insufficient balance");
                        return;
                    }
                    
                    if (company.getBalance() < price) {
                        event.setCancelled(PreTransactionEvent.TransactionOutcome.SHOP_DOES_NOT_HAVE_ENOUGH_MONEY);
                        event.getClient().sendMessage(ChatColor.RED + "Company shop does not have enough money for this transaction.");
                        logger.fine("Transaction cancelled: Company '" + ownerName + "' cannot afford $" + price);
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling ChestShop pre-transaction", e);
        }
    }
    
    /**
     * Handles completed transactions for company-owned shops.
     * Updates company balance based on the transaction type.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransaction(TransactionEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!plugin.getHookManager().isHooked(HookType.ChestShop)) {
            return;
        }
        
        if (!companyConfig.isChestShopEnabled()) {
            return;
        }
        
        try {
            String ownerName = event.getOwnerAccount().getName();
            Optional<Company> companyOpt = chestShopHook.getCompany(ownerName);
            
            if (companyOpt.isPresent()) {
                double price = event.getExactPrice().doubleValue();
                
                if (event.getTransactionType() == TransactionEvent.TransactionType.BUY) {
                    // Customer is buying from shop, company receives money
                    if (chestShopHook.addFunds(ownerName, price)) {
                        logger.fine("Added $" + price + " to company '" + ownerName + "' from shop sale");
                    } else {
                        logger.warning("Failed to add funds to company '" + ownerName + "'");
                    }
                } else if (event.getTransactionType() == TransactionEvent.TransactionType.SELL) {
                    // Customer is selling to shop, company pays money
                    if (chestShopHook.removeFunds(ownerName, price)) {
                        logger.fine("Removed $" + price + " from company '" + ownerName + "' for shop purchase");
                    } else {
                        logger.warning("Failed to remove funds from company '" + ownerName + "'");
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling ChestShop transaction", e);
        }
    }
}
