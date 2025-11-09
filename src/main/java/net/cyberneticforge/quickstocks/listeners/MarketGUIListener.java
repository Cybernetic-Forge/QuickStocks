package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.gui.MarketGUI;
import net.cyberneticforge.quickstocks.gui.PortfolioGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Handles interactions with the Market GUI (company shares)
 */
public class MarketGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if this is a MarketGUI
        if (!(event.getInventory().getHolder() instanceof MarketGUI marketGUI)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement

        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        ClickType clickType = event.getClick();
        
        try {
            handleGUIClick(player, marketGUI, slot, clickType, clickedItem);
        } catch (Exception e) {
            logger.warning("Error handling market GUI click for " + player.getName() + ": " + e.getMessage());
            Translation.GUI_Market_Error.sendMessage(player);
        }
    }
    
    /**
     * Handles different types of clicks in the Market GUI
     */
    private void handleGUIClick(Player player, MarketGUI marketGUI, int slot, ClickType clickType, ItemStack item) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        
        // Handle special buttons
        if (slot == 0 && item.getType() == Material.BOOK) {
            // Portfolio overview button
            openPortfolioGUI(player);
            return;
        }
        
        if (slot == 4) {
            // Filter button - cycle through filter modes
            marketGUI.toggleFilter();
            String filterMode = marketGUI.getFilterMode().toString();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&aFilter changed to: &e" + filterMode
            ));
            return;
        }
        
        if (slot == 8 && item.getType() == Material.GOLD_INGOT) {
            // Wallet button - show balance info
            double balance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
            Translation.Wallet_Balance.sendMessage(player,
                new Replaceable("%balance%", String.format("%.2f", balance)));
            return;
        }
        
        if (slot == 45 && item.getType() == Material.CLOCK) {
            // Refresh button
            marketGUI.refresh();
            Translation.GUI_Market_Refresh_Success.sendMessage(player);
            return;
        }
        
        if (slot == 49 && item.getType() == Material.CHEST) {
            // Portfolio holdings button
            openPortfolioGUI(player);
            return;
        }
        
        if (slot == 53 && item.getType() == Material.BARRIER) {
            // Close button
            player.closeInventory();
            return;
        }
        
        // Handle stock item clicks (slots 9-44)
        if (slot >= 9 && slot < 45) {
            String symbol = marketGUI.getStockSymbolFromSlot(slot);
            if (symbol != null && !symbol.isEmpty()) {
                handleStockClick(player, symbol, clickType);
            }
        }
    }
    
    /**
     * Handles clicks on company shares in the market
     */
    private void handleStockClick(Player player, String symbol, ClickType clickType) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        
        // Find company by symbol
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByNameOrSymbol(symbol);
        if (companyOpt.isEmpty()) {
            Translation.Company_Error_CompanyNotFound.sendMessage(player,
                new Replaceable("%company%", symbol));
            return;
        }
        
        Company company = companyOpt.get();
        
        if (!company.isOnMarket()) {
            Translation.Company_Error_NotOnMarket.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Get current share price
        double sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
        
        switch (clickType) {
            case LEFT:
                // Quick buy 1 share
                handleQuickBuy(player, playerUuid, company, sharePrice);
                break;
                
            case RIGHT:
                // Quick sell 1 share
                handleQuickSell(player, playerUuid, company, sharePrice);
                break;
                
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                // Custom amount - close GUI and prompt for amount
                player.closeInventory();
                String action = clickType == ClickType.SHIFT_LEFT ? "buy" : "sell";
                Translation.Market_Buy_CustomPrompt.sendMessage(player,
                    new Replaceable("%action%", action),
                    new Replaceable("%company%", company.getName()),
                    new Replaceable("%symbol%", symbol));
                break;
                
            default:
                // Show company details
                showCompanyDetails(player, company, sharePrice);
                break;
        }
    }
    
    /**
     * Handles quick buy of 1 share
     */
    private void handleQuickBuy(Player player, String playerUuid, Company company, double price) {
        try {
            double balance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
            
            if (balance < price) {
                Translation.Company_Error_InsufficientFunds.sendMessage(player,
                    new Replaceable("%needed%", String.format("%.2f", price - balance)));
                playErrorSound(player);
                return;
            }
            
            // Execute the purchase
            QuickStocksPlugin.getCompanyMarketService().buyShares(company.getId(), playerUuid, 1.0);
            
            Translation.Market_Buy_Success.sendMessage(player,
                new Replaceable("%qty%", "1"),
                new Replaceable("%company%", company.getName()),
                new Replaceable("%total%", String.format("%.2f", price)));
            Translation.Market_Balance_Updated.sendMessage(player,
                new Replaceable("%balance%", String.format("%.2f", QuickStocksPlugin.getWalletService().getBalance(playerUuid))));
            playSuccessSound(player);
            
        } catch (Exception e) {
            Translation.Market_Error_TransactionFailed.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
            playErrorSound(player);
            logger.warning("Error in quick buy: " + e.getMessage());
        }
    }
    
    /**
     * Handles quick sell of 1 share
     */
    private void handleQuickSell(Player player, String playerUuid, Company company, double price) {
        try {
            // Check if player has shares
            double playerShares = QuickStocksPlugin.getCompanyMarketService().getPlayerSharesFromHoldings(company.getId(), playerUuid);
            if (playerShares < 1.0) {
                Translation.Market_Error_NoShares.sendMessage(player,
                    new Replaceable("%company%", company.getName()));
                playErrorSound(player);
                return;
            }
            
            // Execute the sale
            QuickStocksPlugin.getCompanyMarketService().sellShares(company.getId(), playerUuid, 1.0);
            
            Translation.Market_Sell_Success.sendMessage(player,
                new Replaceable("%qty%", "1"),
                new Replaceable("%company%", company.getName()),
                new Replaceable("%total%", String.format("%.2f", price)));
            Translation.Market_Balance_Updated.sendMessage(player,
                new Replaceable("%balance%", String.format("%.2f", QuickStocksPlugin.getWalletService().getBalance(playerUuid))));
            playSuccessSound(player);
            
        } catch (Exception e) {
            Translation.Market_Error_TransactionFailed.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
            playErrorSound(player);
            logger.warning("Error in quick sell: " + e.getMessage());
        }
    }
    
    /**
     * Shows detailed company information
     */
    private void showCompanyDetails(Player player, Company company, double sharePrice) {
        Translation.Market_CompanyDetails.sendMessage(player,
            new Replaceable("%company%", company.getName()),
            new Replaceable("%symbol%", company.getSymbol()),
            new Replaceable("%price%", String.format("%.2f", sharePrice)),
            new Replaceable("%balance%", String.format("%.2f", company.getBalance())),
            new Replaceable("%market_pct%", String.format("%.1f", company.getMarketPercentage())));
    }
    
    /**
     * Plays success sound
     */
    private void playSuccessSound(Player player) {
        try {
            player.playSound(player.getLocation(), "entity.experience_orb.pickup", 1.0f, 1.2f);
        } catch (Exception ignored) {}
    }
    
    /**
     * Plays error sound
     */
    private void playErrorSound(Player player) {
        try {
            player.playSound(player.getLocation(), "entity.villager.no", 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }
    
    /**
     * Opens the portfolio GUI
     */
    private void openPortfolioGUI(Player player) {
        try {
            PortfolioGUI portfolioGUI = new PortfolioGUI(player);
            portfolioGUI.open();
        } catch (Exception e) {
            logger.warning("Error opening portfolio GUI for " + player.getName() + ": " + e.getMessage());
            
            // Fallback to chat-based portfolio display
            try {
                showPortfolioInChat(player);
            } catch (Exception fallbackError) {
                Translation.GUI_Portfolio_Error.sendMessage(player);
            }
        }
    }
    
    /**
     * Fallback method to show portfolio in chat
     */
    private void showPortfolioInChat(Player player) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        List<HoldingsService.Holding> holdings = QuickStocksPlugin.getHoldingsService().getHoldings(playerUuid);
        double portfolioValue = QuickStocksPlugin.getHoldingsService().getPortfolioValue(playerUuid);
        double walletBalance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
        
        Translation.Market_Portfolio_Header.sendMessage(player);
        Translation.Market_Portfolio_Cash.sendMessage(player,
            new Replaceable("%balance%", String.format("%.2f", walletBalance)));
        Translation.Market_Portfolio_Value.sendMessage(player,
            new Replaceable("%value%", String.format("%.2f", portfolioValue)));
        Translation.Market_Portfolio_Total.sendMessage(player,
            new Replaceable("%total%", String.format("%.2f", walletBalance + portfolioValue)));
        
        if (holdings.isEmpty()) {
            Translation.Market_Portfolio_Empty.sendMessage(player);
            return;
        }
        
        Translation.Market_Portfolio_HoldingsHeader.sendMessage(player);
        for (HoldingsService.Holding holding : holdings) {
            String pnlColor = holding.getUnrealizedPnL() >= 0 ? "&a" : "&c";
            String pnlArrow = holding.getUnrealizedPnL() >= 0 ? "▲" : "▼";
            
            Translation.Market_Portfolio_HoldingItem.sendMessage(player,
                new Replaceable("%symbol%", holding.symbol()),
                new Replaceable("%qty%", String.format("%.2f", holding.qty())),
                new Replaceable("%avgcost%", String.format("%.2f", holding.avgCost())),
                new Replaceable("%current%", String.format("%.2f", holding.currentPrice())),
                new Replaceable("%arrow%", pnlArrow),
                new Replaceable("%pnlcolor%", pnlColor),
                new Replaceable("%pnl%", String.format("%.2f", Math.abs(holding.getUnrealizedPnL()))),
                new Replaceable("%percent%", String.format("%.1f", holding.getUnrealizedPnLPercent())));
        }
    }
}