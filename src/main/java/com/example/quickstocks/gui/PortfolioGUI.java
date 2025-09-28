package com.example.quickstocks.gui;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.HoldingsService;
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
import java.util.logging.Logger;

/**
 * Portfolio GUI showing player's holdings and assets
 */
public class PortfolioGUI implements InventoryHolder {
    
    private static final Logger logger = Logger.getLogger(PortfolioGUI.class.getName());
    private static final int GUI_SIZE = 54; // 6 rows
    
    private final Player player;
    private final QueryService queryService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final Inventory inventory;
    
    public PortfolioGUI(Player player, QueryService queryService, HoldingsService holdingsService, WalletService walletService) {
        this.player = player;
        this.queryService = queryService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, ChatColor.GOLD + "Portfolio - " + player.getName());
        
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Sets up the portfolio GUI
     */
    private void setupGUI() {
        try {
            String playerUuid = player.getUniqueId().toString();
            
            // Add wallet info (top left)
            addWalletInfo();
            
            // Add portfolio summary (top right)
            addPortfolioSummary();
            
            // Add holdings
            addHoldings();
            
            // Add navigation buttons
            addNavigationButtons();
            
        } catch (Exception e) {
            logger.warning("Error setting up Portfolio GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "Failed to load portfolio data.");
        }
    }
    
    /**
     * Adds wallet information
     */
    private void addWalletInfo() {
        try {
            ItemStack walletItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta meta = walletItem.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Cash Balance");
            
            List<String> lore = new ArrayList<>();
            double balance = walletService.getBalance(player.getUniqueId().toString());
            
            lore.add(ChatColor.YELLOW + "Available Cash:");
            lore.add(ChatColor.GREEN + "$" + String.format("%.2f", balance));
            lore.add("");
            lore.add(ChatColor.GRAY + "Ready for trading");
            
            meta.setLore(lore);
            walletItem.setItemMeta(meta);
            inventory.setItem(0, walletItem);
            
        } catch (Exception e) {
            logger.warning("Error adding wallet info: " + e.getMessage());
        }
    }
    
    /**
     * Adds portfolio summary
     */
    private void addPortfolioSummary() {
        try {
            ItemStack summaryItem = new ItemStack(Material.EMERALD);
            ItemMeta meta = summaryItem.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "Portfolio Summary");
            
            List<String> lore = new ArrayList<>();
            String playerUuid = player.getUniqueId().toString();
            
            double walletBalance = walletService.getBalance(playerUuid);
            double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;
            
            lore.add(ChatColor.YELLOW + "Portfolio Value: " + ChatColor.GREEN + "$" + String.format("%.2f", portfolioValue));
            lore.add(ChatColor.YELLOW + "Cash Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", walletBalance));
            lore.add(ChatColor.YELLOW + "Total Assets: " + ChatColor.AQUA + "$" + String.format("%.2f", totalAssets));
            lore.add("");
            lore.add(ChatColor.GRAY + "Your complete financial overview");
            
            meta.setLore(lore);
            summaryItem.setItemMeta(meta);
            inventory.setItem(8, summaryItem);
            
        } catch (Exception e) {
            logger.warning("Error adding portfolio summary: " + e.getMessage());
        }
    }
    
    /**
     * Adds holdings to the GUI
     */
    private void addHoldings() {
        try {
            List<HoldingsService.Holding> holdings = holdingsService.getHoldings(player.getUniqueId().toString());
            
            int slot = 9; // Start from second row
            for (HoldingsService.Holding holding : holdings) {
                if (slot >= 45) break; // Leave bottom row for navigation
                
                ItemStack holdingItem = createHoldingItem(holding);
                inventory.setItem(slot, holdingItem);
                slot++;
            }
            
            // Fill empty slots if no holdings
            if (holdings.isEmpty()) {
                ItemStack noHoldings = new ItemStack(Material.BARRIER);
                ItemMeta meta = noHoldings.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "No Holdings");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "You don't own any stocks yet");
                lore.add(ChatColor.YELLOW + "Visit the market to start trading!");
                meta.setLore(lore);
                noHoldings.setItemMeta(meta);
                inventory.setItem(22, noHoldings); // Center slot
            }
            
            // Fill remaining slots with glass panes
            for (int i = slot; i < 45; i++) {
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                meta.setDisplayName(" ");
                filler.setItemMeta(meta);
                inventory.setItem(i, filler);
            }
            
        } catch (Exception e) {
            logger.warning("Error adding holdings: " + e.getMessage());
        }
    }
    
    /**
     * Creates an ItemStack representing a holding
     */
    private ItemStack createHoldingItem(HoldingsService.Holding holding) {
        // Choose material based on P&L performance
        Material material;
        ChatColor nameColor;
        
        if (holding.getUnrealizedPnL() >= 0) {
            material = holding.getUnrealizedPnL() > 100 ? Material.DIAMOND : Material.EMERALD;
            nameColor = ChatColor.GREEN;
        } else {
            material = holding.getUnrealizedPnL() < -100 ? Material.COAL : Material.REDSTONE;
            nameColor = ChatColor.RED;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(nameColor + holding.getSymbol());
        
        // Create detailed lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Shares Owned: " + ChatColor.WHITE + String.format("%.2f", holding.getQty()));
        lore.add(ChatColor.YELLOW + "Purchase Price: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getAvgCost()));
        lore.add(ChatColor.YELLOW + "Current Price: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getCurrentPrice()));
        lore.add(ChatColor.YELLOW + "Total Value: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getQty() * holding.getCurrentPrice()));
        lore.add("");
        
        // P&L information with color coding
        double pnl = holding.getUnrealizedPnL();
        double pnlPercent = holding.getUnrealizedPnLPercent();
        ChatColor pnlColor = pnl >= 0 ? ChatColor.GREEN : ChatColor.RED;
        String pnlArrow = pnl >= 0 ? "▲" : "▼";
        
        lore.add(ChatColor.YELLOW + "Gain/Loss from Purchase:");
        lore.add(pnlColor + pnlArrow + " $" + String.format("%.2f", Math.abs(pnl)) + " (" + String.format("%.1f%%", Math.abs(pnlPercent)) + ")");
        lore.add("");
        lore.add(ChatColor.GRAY + "Right-click to sell all shares");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Adds navigation buttons
     */
    private void addNavigationButtons() {
        // Back to market button
        ItemStack marketItem = new ItemStack(Material.COMPASS);
        ItemMeta marketMeta = marketItem.getItemMeta();
        marketMeta.setDisplayName(ChatColor.AQUA + "Back to Market");
        List<String> marketLore = new ArrayList<>();
        marketLore.add(ChatColor.GRAY + "Return to market overview");
        marketMeta.setLore(marketLore);
        marketItem.setItemMeta(marketMeta);
        inventory.setItem(45, marketItem);
        
        // Refresh button
        ItemStack refreshItem = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.YELLOW + "Refresh Portfolio");
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add(ChatColor.GRAY + "Update portfolio values");
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        inventory.setItem(49, refreshItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close Portfolio");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Close this interface");
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
     * Gets the holding symbol from an inventory slot
     */
    public String getHoldingSymbolFromSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        return ChatColor.stripColor(displayName);
    }
}