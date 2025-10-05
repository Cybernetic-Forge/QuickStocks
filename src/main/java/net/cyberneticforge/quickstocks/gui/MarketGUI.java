package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Professional Market GUI for QuickStocks
 * Displays market data in an interactive inventory interface
 */
public class MarketGUI implements InventoryHolder {

    private static final Logger logger = Logger.getLogger(MarketGUI.class.getName());

    private final Inventory inventory;
    private final Player player;

    public MarketGUI(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, QuickStocksPlugin.getGuiConfig().getTitle("market"));
        setupGUI();
    }

    @Override
    public @NotNull Inventory getInventory() {
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
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("market.error_message", "&cFailed to load market data. Please try again.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }

    /**
     * Adds portfolio information button
     */
    private void addPortfolioInfoButton() {
        try {
            Material portfolioMat = QuickStocksPlugin.getGuiConfig().getItemMaterial("market.portfolio_overview", Material.BOOK);
            int portfolioSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("market.portfolio_overview", 0);
            ItemStack portfolioItem = new ItemStack(portfolioMat);
            ItemMeta meta = portfolioItem.getItemMeta();
            meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("market.portfolio_overview"));

            String playerUuid = player.getUniqueId().toString();
            double walletBalance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
            double portfolioValue = QuickStocksPlugin.getHoldingsService().getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;

            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.portfolio_overview",
                    new Replaceable("{cash_balance}", String.format("%.2f", walletBalance)),
                    new Replaceable("{portfolio_value}", String.format("%.2f", portfolioValue)),
                    new Replaceable("{total_assets}", String.format("%.2f", totalAssets)));
            meta.lore(lore);
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
            Material walletMat = QuickStocksPlugin.getGuiConfig().getItemMaterial("market.wallet", Material.GOLD_INGOT);
            int walletSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("market.wallet", 8);
            ItemStack walletItem = new ItemStack(walletMat);
            ItemMeta meta = walletItem.getItemMeta();
            meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("market.wallet"));

            double balance = QuickStocksPlugin.getWalletService().getBalance(player.getUniqueId().toString());
            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.wallet", new Replaceable("{balance}", String.format("%.2f", balance)));
            meta.lore(lore);
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
            List<Company> companiesOnMarket = QuickStocksPlugin.getCompanyService().getCompaniesOnMarket();

            int slot = 9; // Start from second row
            for (Company company : companiesOnMarket) {
                if (slot >= 45) break; // Leave bottom row for navigation

                ItemStack companyItem = createCompanyItem(company);
                inventory.setItem(slot, companyItem);
                slot++;
            }

            // Fill empty slots with barrier blocks to indicate no more stocks
            Material noCompMat = QuickStocksPlugin.getGuiConfig().getItemMaterial("market.no_companies", Material.GRAY_STAINED_GLASS_PANE);
            String noCompName = QuickStocksPlugin.getGuiConfig().getConfig().getString("market.no_companies.name", "&7No Company Shares Available");
            for (int i = slot; i < 45; i++) {
                ItemStack emptySlot = new ItemStack(noCompMat);
                ItemMeta meta = emptySlot.getItemMeta();
                meta.displayName(ChatUT.hexComp(noCompName));
                emptySlot.setItemMeta(meta);
                inventory.setItem(i, emptySlot);
            }

        } catch (Exception e) {
            logger.warning("Error adding companies to GUI: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
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
        Component name = QuickStocksPlugin.getGuiConfig().getItemName("market.company_item", new Replaceable("{company_name}", displayName), new Replaceable("{symbol}", symbol));
        meta.displayName(name);

        // Create detailed lore
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.company_item",
                new Replaceable("{company_name}", displayName),
                new Replaceable("{symbol}", symbol),
                new Replaceable("{type}", type),
                new Replaceable("{balance}", String.format("%.2f", balance)),
                new Replaceable("{market_percentage}", String.format("%.1f", company.getMarketPercentage()))
        );
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Determines the appropriate material for a company based on type
     */
    private Material getMaterialForCompany(String type) {
        if (type == null) type = "default";

        // Try to get material from config
        String materialName = QuickStocksPlugin.getGuiConfig().getConfig().getString("market.company_item.materials." + type.toLowerCase(), "PAPER");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage("Invalid material '" + materialName + "' for company type '" + type + "', using PAPER");
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
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial(path, Material.STONE);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot(path, 0);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName(path));
        meta.lore(QuickStocksPlugin.getGuiConfig().getItemLore(path));
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