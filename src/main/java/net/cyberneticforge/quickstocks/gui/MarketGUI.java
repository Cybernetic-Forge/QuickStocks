package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Crypto;
import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
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

import java.sql.SQLException;
import java.util.*;

/**
 * Professional Market GUI for QuickStocks
 * Displays market data in an interactive inventory interface
 */
@SuppressWarnings("deprecation")
public class MarketGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    /**
     * Filter modes for the market GUI
     */
    public enum FilterMode {
        ALL,              // Show all instrument types
        COMPANY_SHARES,   // Show only company shares
        CRYPTO_SHARES,    // Show only cryptocurrencies
        ITEM_SHARES       // Show only item instruments
    }
    
    /**
     * Represents an instrument in a slot with its metadata
     */
    public static class SlotInstrument {
        public final String symbol;
        public final String type; // "COMPANY", "CRYPTO", "ITEM"
        public final Object data; // Company, Crypto, or Instrument object
        
        public SlotInstrument(String symbol, String type, Object data) {
            this.symbol = symbol;
            this.type = type;
            this.data = data;
        }
    }

    private final Inventory inventory;
    private final Player player;
    /**
     * -- GETTER --
     *  Gets the current filter mode
     */
    @Getter
    private FilterMode filterMode = FilterMode.ALL;
    
    // Map of slot number to instrument data for efficient click handling
    private final Map<Integer, SlotInstrument> slotInstrumentMap = new HashMap<>();

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
            addPortfolioInfoButton();
            addFilterButton();
            addWalletInfoButton();
            addStocksToGUI();
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
     * Adds the filter button to toggle between ALL/SHARES/CRYPTO views
     */
    private void addFilterButton() {
        try {
            // Get slot from config
            int filterSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("market.filter", 4);
            
            // Determine config path based on filter mode
            String modePath = switch (filterMode) {
                case ALL -> "market.filter.all";
                case COMPANY_SHARES -> "market.filter.company_shares";
                case CRYPTO_SHARES -> "market.filter.crypto_shares";
                case ITEM_SHARES -> "market.filter.item_shares";
            };
            
            // Get material, name, and lore from config
            Material filterMat = QuickStocksPlugin.getGuiConfig().getItemMaterial(modePath, Material.COMPASS);
            Component filterName = QuickStocksPlugin.getGuiConfig().getItemName(modePath);
            List<Component> filterLore = QuickStocksPlugin.getGuiConfig().getItemLore(modePath);

            ItemStack filterItem = new ItemStack(filterMat);
            ItemMeta meta = filterItem.getItemMeta();
            meta.displayName(filterName);
            meta.lore(filterLore);
            filterItem.setItemMeta(meta);
            
            inventory.setItem(filterSlot, filterItem);

        } catch (Exception e) {
            logger.warning("Error adding filter button: " + e.getMessage());
        }
    }

    /**
     * Adds stock items to the GUI based on the current filter mode
     */
    private void addStocksToGUI() {
        try {
            // Clear the slot map for refresh
            slotInstrumentMap.clear();
            
            int slot = 9; // Start from second row

            // Add company shares if filter allows
            if (filterMode == FilterMode.ALL || filterMode == FilterMode.COMPANY_SHARES) {
                List<Company> companiesOnMarket = QuickStocksPlugin.getCompanyService().getCompaniesOnMarket();
                
                for (Company company : companiesOnMarket) {
                    if (slot >= 45) break; // Leave bottom row for navigation

                    ItemStack companyItem = createCompanyItem(company);
                    inventory.setItem(slot, companyItem);
                    
                    // Store in map for click handling
                    slotInstrumentMap.put(slot, new SlotInstrument(
                        company.getSymbol(),
                        "COMPANY",
                        company
                    ));
                    
                    slot++;
                }
            }

            // Add cryptocurrencies if filter allows
            if (filterMode == FilterMode.ALL || filterMode == FilterMode.CRYPTO_SHARES) {
                List<Crypto> cryptos = QuickStocksPlugin.getCryptoService().getAllCryptos();
                
                for (Crypto crypto : cryptos) {
                    if (slot >= 45) break; // Leave bottom row for navigation

                    ItemStack cryptoItem = createCryptoItem(crypto);
                    inventory.setItem(slot, cryptoItem);
                    
                    // Store in map for click handling
                    slotInstrumentMap.put(slot, new SlotInstrument(
                        crypto.instrument().symbol(),
                        "CRYPTO",
                        crypto
                    ));
                    
                    slot++;
                }
            }

            // Add item instruments if filter allows
            if (filterMode == FilterMode.ALL || filterMode == FilterMode.ITEM_SHARES) {
                List<Instrument> itemInstruments = QuickStocksPlugin.getInstrumentPersistenceService().getInstrumentsByType("ITEM");
                
                for (Instrument itemInstrument : itemInstruments) {
                    if (slot >= 45) break; // Leave bottom row for navigation

                    ItemStack itemItem = createItemInstrumentItem(itemInstrument);
                    inventory.setItem(slot, itemItem);
                    
                    // Store in map for click handling
                    slotInstrumentMap.put(slot, new SlotInstrument(
                        itemInstrument.symbol(),
                        "ITEM",
                        itemInstrument
                    ));
                    
                    slot++;
                }
            }

            // Fill empty slots with barrier blocks
            String emptyPath = switch (filterMode) {
                case COMPANY_SHARES -> "market.no_companies";
                case CRYPTO_SHARES -> "market.no_crypto";
                case ITEM_SHARES -> "market.no_items";
                case ALL -> "market.no_items";
            };
            
            Material emptyMat = QuickStocksPlugin.getGuiConfig().getItemMaterial(emptyPath, Material.GRAY_STAINED_GLASS_PANE);
            Component emptyName = QuickStocksPlugin.getGuiConfig().getItemName(emptyPath);
            
            for (int i = slot; i < 45; i++) {
                ItemStack emptySlot = new ItemStack(emptyMat);
                ItemMeta meta = emptySlot.getItemMeta();
                meta.displayName(emptyName);
                emptySlot.setItemMeta(meta);
                inventory.setItem(i, emptySlot);
            }

        } catch (Exception e) {
            logger.warning("Error adding items to GUI: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
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
        
        // Calculate share price
        double sharePrice = 0.0;
        try {
            sharePrice = QuickStocksPlugin.getCompanyMarketService().calculateSharePrice(company);
        } catch (Exception e) {
            logger.debug("Failed to calculate share price for " + symbol + ": " + e.getMessage());
        }

        // Handle null values with defaults
        if (symbol == null) symbol = "UNKNOWN";
        if (displayName == null) displayName = "Unknown Company";
        if (type == null) type = "other";

        // Choose material based on company type
        Material material = getMaterialForCompany(type);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name
        Component name = QuickStocksPlugin.getGuiConfig().getItemName("market.company_item", 
            new Replaceable("{company_name}", displayName), 
            new Replaceable("{symbol}", symbol));
        meta.displayName(name);

        // Create detailed lore
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.company_item",
                new Replaceable("{company_name}", displayName),
                new Replaceable("{symbol}", symbol),
                new Replaceable("{type}", type),
                new Replaceable("{instrument_type}", "Company Share"),
                new Replaceable("{price}", String.format("%.2f", sharePrice)),
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
     * Creates an ItemStack representing a cryptocurrency
     */
    private ItemStack createCryptoItem(Crypto crypto) {
        String symbol = crypto.instrument().symbol();
        String displayName = crypto.instrument().displayName();
        double price = crypto.state().lastPrice();
        double change24h = crypto.state().change24h();
        double volume = crypto.state().lastVolume();

        // Handle null values with defaults
        if (symbol == null) symbol = "UNKNOWN";
        if (displayName == null) displayName = "Unknown Crypto";

        // Get material from config
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial("market.crypto_item", Material.GOLD_NUGGET);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Set display name from config
        Component name = QuickStocksPlugin.getGuiConfig().getItemName("market.crypto_item",
            new Replaceable("{symbol}", symbol),
            new Replaceable("{display_name}", displayName));
        meta.displayName(name);

        // Calculate color and symbol for change
        String changeColor = change24h >= 0 ? "&a" : "&c";
        String changeSymbol = change24h >= 0 ? "+" : "";
        
        // Get lore from config with replacements
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.crypto_item",
            new Replaceable("{symbol}", symbol),
            new Replaceable("{display_name}", displayName),
            new Replaceable("{instrument_type}", "Cryptocurrency"),
            new Replaceable("{price}", String.format("%.8f", price)),
            new Replaceable("{change_color}", changeColor),
            new Replaceable("{change_symbol}", changeSymbol),
            new Replaceable("{change_24h}", String.format("%.2f", change24h)),
            new Replaceable("{volume}", String.format("%.2f", volume))
        );
        
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Creates an ItemStack representing an item instrument
     */
    private ItemStack createItemInstrumentItem(Instrument instrument) {
        String symbol = instrument.symbol();
        String displayName = instrument.displayName();
        String mcMaterial = instrument.mcMaterial();
        
        // Handle null values with defaults
        if (symbol == null) symbol = "UNKNOWN";
        if (displayName == null) displayName = "Unknown Item";
        
        // Get the instrument state for price information
        try {
            Optional<InstrumentState> stateOpt = QuickStocksPlugin.getInstrumentPersistenceService()
                    .getInstrumentState(instrument.id());
            
            double price = stateOpt.map(InstrumentState::lastPrice).orElse(0.0);
            double change24h = stateOpt.map(InstrumentState::change24h).orElse(0.0);
            double volume = stateOpt.map(InstrumentState::lastVolume).orElse(0.0);
            
            // Use the actual Minecraft material for the display
            Material material = Material.PAPER; // Default fallback
            if (mcMaterial != null) {
                try {
                    material = Material.valueOf(mcMaterial);
                } catch (IllegalArgumentException e) {
                    logger.debug("Invalid material '" + mcMaterial + "' for item instrument, using PAPER");
                }
            }
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            // Set display name from config
            Component name = QuickStocksPlugin.getGuiConfig().getItemName("market.item_instrument",
                new Replaceable("{symbol}", symbol),
                new Replaceable("{display_name}", displayName));
            meta.displayName(name);
            
            // Calculate color and symbol for change
            String changeColor = change24h >= 0 ? "&a" : "&c";
            String changeSymbol = change24h >= 0 ? "+" : "";
            
            // Get lore from config with replacements
            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("market.item_instrument",
                new Replaceable("{symbol}", symbol),
                new Replaceable("{display_name}", displayName),
                new Replaceable("{instrument_type}", "Item Instrument"),
                new Replaceable("{price}", String.format("%.2f", price)),
                new Replaceable("{change_color}", changeColor),
                new Replaceable("{change_symbol}", changeSymbol),
                new Replaceable("{change_24h}", String.format("%.2f", change24h)),
                new Replaceable("{volume}", String.format("%.2f", volume))
            );
            
            meta.lore(lore);
            item.setItemMeta(meta);
            
            return item;
            
        } catch (Exception e) {
            logger.warning("Error creating item instrument display for " + symbol + ": " + e.getMessage());
            
            // Return a basic item on error
            Material material = Material.PAPER;
            if (mcMaterial != null) {
                try {
                    material = Material.valueOf(mcMaterial);
                } catch (IllegalArgumentException ignored) {}
            }
            
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ChatUT.hexComp("&e" + displayName + " (&7" + symbol + "&e)"));
            meta.lore(Arrays.asList(
                ChatUT.hexComp("&cError loading price data"),
                ChatUT.hexComp("&7Click to view details")
            ));
            item.setItemMeta(meta);
            
            return item;
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
     * Toggles the filter mode (ALL -> COMPANY_SHARES -> CRYPTO_SHARES -> ITEM_SHARES -> ALL)
     */
    public void toggleFilter() {
        filterMode = switch (filterMode) {
            case ALL -> FilterMode.COMPANY_SHARES;
            case COMPANY_SHARES -> FilterMode.CRYPTO_SHARES;
            case CRYPTO_SHARES -> FilterMode.ITEM_SHARES;
            case ITEM_SHARES -> FilterMode.ALL;
        };
        refresh();
    }

    /**
     * Gets the instrument from a slot (new HashMap-based approach)
     * @param slot The inventory slot number
     * @return The SlotInstrument for this slot, or null if not an instrument slot
     */
    public SlotInstrument getInstrumentFromSlot(int slot) {
        return slotInstrumentMap.get(slot);
    }
    
    /**
     * @deprecated Use {@link #getInstrumentFromSlot(int)} instead for better performance
     */
    @Deprecated
    public String getStockSymbolFromSlot(int slot) {
        SlotInstrument slotInstrument = slotInstrumentMap.get(slot);
        return slotInstrument != null ? slotInstrument.symbol : null;
    }
}