package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.application.queries.QueryService;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
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
 * Portfolio GUI showing player's holdings and assets
 */
public class PortfolioGUI implements InventoryHolder {
    
    private static final Logger logger = Logger.getLogger(PortfolioGUI.class.getName());
    
    private final Player player;
    private final QueryService queryService;
    private final HoldingsService holdingsService;
    private final WalletService walletService;
    private final GUIConfigManager guiConfig;
    private final Inventory inventory;
    
    public PortfolioGUI(Player player, QueryService queryService, HoldingsService holdingsService, WalletService walletService, GUIConfigManager guiConfig) {
        this.player = player;
        this.queryService = queryService;
        this.holdingsService = holdingsService;
        this.walletService = walletService;
        this.guiConfig = guiConfig;
        
        int guiSize = guiConfig.getInt("portfolio.size", 54);
        String title = guiConfig.getString("portfolio.title", "&6Portfolio - {player_name}")
            .replace("{player_name}", player.getName());
        
        this.inventory = Bukkit.createInventory(this, guiSize, 
            ChatUT.serialize(ChatUT.hexComp(title)));
        
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
            String errorMsg = guiConfig.getString("portfolio.error_message", "&cFailed to load portfolio data.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
    
    /**
     * Adds wallet information
     */
    private void addWalletInfo() {
        try {
            Material walletMaterial = guiConfig.getItemMaterial("portfolio.wallet", Material.GOLD_INGOT);
            int walletSlot = guiConfig.getItemSlot("portfolio.wallet", 0);
            ItemStack walletItem = new ItemStack(walletMaterial);
            ItemMeta meta = walletItem.getItemMeta();
            meta.setDisplayName(guiConfig.getItemNameString("portfolio.wallet"));
            
            double balance = walletService.getBalance(player.getUniqueId().toString());
            List<String> lorePatt = guiConfig.getItemLoreStrings("portfolio.wallet");
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
     * Adds portfolio summary
     */
    private void addPortfolioSummary() {
        try {
            Material summaryMaterial = guiConfig.getItemMaterial("portfolio.summary", Material.EMERALD);
            int summarySlot = guiConfig.getItemSlot("portfolio.summary", 8);
            ItemStack summaryItem = new ItemStack(summaryMaterial);
            ItemMeta meta = summaryItem.getItemMeta();
            meta.setDisplayName(guiConfig.getItemNameString("portfolio.summary"));
            
            String playerUuid = player.getUniqueId().toString();
            double walletBalance = walletService.getBalance(playerUuid);
            double portfolioValue = holdingsService.getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;
            
            List<String> lorePatt = guiConfig.getItemLoreStrings("portfolio.summary");
            List<String> lore = new ArrayList<>();
            
            for (String line : lorePatt) {
                String processedLine = line
                    .replace("{portfolio_value}", String.format("%.2f", portfolioValue))
                    .replace("{cash_balance}", String.format("%.2f", walletBalance))
                    .replace("{total_assets}", String.format("%.2f", totalAssets));
                lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            meta.setLore(lore);
            summaryItem.setItemMeta(meta);
            inventory.setItem(summarySlot, summaryItem);
            
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
                Material noHoldingsMat = guiConfig.getItemMaterial("portfolio.no_holdings", Material.BARRIER);
                int noHoldingsSlot = guiConfig.getItemSlot("portfolio.no_holdings", 22);
                ItemStack noHoldings = new ItemStack(noHoldingsMat);
                ItemMeta meta = noHoldings.getItemMeta();
                meta.setDisplayName(guiConfig.getItemNameString("portfolio.no_holdings"));
                meta.setLore(guiConfig.getItemLoreStrings("portfolio.no_holdings"));
                noHoldings.setItemMeta(meta);
                inventory.setItem(noHoldingsSlot, noHoldings);
            }
            
            // Fill remaining slots with glass panes
            Material fillerMat = guiConfig.getItemMaterial("portfolio.filler", Material.GRAY_STAINED_GLASS_PANE);
            String fillerName = guiConfig.getString("portfolio.filler.name", " ");
            for (int i = slot; i < 45; i++) {
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(fillerName)));
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
        String colorCode;
        
        if (holding.getUnrealizedPnL() >= 0) {
            String highMat = guiConfig.getString("portfolio.holding_item.profit_materials.high", "DIAMOND");
            String medMat = guiConfig.getString("portfolio.holding_item.profit_materials.medium", "EMERALD");
            material = holding.getUnrealizedPnL() > 100 ? 
                Material.valueOf(highMat) : Material.valueOf(medMat);
            colorCode = guiConfig.getString("portfolio.holding_item.profit_color", "&a");
        } else {
            String highMat = guiConfig.getString("portfolio.holding_item.loss_materials.high", "COAL");
            String medMat = guiConfig.getString("portfolio.holding_item.loss_materials.medium", "REDSTONE");
            material = holding.getUnrealizedPnL() < -100 ? 
                Material.valueOf(highMat) : Material.valueOf(medMat);
            colorCode = guiConfig.getString("portfolio.holding_item.loss_color", "&c");
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Set display name
        meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(colorCode + holding.getSymbol())));
        
        // Create detailed lore
        List<String> lorePatt = guiConfig.getItemLoreStrings("portfolio.holding_item");
        List<String> lore = new ArrayList<>();
        
        // P&L information with color coding
        double pnl = holding.getUnrealizedPnL();
        double pnlPercent = holding.getUnrealizedPnLPercent();
        String pnlColor = pnl >= 0 ? 
            guiConfig.getString("portfolio.holding_item.profit_color", "&a") :
            guiConfig.getString("portfolio.holding_item.loss_color", "&c");
        String pnlArrow = pnl >= 0 ? "▲" : "▼";
        
        for (String line : lorePatt) {
            String processedLine = line
                .replace("{qty}", String.format("%.2f", holding.getQty()))
                .replace("{avg_cost}", String.format("%.2f", holding.getAvgCost()))
                .replace("{current_price}", String.format("%.2f", holding.getCurrentPrice()))
                .replace("{total_value}", String.format("%.2f", holding.getQty() * holding.getCurrentPrice()))
                .replace("{pnl_color}", pnlColor)
                .replace("{pnl_arrow}", pnlArrow)
                .replace("{pnl_abs}", String.format("%.2f", Math.abs(pnl)))
                .replace("{pnl_percent}", String.format("%.1f", Math.abs(pnlPercent)));
            lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Adds navigation buttons
     */
    private void addNavigationButtons() {
        // Back to market button
        addButton("back_to_market");
        
        // Refresh button
        addButton("refresh");
        
        // Close button
        addButton("close");
    }
    
    /**
     * Helper method to add a button from config
     */
    private void addButton(String buttonName) {
        String path = "portfolio." + buttonName;
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
     * Gets the holding symbol from an inventory slot
     */
    public String getHoldingSymbolFromSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        return ChatUT.extractText(displayName);
    }
}