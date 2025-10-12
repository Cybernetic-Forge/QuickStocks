package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.core.services.HoldingsService;
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

import java.util.ArrayList;
import java.util.List;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Portfolio GUI showing player's holdings and assets
 */
@SuppressWarnings("deprecation")
public class PortfolioGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Player player;
    private final Inventory inventory;

    public PortfolioGUI(Player player) {
        this.player = player;

        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("portfolio.size", 54);
        String title = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.title", "&6Portfolio - {player_name}")
                .replace("{player_name}", player.getName());

        this.inventory = Bukkit.createInventory(this, guiSize, ChatUT.hexComp(title));

        setupGUI();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets up the portfolio GUI
     */
    private void setupGUI() {
        try {
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
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.error_message", "&cFailed to load portfolio data.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }

    /**
     * Adds wallet information
     */
    private void addWalletInfo() {
        try {
            Material walletMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("portfolio.wallet", Material.GOLD_INGOT);
            int walletSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("portfolio.wallet", 0);
            ItemStack walletItem = new ItemStack(walletMaterial);
            ItemMeta meta = walletItem.getItemMeta();
            meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("portfolio.wallet"));

            double balance = QuickStocksPlugin.getWalletService().getBalance(player.getUniqueId().toString());
            List<String> lorePatt = QuickStocksPlugin.getGuiConfig().getConfig().getStringList("portfolio.wallet.lore");
            List<Component> lore = new ArrayList<>();

            for (String line : lorePatt) {
                String processedLine = line.replace("{balance}", String.format("%.2f", balance));
                lore.add(ChatUT.hexComp(processedLine));
            }

            meta.lore(lore);
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
            Material summaryMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("portfolio.summary", Material.EMERALD);
            int summarySlot = QuickStocksPlugin.getGuiConfig().getItemSlot("portfolio.summary", 8);
            ItemStack summaryItem = new ItemStack(summaryMaterial);
            ItemMeta meta = summaryItem.getItemMeta();
            meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("portfolio.summary"));

            String playerUuid = player.getUniqueId().toString();
            double walletBalance = QuickStocksPlugin.getWalletService().getBalance(playerUuid);
            double portfolioValue = QuickStocksPlugin.getHoldingsService().getPortfolioValue(playerUuid);
            double totalAssets = walletBalance + portfolioValue;

            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("portfolio.summary",
                    new Replaceable("{portfolio_value}", String.format("%.2f", portfolioValue)),
                    new Replaceable("{cash_balance}", String.format("%.2f", walletBalance)),
                    new Replaceable("{total_assets}", String.format("%.2f", totalAssets)));
            meta.lore(lore);
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
            List<HoldingsService.Holding> holdings = QuickStocksPlugin.getHoldingsService().getHoldings(player.getUniqueId().toString());

            int slot = 9; // Start from second row
            for (HoldingsService.Holding holding : holdings) {
                if (slot >= 45) break; // Leave bottom row for navigation

                ItemStack holdingItem = createHoldingItem(holding);
                inventory.setItem(slot, holdingItem);
                slot++;
            }

            // Fill empty slots if no holdings
            if (holdings.isEmpty()) {
                Material noHoldingsMat = QuickStocksPlugin.getGuiConfig().getItemMaterial("portfolio.no_holdings", Material.BARRIER);
                int noHoldingsSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("portfolio.no_holdings", 22);
                ItemStack noHoldings = new ItemStack(noHoldingsMat);
                ItemMeta meta = noHoldings.getItemMeta();
                meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("portfolio.no_holdings"));
                meta.lore(QuickStocksPlugin.getGuiConfig().getItemLore("portfolio.no_holdings"));
                noHoldings.setItemMeta(meta);
                inventory.setItem(noHoldingsSlot, noHoldings);
            }

            // Fill remaining slots with glass panes
            Material fillerMat = QuickStocksPlugin.getGuiConfig().getItemMaterial("portfolio.filler", Material.GRAY_STAINED_GLASS_PANE);
            String fillerName = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.filler.name", " ");
            for (int i = slot; i < 45; i++) {
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                meta.displayName(ChatUT.hexComp(fillerName));
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
            String highMat = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.profit_materials.high", "DIAMOND");
            String medMat = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.profit_materials.medium", "EMERALD");
            material = holding.getUnrealizedPnL() > 100 ?
                    Material.valueOf(highMat) : Material.valueOf(medMat);
            colorCode = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.profit_color", "&a");
        } else {
            String highMat = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.loss_materials.high", "COAL");
            String medMat = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.loss_materials.medium", "REDSTONE");
            material = holding.getUnrealizedPnL() < -100 ?
                    Material.valueOf(highMat) : Material.valueOf(medMat);
            colorCode = QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.loss_color", "&c");
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        meta.displayName(ChatUT.hexComp(colorCode + holding.symbol()));

        // P&L information with color coding
        double pnl = holding.getUnrealizedPnL();
        double pnlPercent = holding.getUnrealizedPnLPercent();
        String pnlColor = pnl >= 0 ?
                QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.profit_color", "&a") :
                QuickStocksPlugin.getGuiConfig().getConfig().getString("portfolio.holding_item.loss_color", "&c");
        String pnlArrow = pnl >= 0 ? "▲" : "▼";
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("portfolio.holding_item",
                new Replaceable("{qty}", String.format("%.2f", holding.qty())),
                new Replaceable("{avg_cost}", String.format("%.2f", holding.avgCost())),
                new Replaceable("{current_price}", String.format("%.2f", holding.currentPrice())),
                new Replaceable("{total_value}", String.format("%.2f", holding.qty() * holding.currentPrice())),
                new Replaceable("{pnl_color}", pnlColor),
                new Replaceable("{pnl_arrow}", pnlArrow),
                new Replaceable("{pnl_abs}", String.format("%.2f", Math.abs(pnl))),
                new Replaceable("{pnl_percent}", String.format("%.1f", Math.abs(pnlPercent)))
        );
        meta.lore(lore);
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
     * Gets the holding symbol from an inventory slot
     */
    @SuppressWarnings("deprecation")
    public String getHoldingSymbolFromSlot(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        // TODO Proper gui handling
        String displayName = item.getItemMeta().getDisplayName();
        return ChatUT.extractText(displayName);
    }
}