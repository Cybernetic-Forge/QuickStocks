package com.example.quickstocks.listeners;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import com.example.quickstocks.gui.MarketGUI;
import com.example.quickstocks.gui.PortfolioGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/**
 * Handles interactions with the Portfolio GUI
 */
public class PortfolioGUIListener implements Listener {
    
    private static final Logger logger = Logger.getLogger(PortfolioGUIListener.class.getName());
    
    private final QueryService queryService;
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    
    public PortfolioGUIListener(QueryService queryService, TradingService tradingService,
                              HoldingsService holdingsService, WalletService walletService) {
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is a PortfolioGUI
        if (!(event.getInventory().getHolder() instanceof PortfolioGUI)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement
        
        PortfolioGUI portfolioGUI = (PortfolioGUI) event.getInventory().getHolder();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        try {
            handlePortfolioClick(player, portfolioGUI, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.warning("Error handling portfolio GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while processing your request.");
        }
    }
    
    /**
     * Handles clicks in the Portfolio GUI
     */
    private void handlePortfolioClick(Player player, PortfolioGUI portfolioGUI, int slot, ItemStack item, org.bukkit.event.inventory.ClickType clickType) throws Exception {
        // Handle navigation buttons
        if (slot == 45 && item.getType() == Material.COMPASS) {
            // Back to market button
            openMarketGUI(player);
            return;
        }
        
        if (slot == 49 && item.getType() == Material.CLOCK) {
            // Refresh button
            portfolioGUI.refresh();
            player.sendMessage(ChatColor.GREEN + "Portfolio refreshed!");
            return;
        }
        
        if (slot == 53 && item.getType() == Material.BARRIER) {
            // Close button
            player.closeInventory();
            return;
        }
        
        // Handle holding clicks (slots 9-44)
        if (slot >= 9 && slot < 45) {
            String symbol = portfolioGUI.getHoldingSymbolFromSlot(slot);
            if (symbol != null && !symbol.isEmpty()) {
                handleHoldingClick(player, symbol, portfolioGUI, clickType);
            }
        }
    }
    
    /**
     * Handles clicks on holding items
     */
    private void handleHoldingClick(Player player, String symbol, PortfolioGUI portfolioGUI, org.bukkit.event.inventory.ClickType clickType) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        
        // Get instrument ID
        String instrumentId = queryService.getInstrumentIdBySymbol(symbol);
        if (instrumentId == null) {
            player.sendMessage(ChatColor.RED + "Stock not found: " + symbol);
            return;
        }
        
        // Get holding info
        HoldingsService.Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
        if (holding == null) {
            player.sendMessage(ChatColor.RED + "You don't own any shares of " + symbol);
            return;
        }
        
        if (clickType == org.bukkit.event.inventory.ClickType.RIGHT) {
            // Sell all shares
            handleSellAllShares(player, playerUuid, instrumentId, symbol, holding, portfolioGUI);
        } else {
            // Show holding details
            showHoldingDetails(player, holding);
        }
    }
    
    /**
     * Handles selling all shares of a holding
     */
    private void handleSellAllShares(Player player, String playerUuid, String instrumentId, String symbol, HoldingsService.Holding holding, PortfolioGUI portfolioGUI) throws Exception {
        double qty = holding.getQty();
        
        // Execute the trade
        TradingService.TradeResult result = tradingService.executeSellOrder(playerUuid, instrumentId, qty);
        
        if (result.isSuccess()) {
            double totalValue = qty * holding.getCurrentPrice();
            player.sendMessage(ChatColor.GREEN + "✓ Sold all " + String.format("%.2f", qty) + " shares of " + symbol);
            player.sendMessage(ChatColor.GREEN + "Received: $" + String.format("%.2f", totalValue));
            player.sendMessage(ChatColor.GRAY + "New balance: $" + String.format("%.2f", walletService.getBalance(playerUuid)));
            
            // Refresh the GUI immediately after successful sale
            portfolioGUI.refresh();
        } else {
            player.sendMessage(ChatColor.RED + "✗ Sale failed: " + result.getMessage());
        }
    }
    
    /**
     * Shows detailed holding information
     */
    private void showHoldingDetails(Player player, HoldingsService.Holding holding) {
        player.sendMessage(ChatColor.GOLD + "=== " + holding.getSymbol() + " Holdings ===");
        player.sendMessage(ChatColor.YELLOW + "Shares: " + ChatColor.WHITE + String.format("%.2f", holding.getQty()));
        player.sendMessage(ChatColor.YELLOW + "Purchase Price: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getAvgCost()));
        player.sendMessage(ChatColor.YELLOW + "Current Price: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getCurrentPrice()));
        player.sendMessage(ChatColor.YELLOW + "Total Value: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getQty() * holding.getCurrentPrice()));
        
        double pnl = holding.getUnrealizedPnL();
        ChatColor pnlColor = pnl >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String pnlArrow = pnl >= 0 ? "▲" : "▼";
        
        player.sendMessage(ChatColor.YELLOW + "Gain/Loss from Purchase: " + pnlColor + pnlArrow + "$" + String.format("%.2f", Math.abs(pnl)) + 
                          " (" + String.format("%.1f%%", Math.abs(holding.getUnrealizedPnLPercent())) + ")");
        
        player.sendMessage(ChatColor.GRAY + "Right-click to sell all shares");
    }
    
    /**
     * Opens the market GUI
     */
    private void openMarketGUI(Player player) {
        try {
            MarketGUI marketGUI = new MarketGUI(player, queryService, tradingService, holdingsService, walletService);
            marketGUI.open();
        } catch (Exception e) {
            logger.warning("Error opening market GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Unable to open market at this time.");
        }
    }
}