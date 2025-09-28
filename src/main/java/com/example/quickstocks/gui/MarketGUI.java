package com.example.quickstocks.gui;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Professional Market GUI for QuickStocks
 * Displays market data in an interactive inventory interface
 */
public class MarketGUI implements InventoryHolder {
    
    private static final Logger logger = Logger.getLogger(MarketGUI.class.getName());
    private static final int GUI_SIZE = 54; // 6 rows
    
    private final QueryService queryService;
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final Inventory inventory;
    private final Player player;
    
    public MarketGUI(Player player, QueryService queryService, TradingService tradingService, 
                     HoldingsService holdingsService, WalletService walletService) {
        this.player = player;
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, ChatColor.DARK_GREEN + "Market - QuickStocks");
        
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Sets up the GUI with market data and interface elements
     */
    private void setupGUI() {
        try {
            // Add portfolio info button (top left)
            addPortfolioInfoButton();
            
            // Add wallet info button (top right)
            addWalletInfoButton();
            
            // Add stocks to the inventory
            addStocksToGUI();
            
            // Add navigation/action buttons at the bottom
            addNavigationButtons();
            
        } catch (Exception e) {
            logger.warning("Error setting up Market GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Failed to load market data. Please try again.");
        }
    }
    
    /**
     * Adds portfolio information button
     */
    private void addPortfolioInfoButton() {
        try {
            ItemStack portfolioItem = new ItemStack(Material.BOOK);
            ItemMeta meta = portfolioItem.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Portfolio Overview");
            
            List<String> lore = new ArrayList<>();
            String playerUuid = player.getUniqueId().toString();
            
            double walletBalance = walletService.getBalance(playerUuid);
            double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;
            
            lore.add(ChatColor.YELLOW + "Cash Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance));
            lore.add(ChatColor.YELLOW + "Portfolio Value: " + ChatColor.GREEN + "$" + String.format("%.2f", portfolioValue));
            lore.add(ChatColor.YELLOW + "Total Assets: " + ChatColor.GREEN + "$" + String.format("%.2f", totalAssets));
            lore.add("");
            lore.add(ChatColor.GRAY + "Click to view detailed portfolio");
            
            meta.setLore(lore);
            portfolioItem.setItemMeta(meta);
            inventory.setItem(0, portfolioItem);
            
        } catch (Exception e) {
            logger.warning("Error adding portfolio info: " + e.getMessage());
        }
    }
    
    /**
     * Adds wallet information button
     */
    private void addWalletInfoButton() {
        try {
            ItemStack walletItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = walletItem.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Wallet");
            
            List<String> lore = new ArrayList<>();
            double balance = walletService.getBalance(player.getUniqueId().toString());
            
            lore.add(ChatColor.YELLOW + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", balance));
            lore.add("");
            lore.add(ChatColor.GRAY + "Your available cash for trading");
            
            meta.setLore(lore);
            walletItem.setItemMeta(meta);
            inventory.setItem(8, walletItem);
            
        } catch (Exception e) {
            logger.warning("Error adding wallet info: " + e.getMessage());
        }
    }
    
    /**
     * Adds stock items to the GUI
     */
    private void addStocksToGUI() {
        try {
            List<Map<String, Object>> topGainers = queryService.getTopGainers(36); // Fill most of the GUI
            
            int slot = 9; // Start from second row
            for (Map<String, Object> stock : topGainers) {
                if (slot >= 45) break; // Leave bottom row for navigation
                
                ItemStack stockItem = createStockItem(stock);
                inventory.setItem(slot, stockItem);
                slot++;
            }
            
            // Fill empty slots with barrier blocks to indicate no more stocks
            for (int i = slot; i < 45; i++) {
                ItemStack emptySlot = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = emptySlot.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "No Stock Data");
                emptySlot.setItemMeta(meta);
                inventory.setItem(i, emptySlot);
            }
            
        } catch (Exception e) {
            logger.warning("Error adding stocks to GUI: " + e.getMessage());
        }
    }
    
    /**
     * Creates an ItemStack representing a stock
     */
    private ItemStack createStockItem(Map<String, Object> stock) {
        String symbol = (String) stock.get("symbol");
        String displayName = (String) stock.get("display_name");
        Double price = (Double) stock.get("last_price");
        Double change24h = (Double) stock.get("change_24h");
        Double volatility = (Double) stock.get("volatility_24h");
        String type = (String) stock.get("type");
        
        // Choose material based on stock type and performance
        Material material = getMaterialForStock(type, change24h);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name with color coding
        ChatColor nameColor = change24h >= 0 ? ChatColor.GREEN : ChatColor.RED;
        meta.setDisplayName(nameColor + displayName + " (" + symbol + ")");
        
        // Create detailed lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Price: " + ChatColor.WHITE + "$" + String.format("%.2f", price));
        
        // 24h change with color and arrow
        ChatColor changeColor = change24h >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String changeArrow = change24h >= 0 ? "▲" : "▼";
        lore.add(ChatColor.YELLOW + "24h Change: " + changeColor + changeArrow + String.format("%.2f%%", Math.abs(change24h)));
        
        // Volatility
        if (volatility != null) {
            lore.add(ChatColor.YELLOW + "Volatility: " + ChatColor.GRAY + String.format("%.2f%%", volatility));
        }
        
        // Stock type
        lore.add(ChatColor.YELLOW + "Type: " + ChatColor.GRAY + type);
        
        lore.add("");
        lore.add(ChatColor.GREEN + "Left Click: " + ChatColor.WHITE + "Quick Buy (1 share)");
        lore.add(ChatColor.RED + "Right Click: " + ChatColor.WHITE + "Quick Sell (1 share)");
        lore.add(ChatColor.YELLOW + "Shift+Click: " + ChatColor.WHITE + "Custom Amount");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Determines the appropriate material for a stock based on type and performance
     */
    private Material getMaterialForStock(String type, Double change24h) {
        // Base material on type
        Material baseMaterial;
        switch (type.toLowerCase()) {
            case "crypto":
            case "cryptocurrency":
                baseMaterial = Material.GOLD_NUGGET;
                break;
            case "tech":
            case "technology":
                baseMaterial = Material.REDSTONE;
                break;
            case "energy":
                baseMaterial = Material.COAL;
                break;
            case "finance":
            case "financial":
                baseMaterial = Material.EMERALD;
                break;
            case "healthcare":
                baseMaterial = Material.POTION;
                break;
            case "consumer":
                baseMaterial = Material.APPLE;
                break;
            default:
                baseMaterial = Material.PAPER;
                break;
        }
        
        // Modify based on performance for visual feedback
        if (change24h != null) {
            if (change24h >= 5.0) {
                // High positive performance - use diamond variant if available
                if (baseMaterial == Material.PAPER) return Material.DIAMOND;
                if (baseMaterial == Material.EMERALD) return Material.EMERALD_BLOCK;
            } else if (change24h <= -5.0) {
                // High negative performance - use coal/dark variant
                if (baseMaterial == Material.PAPER) return Material.COAL;
                if (baseMaterial == Material.EMERALD) return Material.COAL_BLOCK;
            }
        }
        
        return baseMaterial;
    }
    
    /**
     * Adds navigation buttons at the bottom of the GUI
     */
    private void addNavigationButtons() {
        // Refresh button
        ItemStack refreshItem = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.AQUA + "Refresh Market Data");
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add(ChatColor.GRAY + "Click to refresh stock prices");
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        inventory.setItem(45, refreshItem);
        
        // Portfolio view button
        ItemStack portfolioItem = new ItemStack(Material.CHEST);
        ItemMeta portfolioMeta = portfolioItem.getItemMeta();
        portfolioMeta.setDisplayName(ChatColor.GOLD + "My Holdings");
        List<String> portfolioLore = new ArrayList<>();
        portfolioLore.add(ChatColor.GRAY + "View your stock portfolio");
        portfolioMeta.setLore(portfolioLore);
        portfolioItem.setItemMeta(portfolioMeta);
        inventory.setItem(49, portfolioItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close Market");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Close the market interface");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
    }
    
    /**
     * Opens the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }
    
    /**
     * Refreshes the GUI content
     */
    public void refresh() {
        inventory.clear();
        setupGUI();
    }
    
    /**
     * Gets the stock symbol from an inventory slot
     */
    public String getStockSymbolFromSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        // Extract symbol from display name format: "DisplayName (SYMBOL)"
        int startIndex = displayName.lastIndexOf('(');
        int endIndex = displayName.lastIndexOf(')');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            String symbol = displayName.substring(startIndex + 1, endIndex);
            return ChatColor.stripColor(symbol);
        }
        
        return null;
    }
}