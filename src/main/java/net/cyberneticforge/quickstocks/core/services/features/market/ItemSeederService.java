package net.cyberneticforge.quickstocks.core.services.features.market;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for seeding common Minecraft items as tradeable instruments.
 * This allows players to trade items on the market.
 * 
 * Items are seeded as ITEM type instruments with initial prices based on rarity.
 */
public class ItemSeederService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    
    /**
     * Seeds common tradeable items into the database.
     * Only creates items that don't already exist.
     * 
     * @param overwrite If true, recreates existing items
     */
    public void seedCommonItems(boolean overwrite) throws SQLException {
        logger.info("Seeding common tradeable items...");
        
        Map<Material, Double> commonItems = getCommonTradeableItems();
        int created = 0;
        int skipped = 0;
        
        for (Map.Entry<Material, Double> entry : commonItems.entrySet()) {
            Material material = entry.getKey();
            double initialPrice = entry.getValue();
            
            boolean exists = checkInstrumentExists(material);
            
            if (exists && !overwrite) {
                skipped++;
                continue;
            }
            
            if (exists && overwrite) {
                deleteInstrument(material);
            }
            
            createItemInstrument(material, initialPrice);
            created++;
        }
        
        logger.info("Item seeding complete: " + created + " created, " + skipped + " skipped");
    }
    
    /**
     * Returns a map of common tradeable items with their initial prices.
     */
    private Map<Material, Double> getCommonTradeableItems() {
        Map<Material, Double> items = new HashMap<>();
        
        // Ores and minerals (high value)
        items.put(Material.DIAMOND, 100.0);
        items.put(Material.EMERALD, 80.0);
        items.put(Material.GOLD_INGOT, 50.0);
        items.put(Material.IRON_INGOT, 20.0);
        items.put(Material.COAL, 5.0);
        items.put(Material.COPPER_INGOT, 8.0);
        items.put(Material.NETHERITE_INGOT, 500.0);
        
        // Building blocks (medium value)
        items.put(Material.STONE, 1.0);
        items.put(Material.COBBLESTONE, 0.5);
        items.put(Material.OAK_LOG, 3.0);
        items.put(Material.SPRUCE_LOG, 3.0);
        items.put(Material.BIRCH_LOG, 3.0);
        items.put(Material.GLASS, 2.0);
        items.put(Material.BRICK, 5.0);
        
        // Food items (low-medium value)
        items.put(Material.BREAD, 4.0);
        items.put(Material.COOKED_BEEF, 6.0);
        items.put(Material.GOLDEN_APPLE, 50.0);
        items.put(Material.ENCHANTED_GOLDEN_APPLE, 300.0);
        
        // Redstone and tech (medium value)
        items.put(Material.REDSTONE, 10.0);
        items.put(Material.GLOWSTONE_DUST, 15.0);
        items.put(Material.ENDER_PEARL, 25.0);
        items.put(Material.BLAZE_ROD, 30.0);
        
        // Rare items (high value)
        items.put(Material.NETHER_STAR, 1000.0);
        items.put(Material.DRAGON_EGG, 5000.0);
        items.put(Material.ELYTRA, 750.0);
        items.put(Material.TOTEM_OF_UNDYING, 500.0);
        
        return items;
    }
    
    /**
     * Checks if an instrument already exists for this material.
     */
    private boolean checkInstrumentExists(Material material) throws SQLException {
        Map<String, Object> result = database.queryOne(
            "SELECT id FROM instruments WHERE mc_material = ?",
            material.name()
        );
        return result != null;
    }
    
    /**
     * Deletes an instrument for this material.
     */
    private void deleteInstrument(Material material) throws SQLException {
        // Get instrument ID first
        Map<String, Object> result = database.queryOne(
            "SELECT id FROM instruments WHERE mc_material = ?",
            material.name()
        );
        
        if (result != null) {
            String instrumentId = (String) result.get("id");
            
            // Delete in order (foreign key constraints)
            database.execute("DELETE FROM instrument_price_history WHERE instrument_id = ?", instrumentId);
            database.execute("DELETE FROM instrument_state WHERE instrument_id = ?", instrumentId);
            database.execute("DELETE FROM instruments WHERE id = ?", instrumentId);
            
            logger.debug("Deleted existing instrument for " + material.name());
        }
    }
    
    /**
     * Creates an ITEM type instrument for a Minecraft material.
     */
    private void createItemInstrument(Material material, double initialPrice) throws SQLException {
        String instrumentId = UUID.randomUUID().toString();
        String symbol = "MC_" + material.name();
        String displayName = formatDisplayName(material.name());
        long now = System.currentTimeMillis();
        
        // Create instrument
        database.execute(
            """
            INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            instrumentId,
            "ITEM",
            symbol,
            displayName,
            material.name(),
            0,  // No decimals for items
            null,  // System created
            now
        );
        
        // Create initial state
        database.execute(
            """
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            instrumentId,
            initialPrice,
            0.0,
            0.0,
            0.0,
            0.5,  // Medium volatility
            initialPrice * 10000,  // Assume 10k items in circulation
            now
        );
        
        // Create initial price history entry
        String historyId = UUID.randomUUID().toString();
        database.execute(
            """
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            historyId,
            instrumentId,
            now,
            initialPrice,
            0.0,
            "INITIAL_SEED"
        );
        
        logger.debug("Created item instrument: " + symbol + " at $" + initialPrice);
    }
    
    /**
     * Formats a material name into a display name.
     * Example: DIAMOND_SWORD -> Diamond Sword
     */
    private String formatDisplayName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            result.append(part.substring(1));
        }
        
        return result.toString();
    }
    
    /**
     * Removes all seeded items from the database.
     * Use with caution - this will delete all ITEM type instruments.
     */
    public void clearAllItems() throws SQLException {
        logger.info("Clearing all seeded items...");
        
        // Get all ITEM instruments
        var results = database.query("SELECT id FROM instruments WHERE type = 'ITEM'");
        
        int deleted = 0;
        for (var row : results) {
            String instrumentId = (String) row.get("id");
            
            // Delete cascading data
            database.execute("DELETE FROM instrument_price_history WHERE instrument_id = ?", instrumentId);
            database.execute("DELETE FROM instrument_state WHERE instrument_id = ?", instrumentId);
            database.execute("DELETE FROM instruments WHERE id = ?", instrumentId);
            
            deleted++;
        }
        
        logger.info("Cleared " + deleted + " item instruments");
    }
}
