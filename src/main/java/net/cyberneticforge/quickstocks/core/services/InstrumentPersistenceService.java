package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

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
    private final Map<String, Instrument> symbolToInstrumentMap = new HashMap<>(); // symbol -> instrument_id
    
    public InstrumentPersistenceService(Db database) {
        this.database = database;
        loadExistingMappings();
    }
    
    /**
     * Ensures a Stock is represented as an instrument in the database.
     * Returns the instrument ID for the stock.
     */
    public Instrument ensureInstrument(Stock stock) {
        String symbol = stock.getSymbol();
        Instrument instrument = symbolToInstrumentMap.get(symbol);
        
        if (instrument == null) {
            // Create new instrument
            instrument = createInstrument(stock);
            symbolToInstrumentMap.put(symbol, instrument);
        } else {
            // Update existing instrument if needed
            updateInstrument(instrument, stock);
        }
        
        return instrument;
    }
    
    /**
     * Creates a new instrument record for the given stock.
     */
    private Instrument createInstrument(Stock stock) {
        try {
            String instrumentId = UUID.randomUUID().toString();
            String type = "EQUITY"; // Assuming all Stock objects are equities
            String symbol = stock.getSymbol();
            String displayName = stock.getName();
            String material = null; // No Minecraft material for stocks
            double decimal = 2; // 2 decimal places for stock prices
            String createdBy = null; // Not created by a player
            long now = System.currentTimeMillis();
            Instrument instrument = new Instrument(instrumentId, type, symbol, displayName, material, decimal, createdBy, now);
            database.execute("""
                INSERT INTO instruments 
                (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    instrument.getInstrumentId(),
                    instrument.getType(),
                    instrument.getSymbol(),
                    instrument.getDisplayName(),
                    instrument.getMaterial(),
                    instrument.getDecimal(),
                    instrument.getCreatedBy(),
                    instrument.getCreatedAt()
            );
            
            logger.info("Created instrument: " + stock.getSymbol() + " -> " + instrumentId);
            return instrument;
            
        } catch (Exception e) {
            logger.severe("Failed to create instrument for " + stock.getSymbol() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create instrument", e);
        }
    }
    
    /**
     * Updates an existing instrument record if the stock data has changed.
     */
    private void updateInstrument(Instrument instrument, Stock stock) {
        try {
            // For now, we only update the display name if it has changed
            database.execute("""
                UPDATE instruments 
                SET display_name = ?
                WHERE id = ?
                """,
                stock.getName(),
                instrument.getInstrumentId()
            );
            
        } catch (Exception e) {
            logger.warning("Failed to update instrument " + instrument.getInstrumentId() + ": " + e.getMessage());
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
                String instrumentId = (String) row.get("id");
                String type = (String) row.get("type");
                String symbol = (String) row.get("symbol");
                String displayName = (String) row.get("display_name");
                String material = (String) row.get("mc_material");
                double decimal = ((Number) row.get("decimals")).doubleValue();
                String createdBy = (String) row.get("created_by");
                long createdAt = ((Number) row.get("created_at")).longValue();
                Instrument instrument = new Instrument(instrumentId, type, symbol, displayName, material, decimal, createdBy, createdAt);
                symbolToInstrumentMap.put(symbol, instrument);
            }
            
            logger.info("Loaded " + symbolToInstrumentMap.size() + " existing instrument mappings");
            
        } catch (Exception e) {
            logger.warning("Failed to load existing instrument mappings: " + e.getMessage());
            // Continue with empty mappings
        }
    }
    
    /**
     * Gets the instrument ID for a given stock symbol.
     * Returns null if the stock is not yet persisted.
     */
    public Instrument getInstrument(String symbol) {
        return symbolToInstrumentMap.get(symbol);
    }
    
    /**
     * Gets all known symbol->ID mappings.
     */
    public Map<String, Instrument> getAllInstruments() {
        return new HashMap<>(symbolToInstrumentMap);
    }

    /**
     * Represents a player's holding in an instrument.
     */
    public class Instrument {
        private final String instrumentId;
        private final String type;
        private final String symbol;
        private final String displayName;
        private final String material;
        private final double decimal;
        private final String createdBy;
        private final long createdAt;

        public Instrument(String instrumentId, String type, String symbol, String displayName,
                          String material, double decimal, String createdBy, long createdAt) {
            this.instrumentId = instrumentId;
            this.type = type;
            this.symbol = symbol;
            this.displayName = displayName;
            this.material = material;
            this.decimal = decimal;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public String getInstrumentId() {
            return instrumentId;
        }
        public String getType() {
            return type;
        }
        public String getSymbol() {
            return symbol;
        }
        public String getDisplayName() {
            return displayName;
        }
        public String getMaterial() {
            return material;
        }
        public double getDecimal() {
            return decimal;
        }
        public String getCreatedBy() {
            return createdBy;
        }
        public long getCreatedAt() {
            return createdAt;
        }
    }
}