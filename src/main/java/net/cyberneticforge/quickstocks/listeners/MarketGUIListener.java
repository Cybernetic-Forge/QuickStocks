package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import Instrument;
import InstrumentState;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.features.portfolio.HoldingsService;
import net.cyberneticforge.quickstocks.gui.MarketGUI;
import net.cyberneticforge.quickstocks.gui.PortfolioGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
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
            player.sendMessage(ChatUT.hexComp(
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
                handleInstrumentClick(player, symbol, clickType);
            }
        }
    }
    
    /**
     * Handles clicks on any instrument in the market (company shares, crypto, or items)
     */
    private void handleInstrumentClick(Player player, String symbol, ClickType clickType) throws Exception {
        String playerUuid = player.getUniqueId().toString();
        
        // Determine the instrument type by checking different sources
        // 1. Check if it's a company share
        Optional<Company> companyOpt = QuickStocksPlugin.getCompanyService().getCompanyByNameOrSymbol(symbol);
        if (companyOpt.isPresent()) {
            handleCompanyShareClick(player, playerUuid, companyOpt.get(), clickType);
            return;
        }
        
        // 2. Check if it's a generic instrument (crypto or item)
        try {
            Optional<Instrument> instrumentOpt = 
                QuickStocksPlugin.getInstrumentPersistenceService().getInstrumentBySymbol(symbol);
            
            if (instrumentOpt.isPresent()) {
                handleGenericInstrumentClick(player, playerUuid, instrumentOpt.get(), clickType);
                return;
            }
        } catch (SQLException e) {
            logger.warning("Error looking up instrument " + symbol + ": " + e.getMessage());
        }
        
        // Unknown instrument
        Translation.Market_Error_InstrumentNotFound.sendMessage(player,
            new Replaceable("%symbol%", symbol));
    }
    
    /**
     * Handles clicks on company shares in the market
     */
    private void handleCompanyShareClick(Player player, String playerUuid, Company company, ClickType clickType) throws Exception {
        
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
     * Handles clicks on generic instruments (crypto and items)
     */
    private void handleGenericInstrumentClick(Player player, String playerUuid, 
            Instrument instrument, ClickType clickType) throws Exception {
        
        // Get current price
        Optional<InstrumentState> stateOpt = 
            QuickStocksPlugin.getInstrumentPersistenceService().getInstrumentState(instrument.id());
        
        if (stateOpt.isEmpty()) {
            Translation.Market_Error_PriceNotAvailable.sendMessage(player,
                new Replaceable("%symbol%", instrument.symbol()));
            return;
        }
        
        double currentPrice = stateOpt.get().lastPrice();
        
        switch (clickType) {
            case LEFT:
                // Quick buy 1 unit
                handleGenericInstrumentBuy(player, playerUuid, instrument, currentPrice, 1.0);
                break;
                
            case RIGHT:
                // Quick sell 1 unit
                handleGenericInstrumentSell(player, playerUuid, instrument, currentPrice, 1.0);
                break;
                
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                // Custom amount - close GUI and prompt for amount
                player.closeInventory();
                String action = clickType == ClickType.SHIFT_LEFT ? "buy" : "sell";
                Translation.Market_Buy_CustomPrompt.sendMessage(player,
                    new Replaceable("%action%", action),
                    new Replaceable("%company%", instrument.displayName()),
                    new Replaceable("%symbol%", instrument.symbol()));
                break;
                
            default:
                // Show instrument details
                showGenericInstrumentDetails(player, instrument, stateOpt.get());
                break;
        }
    }
    
    /**
     * Handles buying a generic instrument (crypto or item)
     */
    private void handleGenericInstrumentBuy(Player player, String playerUuid, 
            Instrument instrument, double price, double quantity) {
        try {
            double totalCost = price * quantity;
            double balance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
            
            if (balance < totalCost) {
                Translation.Company_Error_InsufficientFunds.sendMessage(player,
                    new Replaceable("%needed%", String.format("%.2f", totalCost - balance)));
                playErrorSound(player);
                return;
            }
            
            // Execute the purchase using TradingService
            var result = QuickStocksPlugin.getTradingService().executeBuyOrder(playerUuid, instrument.id(), quantity);
            
            if (result.success()) {
                Translation.Market_Buy_Success.sendMessage(player,
                    new Replaceable("%qty%", String.format("%.2f", quantity)),
                    new Replaceable("%company%", instrument.displayName()),
                    new Replaceable("%total%", String.format("%.2f", totalCost)));
                Translation.Market_Balance_Updated.sendMessage(player,
                    new Replaceable("%balance%", String.format("%.2f", QuickStocksPlugin.getWalletService().getBalance(playerUuid))));
                playSuccessSound(player);
            } else {
                Translation.Market_Error_TransactionFailed.sendMessage(player,
                    new Replaceable("%error%", result.message()));
                playErrorSound(player);
            }
            
        } catch (Exception e) {
            Translation.Market_Error_TransactionFailed.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
            playErrorSound(player);
            logger.warning("Error in generic instrument buy: " + e.getMessage());
        }
    }
    
    /**
     * Handles selling a generic instrument (crypto or item)
     */
    private void handleGenericInstrumentSell(Player player, String playerUuid, 
            Instrument instrument, double price, double quantity) {
        try {
            // Check if player has holdings
            var holdings = QuickStocksPlugin.getHoldingsService().getHoldings(playerUuid);
            boolean hasHolding = holdings.stream()
                .anyMatch(h -> h.symbol().equals(instrument.symbol()) && h.qty() >= quantity);
            
            if (!hasHolding) {
                Translation.Market_Error_NoShares.sendMessage(player,
                    new Replaceable("%company%", instrument.displayName()));
                playErrorSound(player);
                return;
            }
            
            // Execute the sale using TradingService
            var result = QuickStocksPlugin.getTradingService().executeSellOrder(playerUuid, instrument.id(), quantity);
            
            if (result.success()) {
                double totalValue = price * quantity;
                Translation.Market_Sell_Success.sendMessage(player,
                    new Replaceable("%qty%", String.format("%.2f", quantity)),
                    new Replaceable("%company%", instrument.displayName()),
                    new Replaceable("%total%", String.format("%.2f", totalValue)));
                Translation.Market_Balance_Updated.sendMessage(player,
                    new Replaceable("%balance%", String.format("%.2f", QuickStocksPlugin.getWalletService().getBalance(playerUuid))));
                playSuccessSound(player);
            } else {
                Translation.Market_Error_TransactionFailed.sendMessage(player,
                    new Replaceable("%error%", result.message()));
                playErrorSound(player);
            }
            
        } catch (Exception e) {
            Translation.Market_Error_TransactionFailed.sendMessage(player,
                new Replaceable("%error%", e.getMessage()));
            playErrorSound(player);
            logger.warning("Error in generic instrument sell: " + e.getMessage());
        }
    }
    
    /**
     * Shows detailed information about a generic instrument
     */
    private void showGenericInstrumentDetails(Player player, 
            Instrument instrument,
            InstrumentState state) {
        
        String typeDisplay = switch (instrument.type()) {
            case "ITEM" -> "Item Instrument";
            case "CRYPTO", "CUSTOM_CRYPTO" -> "Cryptocurrency";
            default -> "Instrument";
        };
        
        Translation.Market_InstrumentDetails.sendMessage(player,
            new Replaceable("%name%", instrument.displayName()),
            new Replaceable("%symbol%", instrument.symbol()),
            new Replaceable("%type%", typeDisplay),
            new Replaceable("%price%", String.format("%.2f", state.lastPrice())),
            new Replaceable("%change_24h%", String.format("%.2f", state.change24h())),
            new Replaceable("%volume%", String.format("%.2f", state.lastVolume())));
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