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
        
        // In production, this would be Material.values()
        // For now, using MockMaterial for development
        for (MockMaterial material : MockMaterial.values()) {
            // Filter out non-items: AIR, legacy, and non-items
            if (!material.isLegacy() && material.isItem()) {
                totalItems++;
                
                String symbol = "MC_" + material.name(); // UPPERCASE, unique
                String displayName = WordUtils.capitalizeFully(material.name().replace('_', ' '));
                String mcMaterial = material.name();
                
                // Check if symbol already exists (upsert behavior)
                if (instrumentExists(symbol)) {
                    skippedItems++;
                    logger.fine("Skipping existing instrument: " + symbol);
                    continue;
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
                
                newItems++;
                logger.fine("Created instrument: " + symbol + " (" + displayName + ")");
            }
        }
        
        logger.info(String.format("Item seeding completed. Total items processed: %d, New items created: %d, Existing items skipped: %d", 
                totalItems, newItems, skippedItems));
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