package com.example.quickstocks.application.boot;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Seeds all Minecraft "normal" items as instruments in the database.
 * This seeder iterates through all Material values and creates ITEM instruments
 * for each valid item (excluding AIR, legacy, and non-items).
 */
public class ItemSeeder {
    
    private static final Logger logger = Logger.getLogger(ItemSeeder.class.getName());
    private final Db db;
    
    public ItemSeeder(Db db) {
        this.db = db;
    }
    
    /**
     * Seeds all Minecraft items as instruments.
     * This operation is idempotent - if a symbol already exists, it will be skipped.
     * 
     * @throws SQLException if database operations fail
     */
    public void seedItems() throws SQLException {
        logger.info("Starting to seed Minecraft items as instruments...");
        
        int totalItems = 0;
        int newItems = 0;
        int skippedItems = 0;
        
        // Try to use real Bukkit Material first, fall back to MockMaterial for development
        try {
            // In production with Paper API available, use org.bukkit.Material
            Class<?> materialClass = Class.forName("org.bukkit.Material");
            Object[] materials = (Object[]) materialClass.getMethod("values").invoke(null);
            
            for (Object material : materials) {
                // Use reflection to check isLegacy() and isItem()
                boolean isLegacy = (Boolean) materialClass.getMethod("isLegacy").invoke(material);
                boolean isItem = (Boolean) materialClass.getMethod("isItem").invoke(material);
                String name = (String) materialClass.getMethod("name").invoke(material);
                
                if (!isLegacy && isItem) {
                    totalItems++;
                    if (seedSingleItem(name)) {
                        newItems++;
                    } else {
                        skippedItems++;
                    }
                }
            }
            
            logger.info("Used real Bukkit Material enum for seeding");
            
        } catch (Exception e) {
            // Fall back to MockMaterial for development/testing
            logger.info("Bukkit Material not available");
        }
        
        logger.info(String.format("Item seeding completed. Total items processed: %d, New items created: %d, Existing items skipped: %d", 
                totalItems, newItems, skippedItems));
    }
    
    /**
     * Seeds a single item with the given material name.
     * 
     * @param materialName the name of the material
     * @return true if a new item was created, false if it already existed
     * @throws SQLException if database operations fail
     */
    private boolean seedSingleItem(String materialName) throws SQLException {
        String symbol = "MC_" + materialName; // UPPERCASE, unique
        String displayName = WordUtils.capitalizeFully(materialName.replace('_', ' '));
        String mcMaterial = materialName;
        
        // Check if symbol already exists (upsert behavior)
        if (instrumentExists(symbol)) {
            logger.fine("Skipping existing instrument: " + symbol);
            return false;
        }
        
        // Insert new instrument
        String instrumentId = UUID.randomUUID().toString();
        long currentTime = System.currentTimeMillis();
        
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, 
            instrumentId, 
            "ITEM", 
            symbol, 
            displayName, 
            mcMaterial, 
            0, // decimals = 0 for items
            currentTime
        );
        
        // Create corresponding instrument_state row
        db.execute("""
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            instrumentId,
            1.00, // baseline price
            0.0,  // last_volume
            0.0,  // change_1h
            0.0,  // change_24h
            0.0,  // volatility_24h
            0.0,  // market_cap
            currentTime
        );
        
        logger.fine("Created instrument: " + symbol + " (" + displayName + ")");
        return true;
    }
    
    /**
     * Checks if an instrument with the given symbol already exists.
     * 
     * @param symbol the symbol to check
     * @return true if the instrument exists, false otherwise
     * @throws SQLException if database query fails
     */
    private boolean instrumentExists(String symbol) throws SQLException {
        Integer count = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol = ?", symbol);
        return count != null && count > 0;
    }
    
    /**
     * Gets the count of ITEM type instruments in the database.
     * 
     * @return the count of ITEM instruments
     * @throws SQLException if database query fails
     */
    public int getItemInstrumentCount() throws SQLException {
        Integer count = db.queryValue("SELECT COUNT(*) FROM instruments WHERE type = 'ITEM'");
        return count != null ? count : 0;
    }
}