package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.features.market.TradingService;
import net.cyberneticforge.quickstocks.core.services.features.portfolio.HoldingsService;
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

/**
 * Handles interactions with the Portfolio GUI
 */
public class PortfolioGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if this is a PortfolioGUI
        if (!(event.getInventory().getHolder() instanceof PortfolioGUI portfolioGUI)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        try {
            handlePortfolioClick(player, portfolioGUI, slot, clickedItem, event.getClick());
        } catch (Exception e) {
            logger.warning("Error handling portfolio GUI click for " + player.getName() + ": " + e.getMessage());
            Translation.GUI_Portfolio_Error.sendMessage(player);
        }
    }
    
    /**
     * Handles clicks in the Portfolio GUI
     */
    private void handlePortfolioClick(Player player, PortfolioGUI portfolioGUI, int slot, ItemStack item, ClickType clickType) throws Exception {
        // Handle navigation buttons
        if (slot == 45 && item.getType() == Material.COMPASS) {
            // Back to market button
            openMarketGUI(player);
            return;
        }
        
        if (slot == 49 && item.getType() == Material.CLOCK) {
            // Refresh button
            portfolioGUI.refresh();
            Translation.GUI_Portfolio_Refresh_Success.sendMessage(player);
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
                handleHoldingClick(player, symbol, clickType);
            }
        }
    }
    
    /**
     * Handles clicks on holding items
     */
    private void handleHoldingClick(Player player, String symbol, org.bukkit.event.inventory.ClickType clickType) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        
        // Get instrument ID
        String instrumentId = QuickStocksPlugin.getQueryService().getInstrumentIdBySymbol(symbol);
        if (instrumentId == null) {
            Translation.GUI_Portfolio_StockNotFound.sendMessage(player,
                new Replaceable("%symbol%", symbol));
            return;
        }
        
        // Get holding info
        HoldingsService.Holding holding = QuickStocksPlugin.getHoldingsService().getHolding(playerUuid, instrumentId);
        if (holding == null) {
            Translation.GUI_Portfolio_NoShares.sendMessage(player,
                new Replaceable("%symbol%", symbol));
            return;
        }
        
        if (clickType == org.bukkit.event.inventory.ClickType.RIGHT) {
            // Sell all shares
            handleSellAllShares(player, playerUuid, instrumentId, symbol, holding);
        } else {
            // Show holding details
            showHoldingDetails(player, holding);
        }
    }
    
    /**
     * Handles selling all shares of a holding
     */
    private void handleSellAllShares(Player player, String playerUuid, String instrumentId, String symbol, HoldingsService.Holding holding) throws Exception {
        double qty = holding.qty();
        
        // Execute the trade
        TradingService.TradeResult result = QuickStocksPlugin.getTradingService().executeSellOrder(playerUuid, instrumentId, qty);
        
        if (result.success()) {
            double totalValue = qty * holding.currentPrice();
            Translation.GUI_Portfolio_SoldAll.sendMessage(player,
                new Replaceable("%qty%", String.format("%.2f", qty)),
                new Replaceable("%symbol%", symbol));
            Translation.GUI_Portfolio_Received.sendMessage(player,
                new Replaceable("%total%", String.format("%.2f", totalValue)));
            Translation.GUI_Portfolio_NewBalance.sendMessage(player,
                new Replaceable("%balance%", String.format("%.2f", QuickStocksPlugin.getWalletService().getBalance(playerUuid))));
            
            // Note: GUI will be refreshed on next view
        } else {
            Translation.GUI_Portfolio_SaleFailed.sendMessage(player,
                new Replaceable("%message%", result.message()));
        }
    }
    
    /**
     * Shows detailed holding information
     */
    private void showHoldingDetails(Player player, HoldingsService.Holding holding) {
        Translation.GUI_Portfolio_HoldingDetails_Header.sendMessage(player,
            new Replaceable("%symbol%", holding.symbol()));
        Translation.GUI_Portfolio_HoldingDetails_Shares.sendMessage(player,
            new Replaceable("%qty%", String.format("%.2f", holding.qty())));
        Translation.GUI_Portfolio_HoldingDetails_AvgCost.sendMessage(player,
            new Replaceable("%cost%", String.format("%.2f", holding.avgCost())));
        Translation.GUI_Portfolio_HoldingDetails_CurrentPrice.sendMessage(player,
            new Replaceable("%price%", String.format("%.2f", holding.currentPrice())));
        Translation.GUI_Portfolio_HoldingDetails_TotalValue.sendMessage(player,
            new Replaceable("%value%", String.format("%.2f", holding.qty() * holding.currentPrice())));
        
        double pnl = holding.getUnrealizedPnL();
        String pnlColor = pnl >= 0 ? "&a" : "&c";
        String pnlArrow = pnl >= 0 ? "▲" : "▼";
        
        Translation.GUI_Portfolio_HoldingDetails_PnL.sendMessage(player,
            new Replaceable("%color%", pnlColor),
            new Replaceable("%arrow%", pnlArrow),
            new Replaceable("%pnl%", String.format("%.2f", Math.abs(pnl))),
            new Replaceable("%percent%", String.format("%.1f%%", Math.abs(holding.getUnrealizedPnLPercent()))));
        
        Translation.GUI_Portfolio_HoldingDetails_SellHint.sendMessage(player);
    }
    
    /**
     * Opens the market GUI
     */
    private void openMarketGUI(Player player) {
        try {
            MarketGUI marketGUI = new MarketGUI(player);
            marketGUI.open();
        } catch (Exception e) {
            logger.warning("Error opening market GUI for " + player.getName() + ": " + e.getMessage());
            Translation.GUI_Portfolio_MarketOpenError.sendMessage(player);
        }
    }
}