package net.cyberneticforge.quickstocks.listeners.shops;

import com.Acrobot.ChestShop.Events.Economy.AccountCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.hooks.chestshop.ChestShopHook;
import net.cyberneticforge.quickstocks.hooks.HookType;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for ChestShop transaction events to handle company-owned shop transactions.
 * This listener handles the money flow for chest shops owned by companies.
 */
public class ChestShopTransactionListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final ChestShopHook chestShopHook;
    
    public ChestShopTransactionListener(ChestShopHook chestShopHook) {
        this.chestShopHook = chestShopHook;
    }

    /**
     * Handles account queries to re-establish company accounts in ChestShop.
     * This allows ChestShop to recognize company names as valid shop owners.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAccountCheck(AccountCheckEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!QuickStocksPlugin.getCompanyService().getConfig().isChestShopEnabled()) return;

        try {
            Company company = chestShopHook.getCompanyByAccountId(event.getAccount());
            if (company != null) {
                event.hasAccount(true);
            }
        } catch (Exception e) {
            logger.severe("Error handling ChestShop account query", e);
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
        if (!QuickStocksPlugin.getCompanyService().getConfig().isChestShopEnabled()) return;

        try {
            Company company = chestShopHook.getCompanyByAccountId(event.getAccount());
            if (company != null) {
                double balance = company.getBalance();
                if( balance < QuickStocksPlugin.getCompanyService().getConfig().getChestShopCompanyMinBalance() || balance < event.getAmount().doubleValue()) {
                    event.hasEnough(false);
                    return;
                }
                event.hasEnough(true);
            }
        } catch (Exception e) {
            logger.severe("Error handling ChestShop currency check", e);
        }
    }

    /**
     * Handles currency transfers for company-owned shops.
     * This intercepts ChestShop's money transfers and handles company balance updates.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCurrencyTransfer(CurrencyTransferEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!QuickStocksPlugin.getCompanyService().getConfig().isChestShopEnabled()) return;
        
        try {
            UUID receiverUUID = event.getReceiver();
            UUID senderUUID = event.getSender();
            BigDecimal amount = event.getAmountReceived();
            
            // Check if receiver is a company (shop owner receiving money from customer)
            Company receiverCompany = chestShopHook.getCompanyByAccountId(receiverUUID);
            // Check if sender is a company (shop owner paying customer)
            Company senderCompany = chestShopHook.getCompanyByAccountId(senderUUID);
            
            if (receiverCompany != null) {
                // Company is receiving money (customer buying from shop)
                logger.info("Company '" + receiverCompany.getName() + "' receiving $" + amount + " from shop sale");
                
                // Add money to company balance
                if (chestShopHook.addFunds(receiverCompany.getName(), amount.doubleValue())) {
                    logger.info("Successfully added $" + amount + " to company '" + receiverCompany.getName() + "'");
                } else {
                    logger.warning("Failed to add funds to company '" + receiverCompany.getName() + "'");
                }
                
                // Mark as handled so ChestShop doesn't try to transfer to a player account
                event.setHandled(true);
                
            } else if (senderCompany != null) {
                // Company is paying money (customer selling to shop)
                logger.info("Company '" + senderCompany.getName() + "' paying $" + amount + " for shop purchase");
                
                // Remove money from company balance
                if (chestShopHook.removeFunds(senderCompany.getName(), amount.doubleValue())) {
                    logger.info("Successfully removed $" + amount + " from company '" + senderCompany.getName() + "'");
                } else {
                    logger.warning("Failed to remove funds from company '" + senderCompany.getName() + "'");
                    event.setHandled(true);
                    return;
                }
                
                // Mark as handled so ChestShop doesn't try to transfer from a player account
                event.setHandled(true);
            }
            
            // Handle player side of the transaction
            // If receiver is a player (customer selling to shop), add money
            Player receiverPlayer = Bukkit.getPlayer(receiverUUID);
            if (receiverPlayer != null && senderCompany != null) {
                try {
                    String playerUuid = receiverPlayer.getUniqueId().toString();
                    QuickStocksPlugin.getWalletService().addBalance(playerUuid, amount.doubleValue());
                } catch (Exception e) {
                    logger.severe("Error adding balance to player", e);
                    event.setHandled(true);
                }
            }
            
            // If sender is a player (customer buying from shop), remove money
            Player senderPlayer = Bukkit.getPlayer(senderUUID);
            if (senderPlayer != null && receiverCompany != null) {
                try {
                    String playerUuid = senderPlayer.getUniqueId().toString();
                    if (QuickStocksPlugin.getWalletService().removeBalance(playerUuid, amount.doubleValue())) {
                        logger.info("Removed $" + amount + " from player " + senderPlayer.getName());
                    } else {
                        event.setHandled(true);
                        logger.warning("Player " + senderPlayer.getName() + " has insufficient funds");
                    }
                } catch (Exception e) {
                    logger.severe("Error removing balance from player", e);
                    event.setHandled(true);
                }
            }
        } catch (Exception e) {
            logger.severe("Error handling ChestShop currency transfer", e);
            event.setHandled(true);
        }
    }
    
    /**
     * Handles completed transactions for company-owned shops.
     * Sends appropriate messages to players.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransaction(TransactionEvent event) {
        // Only process if ChestShop is hooked and enabled
        if (!QuickStocksPlugin.getHookManager().isHooked(HookType.ChestShop)) return;
        if (!QuickStocksPlugin.getCompanyService().getConfig().isChestShopEnabled()) return;
        
        try {
            String ownerName = event.getOwnerAccount().getName();
            Optional<Company> companyOpt = chestShopHook.getCompany(ownerName);
            
            if (companyOpt.isPresent()) {
                Company company = companyOpt.get();
                Player client = event.getClient();
                String itemName = event.getStock()[0].getType().toString().toLowerCase().replace("_", " ");
                int quantity = event.getStock()[0].getAmount();
                double price = event.getExactPrice().doubleValue();
                
                // Send appropriate message based on transaction type
                if (event.getTransactionType() == TransactionEvent.TransactionType.BUY) {
                    logger.info("Transaction: " + (client != null ? client.getName() : "Unknown") + 
                               " bought " + quantity + " " + itemName + " for $" + price + 
                               " from company " + company.getName());
                    
                } else if (event.getTransactionType() == TransactionEvent.TransactionType.SELL) {
                    // Customer sold to shop
                    logger.info("Transaction: " + (client != null ? client.getName() : "Unknown") + 
                               " sold " + quantity + " " + itemName + " for $" + price + 
                               " to company " + company.getName());
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error handling ChestShop transaction message", e);
        }
    }
}
