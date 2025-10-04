package net.cyberneticforge.quickstocks.listeners.shops;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.AccountQueryEvent;
import com.Acrobot.ChestShop.Events.Economy.AccountCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyConfig;
import net.cyberneticforge.quickstocks.infrastructure.hooks.ChestShopHook;
import net.cyberneticforge.quickstocks.infrastructure.hooks.HookType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for ChestShop transaction events to handle company-owned shop transactions.
 * This listener handles the money flow for chest shops owned by companies.
 */
public class ChestShopTransactionListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(ChestShopTransactionListener.class.getName());

    private final ChestShopHook chestShopHook;
    private final CompanyConfig companyConfig;
    
    public ChestShopTransactionListener(ChestShopHook chestShopHook, CompanyConfig companyConfig) {
        this.chestShopHook = chestShopHook;
        this.companyConfig = companyConfig;
    }

    /**
     * Handles account queries to re-establish company accounts in ChestShop.
     * This allows ChestShop to recognize company names as valid shop owners.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAccountCheck(AccountCheckEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;

        try {
            Company company = chestShopHook.getCompanyByAccountId(event.getAccount());
            if (company != null) {
                event.hasAccount(true);
                Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Reestablished account for company '" + company.getName() + "'");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling ChestShop account query", e);
        }
    }

    /**
     * Handles currency checks for company-owned shops.
     * Validates if the company has enough balance for the transaction.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCurrencyCheck(CurrencyCheckEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;

        try {
            Company company = chestShopHook.getCompanyByAccountId(event.getAccount());
            if (company != null) {
                double balance = company.getBalance();
                if( balance < companyConfig.getChestShopCompanyMinBalance() || balance < event.getAmount().doubleValue()) {
                    event.hasEnough(false);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Company shop '" + company.getName() + "' has insufficient balance: $" + balance);
                    return;
                }
                event.hasEnough(true);
                Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Validated currency for company shop '" + company.getName() + "'");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling ChestShop currency check", e);
        }
    }

    /**
     * Handles completed transactions for company-owned shops.
     * Updates company balance based on the transaction type.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTransaction(TransactionEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!companyConfig.isChestShopEnabled()) return;
        
        try {
            String ownerName = event.getOwnerAccount().getName();
            Optional<Company> companyOpt = chestShopHook.getCompany(ownerName);

            Bukkit.getConsoleSender().sendMessage("Checking transaction for shop owned by: " + ownerName + " (Type: " + event.getTransactionType() + ")");
            if (companyOpt.isPresent()) {
                double price = event.getExactPrice().doubleValue();

                Bukkit.getConsoleSender().sendMessage("Stock transaction size:'" + Arrays.toString(event.getStock()) + "' amount: $" + price);
                if (event.getTransactionType() == TransactionEvent.TransactionType.BUY) {
                    // Customer is buying from shop, company receives money
                    if (chestShopHook.addFunds(ownerName, price)) {
                        Bukkit.getConsoleSender().sendMessage("Added $" + price + " to company '" + ownerName + "' from shop sale");
                        event.setCancelled(false);
                    } else {
                        Bukkit.getConsoleSender().sendMessage("Failed to add funds to company '" + ownerName + "'");
                    }
                } else if (event.getTransactionType() == TransactionEvent.TransactionType.SELL) {
                    // Customer is selling to shop, company pays money
                    if (chestShopHook.removeFunds(ownerName, price)) {
                        event.setCancelled(false);
                        Bukkit.getConsoleSender().sendMessage("Removed $" + price + " from company '" + ownerName + "' for shop purchase");
                    } else {
                        Bukkit.getConsoleSender().sendMessage("Failed to remove funds from company '" + ownerName + "'");
                    }
                }
                event.setCancelled(false);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling ChestShop transaction", e);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void test(CurrencyTransferEvent event) {
        Bukkit.getConsoleSender().sendMessage("Test");
    }
}
