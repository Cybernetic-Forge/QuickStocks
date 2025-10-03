package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.application.queries.QueryService;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
import net.cyberneticforge.quickstocks.core.services.TradingService;
import net.cyberneticforge.quickstocks.core.services.WalletService;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.cyberneticforge.quickstocks.utils.GUIConfigManager;
import org.bukkit.Bukkit;
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
    
    private final QueryService queryService;
    private final TradingService tradingService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final CompanyService companyService;
    private final GUIConfigManager guiConfig;
    private final Inventory inventory;
    private final Player player;
    
    public MarketGUI(Player player, QueryService queryService, TradingService tradingService, 
                     HoldingsService holdingsService, WalletService walletService, CompanyService companyService,
                     GUIConfigManager guiConfig) {
        this.player = player;
        this.queryService = queryService;
        this.tradingService = tradingService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.companyService = companyService;
        this.guiConfig = guiConfig;
        
        int guiSize = guiConfig.getInt("market.size", 54);
        String title = guiConfig.getString("market.title", "&2Market - QuickStocks");
        
        this.inventory = Bukkit.createInventory(this, guiSize, 
            ChatUT.serialize(ChatUT.hexComp(title)));
        
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
            String errorMsg = guiConfig.getString("market.error_message", "&cFailed to load market data. Please try again.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
    
    /**
     * Adds portfolio information button
     */
    private void addPortfolioInfoButton() {
        try {
            Material portfolioMat = guiConfig.getItemMaterial("market.portfolio_overview", Material.BOOK);
            int portfolioSlot = guiConfig.getItemSlot("market.portfolio_overview", 0);
            ItemStack portfolioItem = new ItemStack(portfolioMat);
            ItemMeta meta = portfolioItem.getItemMeta();
            meta.setDisplayName(guiConfig.getItemNameString("market.portfolio_overview"));
            
            String playerUuid = player.getUniqueId().toString();
            double walletBalance = walletService.getBalance(playerUuid);
            double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;
            
            List<String> lorePatt = guiConfig.getItemLoreStrings("market.portfolio_overview");
            List<String> lore = new ArrayList<>();
            
            for (String line : lorePatt) {
                String processedLine = line
                    .replace("{cash_balance}", String.format("%.2f", walletBalance))
                    .replace("{portfolio_value}", String.format("%.2f", portfolioValue))
                    .replace("{total_assets}", String.format("%.2f", totalAssets));
                lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            meta.setLore(lore);
            portfolioItem.setItemMeta(meta);
            inventory.setItem(portfolioSlot, portfolioItem);
            
        } catch (Exception e) {
            logger.warning("Error adding portfolio info: " + e.getMessage());
        }
    }
    
    /**
     * Adds wallet information button
     */
    private void addWalletInfoButton() {
        try {
            Material walletMat = guiConfig.getItemMaterial("market.wallet", Material.GOLD_INGOT);
            int walletSlot = guiConfig.getItemSlot("market.wallet", 8);
            ItemStack walletItem = new ItemStack(walletMat);
            ItemMeta meta = walletItem.getItemMeta();
            meta.setDisplayName(guiConfig.getItemNameString("market.wallet"));
            
            double balance = walletService.getBalance(player.getUniqueId().toString());
            List<String> lorePatt = guiConfig.getItemLoreStrings("market.wallet");
            List<String> lore = new ArrayList<>();
            
            for (String line : lorePatt) {
                String processedLine = line.replace("{balance}", String.format("%.2f", balance));
                lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            meta.setLore(lore);
            walletItem.setItemMeta(meta);
            inventory.setItem(walletSlot, walletItem);
            
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
            Material noCompMat = guiConfig.getItemMaterial("market.no_companies", Material.GRAY_STAINED_GLASS_PANE);
            String noCompName = guiConfig.getString("market.no_companies.name", "&7No Company Shares Available");
            for (int i = slot; i < 45; i++) {
                ItemStack emptySlot = new ItemStack(noCompMat);
                ItemMeta meta = emptySlot.getItemMeta();
                meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(noCompName)));
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
        String namePatt = guiConfig.getString("market.company_item.name", "&a{company_name} ({symbol})")
            .replace("{company_name}", displayName)
            .replace("{symbol}", symbol);
        meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(namePatt)));
        
        // Create detailed lore
        List<String> lorePatt = guiConfig.getItemLoreStrings("market.company_item");
        List<String> lore = new ArrayList<>();
        
        for (String line : lorePatt) {
            String processedLine = line
                .replace("{balance}", String.format("%.2f", balance))
                .replace("{market_percentage}", String.format("%.1f", company.getMarketPercentage()))
                .replace("{type}", type)
                .replace("{company_name}", displayName);
            lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Determines the appropriate material for a company based on type
     */
    private Material getMaterialForCompany(String type) {
        if (type == null) type = "other";
        
        // Try to get material from config
        String materialName = guiConfig.getString("market.company_item.materials." + type.toLowerCase(), null);
        if (materialName == null) {
            materialName = guiConfig.getString("market.company_item.materials.default", "PAPER");
        }
        
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid material '" + materialName + "' for company type '" + type + "', using PAPER");
            return Material.PAPER;
        }
    }
    
    /**
     * Adds navigation buttons at the bottom of the GUI
     */
    private void addNavigationButtons() {
        // Refresh button
        addButton("refresh");
        
        // Portfolio view button
        addButton("my_holdings");
        
        // Close button
        addButton("close");
    }
    
    /**
     * Helper method to add a button from config
     */
    private void addButton(String buttonName) {
        String path = "market." + buttonName;
        Material material = guiConfig.getItemMaterial(path, Material.STONE);
        int slot = guiConfig.getItemSlot(path, 0);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(guiConfig.getItemNameString(path));
        meta.setLore(guiConfig.getItemLoreStrings(path));
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
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
        String plainText = ChatUT.extractText(displayName);
        
        // Extract symbol from display name format: "DisplayName (SYMBOL)"
        int startIndex = plainText.lastIndexOf('(');
        int endIndex = plainText.lastIndexOf(')');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return plainText.substring(startIndex + 1, endIndex);
        }
        
        return null;
    }
}