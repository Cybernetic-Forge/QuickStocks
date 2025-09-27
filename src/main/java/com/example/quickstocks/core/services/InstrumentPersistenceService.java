package com.example.quickstocks.core.services;

import com.example.quickstocks.core.models.Stock;
import com.example.quickstocks.infrastructure.db.Db;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles the mapping and persistence of Stock objects to database instruments.
 */
public class InstrumentPersistenceService {
    
    private static final Logger logger = Logger.getLogger(InstrumentPersistenceService.class.getName());
    
    private final Db database;
    private final Map<String, String> symbolToIdMap = new HashMap<>(); // symbol -> instrument_id
    
    public InstrumentPersistenceService(Db database) {
        this.database = database;
        loadExistingMappings();
    }
    
    /**
     * Ensures a Stock is represented as an instrument in the database.
     * Returns the instrument ID for the stock.
     */
    public String ensureInstrument(Stock stock) {
        String symbol = stock.getSymbol();
        String instrumentId = symbolToIdMap.get(symbol);
        
        if (instrumentId == null) {
            // Create new instrument
            instrumentId = createInstrument(stock);
            symbolToIdMap.put(symbol, instrumentId);
        } else {
            // Update existing instrument if needed
            updateInstrument(instrumentId, stock);
        }
        
        return instrumentId;
    }
    
    /**
     * Creates a new instrument record for the given stock.
     */
    private String createInstrument(Stock stock) {
        try {
            String instrumentId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            
            database.execute("""
                INSERT INTO instruments 
                (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instrumentId,
                "EQUITY", // Assuming all Stock objects are equities
                stock.getSymbol(),
                stock.getName(),
                null, // No Minecraft material for stocks
                2, // 2 decimal places for stock prices
                null, // Not created by a player
                now
            );
            
            logger.info("Created instrument: " + stock.getSymbol() + " -> " + instrumentId);
            return instrumentId;
            
        } catch (Exception e) {
            logger.severe("Failed to create instrument for " + stock.getSymbol() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create instrument", e);
        }
    }
    
    /**
     * Updates an existing instrument record if the stock data has changed.
     */
    private void updateInstrument(String instrumentId, Stock stock) {
        try {
            // For now, we only update the display name if it has changed
            database.execute("""
                UPDATE instruments 
                SET display_name = ?
                WHERE id = ?
                """,
                stock.getName(),
                instrumentId
            );
            
        } catch (Exception e) {
            logger.warning("Failed to update instrument " + instrumentId + ": " + e.getMessage());
            // Non-critical error, continue
        }
    }
    
    /**
     * Loads existing symbol->ID mappings from the database.
     */
    private void loadExistingMappings() {
        try {
            var results = database.query("SELECT id, symbol FROM instruments WHERE type = 'EQUITY'");
            
            for (var row : results) {
                String id = (String) row.get("id");
                String symbol = (String) row.get("symbol");
                symbolToIdMap.put(symbol, id);
            }
            
            logger.info("Loaded " + symbolToIdMap.size() + " existing instrument mappings");
            
        } catch (Exception e) {
            logger.warning("Failed to load existing instrument mappings: " + e.getMessage());
            // Continue with empty mappings
        }
    }
    
    /**
     * Gets the instrument ID for a given stock symbol.
     * Returns null if the stock is not yet persisted.
     */
    public String getInstrumentId(String symbol) {
        return symbolToIdMap.get(symbol);
    }
    
    /**
     * Gets all known symbol->ID mappings.
     */
    public Map<String, String> getAllMappings() {
        return new HashMap<>(symbolToIdMap);
    }
}