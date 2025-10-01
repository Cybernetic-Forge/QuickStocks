package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.application.queries.QueryService;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.TradingService;
import net.cyberneticforge.quickstocks.core.services.WalletService;
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
    private final CompanyService companyService;
    private final Inventory inventory;
    private final Player player;
    
    public MarketGUI(Player player, QueryService queryService, TradingService tradingService, 
                     HoldingsService holdingsService, WalletService walletService, CompanyService companyService) {
        this.player = player;
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.companyService = companyService;
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
            // Get companies that are on the market instead of predefined stocks
            List<Company> companiesOnMarket = companyService.getCompaniesOnMarket();
            
            int slot = 9; // Start from second row
            for (Company company : companiesOnMarket) {
                if (slot >= 45) break; // Leave bottom row for navigation
                
                ItemStack companyItem = createCompanyItem(company);
                inventory.setItem(slot, companyItem);
                slot++;
            }
            
            // Fill empty slots with barrier blocks to indicate no more stocks
            for (int i = slot; i < 45; i++) {
                ItemStack emptySlot = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = emptySlot.getItemMeta();
                meta.setDisplayName(ChatColor.GRAY + "No Company Shares Available");
                emptySlot.setItemMeta(meta);
                inventory.setItem(i, emptySlot);
            }
            
        } catch (Exception e) {
            logger.warning("Error adding companies to GUI: " + e.getMessage());
        }
    }
    
    /**
     * Creates an ItemStack representing a company share
     */
    private ItemStack createCompanyItem(Company company) {
        String symbol = company.getSymbol();
        String displayName = company.getName();
        String type = company.getType();
        double balance = company.getBalance();
        
        // Handle null values with defaults
        if (symbol == null) symbol = "UNKNOWN";
        if (displayName == null) displayName = "Unknown Company";
        if (type == null) type = "other";
        
        // Choose material based on company type
        Material material = getMaterialForCompany(type);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatColor.GREEN + displayName + " (" + symbol + ")");
        
        // Create detailed lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Company Balance: " + ChatColor.WHITE + "$" + String.format("%.2f", balance));
        lore.add(ChatColor.YELLOW + "Market Percentage: " + ChatColor.WHITE + String.format("%.1f%%", company.getMarketPercentage()));
        lore.add(ChatColor.YELLOW + "Type: " + ChatColor.GRAY + type);
        lore.add("");
        lore.add(ChatColor.AQUA + "This is a company share");
        lore.add(ChatColor.GRAY + "Buy shares with: /company buyshares " + displayName + " <qty>");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Determines the appropriate material for a company based on type
     */
    private Material getMaterialForCompany(String type) {
        if (type == null) type = "other";
        
        // Base material on type
        switch (type.toLowerCase()) {
            case "tech":
            case "technology":
                return Material.REDSTONE;
            case "finance":
            case "financial":
                return Material.EMERALD;
            case "retail":
            case "consumer":
                return Material.CHEST;
            case "manufacturing":
                return Material.IRON_INGOT;
            case "agriculture":
                return Material.WHEAT;
            default:
                return Material.PAPER;
        }
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