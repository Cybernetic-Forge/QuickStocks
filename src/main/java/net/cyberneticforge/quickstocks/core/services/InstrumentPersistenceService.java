package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.core.model.PriceHistory;
import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the mapping and persistence of Stock objects to database instruments.
 */
@SuppressWarnings({"unused", "ConstantValue"})
public class InstrumentPersistenceService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final Map<String, Instrument> symbolToInstrumentMap = new HashMap<>(); // symbol -> Instrument
    private final Map<String, Instrument> idToInstrumentMap = new HashMap<>(); // id -> Instrument
    
    public InstrumentPersistenceService() {
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
            int decimals = 2; // 2 decimal places for stock prices
            String createdBy = null; // Not created by a player
            long now = System.currentTimeMillis();
            Instrument instrument = new Instrument(instrumentId, type, symbol, displayName, material, decimals, createdBy, now);
            database.execute("""
                INSERT INTO instruments\s
                (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
               \s""",
                    instrument.id(),
                    instrument.type(),
                    instrument.symbol(),
                    instrument.displayName(),
                    instrument.mcMaterial(),
                    instrument.decimals(),
                    instrument.createdBy(),
                    instrument.createdAt()
            );
            
            idToInstrumentMap.put(instrumentId, instrument);
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
                UPDATE instruments\s
                SET display_name = ?
                WHERE id = ?
               \s""",
                stock.getName(),
                instrument.id()
            );
            
        } catch (Exception e) {
            logger.warning("Failed to update instrument " + instrument.id() + ": " + e.getMessage());
            // Non-critical error, continue
        }
    }
    
    /**
     * Loads existing instrument mappings from the database.
     */
    private void loadExistingMappings() {
        try {
            var results = database.query("SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at FROM instruments");
            
            for (var row : results) {
                String instrumentId = (String) row.get("id");
                String type = (String) row.get("type");
                String symbol = (String) row.get("symbol");
                String displayName = (String) row.get("display_name");
                String material = (String) row.get("mc_material");
                int decimals = ((Number) row.get("decimals")).intValue();
                String createdBy = (String) row.get("created_by");
                long createdAt = ((Number) row.get("created_at")).longValue();
                Instrument instrument = new Instrument(instrumentId, type, symbol, displayName, material, decimals, createdBy, createdAt);
                symbolToInstrumentMap.put(symbol, instrument);
                idToInstrumentMap.put(instrumentId, instrument);
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
     * Gets all known symbol->Instrument mappings.
     */
    public Map<String, Instrument> getAllInstruments() {
        return new HashMap<>(symbolToInstrumentMap);
    }
    
    /**
     * Gets an instrument by its ID.
     * 
     * @param instrumentId The instrument ID
     * @return Optional containing the instrument if found
     * @throws SQLException if database error occurs
     */
    public Optional<Instrument> getInstrumentById(String instrumentId) throws SQLException {
        // Check cache first
        Instrument cached = idToInstrumentMap.get(instrumentId);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Query database
        var result = database.queryOne("""
            SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at
            FROM instruments
            WHERE id = ?
            """, instrumentId);
        
        if (result == null) {
            return Optional.empty();
        }
        
        Instrument instrument = mapToInstrument(result);
        idToInstrumentMap.put(instrumentId, instrument);
        symbolToInstrumentMap.put(instrument.symbol(), instrument);
        return Optional.of(instrument);
    }
    
    /**
     * Gets an instrument by its symbol.
     * 
     * @param symbol The instrument symbol
     * @return Optional containing the instrument if found
     * @throws SQLException if database error occurs
     */
    public Optional<Instrument> getInstrumentBySymbol(String symbol) throws SQLException {
        // Check cache first
        Instrument cached = symbolToInstrumentMap.get(symbol);
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Query database
        var result = database.queryOne("""
            SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at
            FROM instruments
            WHERE UPPER(symbol) = UPPER(?)
            """, symbol);
        
        if (result == null) {
            return Optional.empty();
        }
        
        Instrument instrument = mapToInstrument(result);
        idToInstrumentMap.put(instrument.id(), instrument);
        symbolToInstrumentMap.put(instrument.symbol(), instrument);
        return Optional.of(instrument);
    }
    
    /**
     * Gets all instruments of a specific type.
     * 
     * @param type The instrument type (ITEM, CRYPTO, EQUITY, etc.)
     * @return List of instruments
     * @throws SQLException if database error occurs
     */
    public List<Instrument> getInstrumentsByType(String type) throws SQLException {
        var results = database.query("""
            SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at
            FROM instruments
            WHERE type = ?
            """, type);
        
        return results.stream()
                .map(this::mapToInstrument)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the current state of an instrument (price, volume, changes, etc.).
     * 
     * @param instrumentId The instrument ID
     * @return Optional containing the instrument state if found
     * @throws SQLException if database error occurs
     */
    public Optional<InstrumentState> getInstrumentState(String instrumentId) throws SQLException {
        var result = database.queryOne("""
            SELECT instrument_id, last_price, last_volume, change_1h, change_24h,\s
                   volatility_24h, market_cap, updated_at
            FROM instrument_state
            WHERE instrument_id = ?
           \s""", instrumentId);
        
        if (result == null) {
            return Optional.empty();
        }
        
        return Optional.of(mapToInstrumentState(result));
    }
    
    /**
     * Gets price history for an instrument.
     * 
     * @param instrumentId The instrument ID
     * @param limit Maximum number of history entries to return
     * @return List of price history entries
     * @throws SQLException if database error occurs
     */
    public List<PriceHistory> getPriceHistory(String instrumentId, int limit) throws SQLException {
        var results = database.query("""
            SELECT id, instrument_id, ts, price, volume, reason
            FROM instrument_price_history
            WHERE instrument_id = ?
            ORDER BY ts DESC
            LIMIT ?
            """, instrumentId, limit);
        
        return results.stream()
                .map(this::mapToPriceHistory)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps a database row to an Instrument object.
     */
    private Instrument mapToInstrument(Map<String, Object> row) {
        String id = (String) row.get("id");
        String type = (String) row.get("type");
        String symbol = (String) row.get("symbol");
        String displayName = (String) row.get("display_name");
        String material = (String) row.get("mc_material");
        int decimals = ((Number) row.get("decimals")).intValue();
        String createdBy = (String) row.get("created_by");
        long createdAt = ((Number) row.get("created_at")).longValue();
        return new Instrument(id, type, symbol, displayName, material, decimals, createdBy, createdAt);
    }
    
    /**
     * Maps a database row to an InstrumentState object.
     */
    private InstrumentState mapToInstrumentState(Map<String, Object> row) {
        String instrumentId = (String) row.get("instrument_id");
        double lastPrice = ((Number) row.get("last_price")).doubleValue();
        double lastVolume = ((Number) row.get("last_volume")).doubleValue();
        double change1h = ((Number) row.get("change_1h")).doubleValue();
        double change24h = ((Number) row.get("change_24h")).doubleValue();
        double volatility24h = ((Number) row.get("volatility_24h")).doubleValue();
        double marketCap = ((Number) row.get("market_cap")).doubleValue();
        long updatedAt = ((Number) row.get("updated_at")).longValue();
        return new InstrumentState(instrumentId, lastPrice, lastVolume, change1h, change24h, 
                                   volatility24h, marketCap, updatedAt);
    }
    
    /**
     * Maps a database row to a PriceHistory object.
     */
    private PriceHistory mapToPriceHistory(Map<String, Object> row) {
        String id = (String) row.get("id");
        String instrumentId = (String) row.get("instrument_id");
        long timestamp = ((Number) row.get("ts")).longValue();
        double price = ((Number) row.get("price")).doubleValue();
        double volume = ((Number) row.get("volume")).doubleValue();
        String reason = (String) row.get("reason");
        return new PriceHistory(id, instrumentId, timestamp, price, volume, reason);
    }
}