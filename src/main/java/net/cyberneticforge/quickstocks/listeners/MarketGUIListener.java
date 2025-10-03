package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.application.queries.QueryService;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyMarketService;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.TradingService;
import net.cyberneticforge.quickstocks.core.services.WalletService;
import net.cyberneticforge.quickstocks.gui.MarketGUI;
import net.cyberneticforge.quickstocks.gui.PortfolioGUI;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.cyberneticforge.quickstocks.utils.GUIConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Handles interactions with the Market GUI (company shares)
 */
public class MarketGUIListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(MarketGUIListener.class.getName());
    
    private final QueryService queryService;
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final CompanyService companyService;
    private final CompanyMarketService companyMarketService;
    private final GUIConfigManager guiConfigManager;
    
    public MarketGUIListener(QueryService queryService, TradingService tradingService,
                           HoldingsService holdingsService, WalletService walletService,
                           CompanyService companyService, CompanyMarketService companyMarketService,
                           GUIConfigManager guiConfigManager) {
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.companyService = companyService;
        this.companyMarketService = companyMarketService;
        this.guiConfigManager = guiConfigManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is a MarketGUI
        if (!(event.getInventory().getHolder() instanceof MarketGUI)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement
        
        MarketGUI marketGUI = (MarketGUI) event.getInventory().getHolder();
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
            player.sendMessage(ChatUT.hexComp("&cAn error occurred while processing your request."));
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
        
        if (slot == 8 && item.getType() == Material.GOLD_INGOT) {
            // Wallet button - show balance info
            double balance = walletService.getBalance(playerUuid);
            player.sendMessage(ChatUT.hexComp("&gold" + "Your wallet balance: " + ChatColor.GREEN + "$" + String.format("%.2f", balance)));
            return;
        }
        
        if (slot == 45 && item.getType() == Material.CLOCK) {
            // Refresh button
            marketGUI.refresh();
            player.sendMessage(ChatUT.hexComp("&green" + "Market data refreshed!"));
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
        Optional<Company> companyOpt = companyService.getCompanyByNameOrSymbol(symbol);
        if (companyOpt.isEmpty()) {
            player.sendMessage(ChatUT.hexComp("&red" + "Company not found: " + symbol));
            return;
        }
        
        Company company = companyOpt.get();
        
        if (!company.isOnMarket()) {
            player.sendMessage(ChatUT.hexComp("&red" + "Company '" + company.getName() + "' is not on the market."));
            return;
        }
        
        // Get current share price
        double sharePrice = companyMarketService.calculateSharePrice(company);
        
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
                player.sendMessage(ChatUT.hexComp("&yellow" + "Enter amount to " + action + " for " + company.getName() + ":"));
                player.sendMessage(ChatUT.hexComp("&gray" + "Use: " + ChatColor.WHITE + "/market " + action + " " + symbol + " <amount>"));
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
            double balance = walletService.getBalance(playerUuid);
            
            if (balance < price) {
                player.sendMessage(ChatUT.hexComp("&red" + "Insufficient funds! Need $" + String.format("%.2f", price - balance) + " more."));
                playErrorSound(player);
                return;
            }
            
            // Execute the purchase
            companyMarketService.buyShares(company.getId(), playerUuid, 1.0);
            
            player.sendMessage(ChatUT.hexComp("&green" + "✓ Bought 1 share of " + company.getName() + " for $" + String.format("%.2f", price)));
            player.sendMessage(ChatUT.hexComp("&gray" + "New balance: $" + String.format("%.2f", walletService.getBalance(playerUuid))));
            playSuccessSound(player);
            
        } catch (Exception e) {
            player.sendMessage(ChatUT.hexComp("&red" + "✗ Purchase failed: " + e.getMessage()));
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
            double playerShares = companyMarketService.getPlayerSharesFromHoldings(company.getId(), playerUuid);
            if (playerShares < 1.0) {
                player.sendMessage(ChatUT.hexComp("&red" + "You don't have any shares of " + company.getName() + "!"));
                playErrorSound(player);
                return;
            }
            
            // Execute the sale
            companyMarketService.sellShares(company.getId(), playerUuid, 1.0);
            
            player.sendMessage(ChatUT.hexComp("&green" + "✓ Sold 1 share of " + company.getName() + " for $" + String.format("%.2f", price)));
            player.sendMessage(ChatUT.hexComp("&gray" + "New balance: $" + String.format("%.2f", walletService.getBalance(playerUuid))));
            playSuccessSound(player);
            
        } catch (Exception e) {
            player.sendMessage(ChatUT.hexComp("&red" + "✗ Sale failed: " + e.getMessage()));
            playErrorSound(player);
            logger.warning("Error in quick sell: " + e.getMessage());
        }
    }
    
    /**
     * Shows detailed company information
     */
    private void showCompanyDetails(Player player, Company company, double sharePrice) {
        player.sendMessage(ChatUT.hexComp("&gold" + "=== " + company.getName() + " (" + company.getSymbol() + ") ==="));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Share Price: " + ChatColor.WHITE + "$" + String.format("%.2f", sharePrice)));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Company Balance: " + ChatColor.WHITE + "$" + String.format("%.2f", company.getBalance())));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Market %: " + ChatColor.WHITE + String.format("%.1f%%", company.getMarketPercentage())));
        player.sendMessage(ChatUT.hexComp("&gray" + "Use left-click to buy, right-click to sell"));
        player.sendMessage(ChatUT.hexComp("&gray" + "Shift+click for custom amounts"));
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
            PortfolioGUI portfolioGUI = new PortfolioGUI(player, queryService, holdingsService, walletService, guiConfigManager);
            portfolioGUI.open();
        } catch (Exception e) {
            logger.warning("Error opening portfolio GUI for " + player.getName() + ": " + e.getMessage());
            
            // Fallback to chat-based portfolio display
            try {
                showPortfolioInChat(player);
            } catch (Exception fallbackError) {
                player.sendMessage(ChatUT.hexComp("&red" + "Unable to display portfolio at this time."));
            }
        }
    }
    
    /**
     * Fallback method to show portfolio in chat
     */
    private void showPortfolioInChat(Player player) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        List<HoldingsService.Holding> holdings = holdingsService.getHoldings(playerUuid);
        double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
        double walletBalance = walletService.getBalance(playerUuid);
        
        player.sendMessage(ChatUT.hexComp("&gold" + "=== " + ChatColor.WHITE + "Your Portfolio" + ChatColor.GOLD + " ==="));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Cash Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance)));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Portfolio Value: " + ChatColor.GREEN + "$" + String.format("%.2f", portfolioValue)));
        player.sendMessage(ChatUT.hexComp("&yellow" + "Total Assets: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance + portfolioValue)));
        
        if (holdings.isEmpty()) {
            player.sendMessage(ChatUT.hexComp("&gray" + "No holdings found."));
            return;
        }
        
        player.sendMessage(ChatUT.hexComp("&yellow" + "\nHoldings:"));
        for (HoldingsService.Holding holding : holdings) {
            ChatColor pnlColor = holding.getUnrealizedPnL() >= 0 ? ChatColor.GREEN : ChatColor.RED;
            String pnlArrow = holding.getUnrealizedPnL() >= 0 ? "▲" : "▼";
            
            player.sendMessage(String.format(ChatColor.WHITE + "%s: " + ChatColor.GRAY + "%.2f shares @ $%.2f avg " +
                ChatColor.YELLOW + "($%.2f current) " + pnlColor + "%s$%.2f (%.1f%%)",
                holding.getSymbol(), holding.getQty(), holding.getAvgCost(), 
                holding.getCurrentPrice(), pnlArrow, Math.abs(holding.getUnrealizedPnL()), 
                holding.getUnrealizedPnLPercent()));
        }
    }
}