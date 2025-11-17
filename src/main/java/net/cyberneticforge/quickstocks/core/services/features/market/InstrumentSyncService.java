package net.cyberneticforge.quickstocks.core.services.features.market;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service that synchronizes Stock objects (in-memory) with Instrument database records.
 * This bridges the gap between the legacy Stock model and the new Instrument-based system.
 * 
 * Purpose:
 * - When StockMarketService creates/updates Stock objects, sync them to database
 * - When prices change, persist to instrument_state table
 * - Ensure all trading operations work with persisted data
 */
public class InstrumentSyncService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    private final Map<String, String> symbolToInstrumentId = new HashMap<>(); // symbol -> instrument_id
    
    /**
     * Ensures a Stock object has a corresponding Instrument in the database.
     * Creates or updates the instrument as needed.
     * 
     * @param stock The Stock object to sync
     * @return The instrument ID
     */
    public String ensureInstrumentExists(Stock stock) throws SQLException {
        String symbol = stock.getSymbol();
        String instrumentId = symbolToInstrumentId.get(symbol);
        
        if (instrumentId == null) {
            // Check if instrument exists in database
            Map<String, Object> existing = database.queryOne(
                "SELECT id FROM instruments WHERE UPPER(symbol) = UPPER(?)", 
                symbol
            );
            
            if (existing != null) {
                instrumentId = (String) existing.get("id");
                symbolToInstrumentId.put(symbol, instrumentId);
                logger.debug("Found existing instrument for symbol " + symbol + ": " + instrumentId);
            } else {
                // Create new instrument
                instrumentId = createInstrument(stock);
                symbolToInstrumentId.put(symbol, instrumentId);
                logger.info("Created new instrument for symbol " + symbol + ": " + instrumentId);
            }
        }
        
        return instrumentId;
    }
    
    /**
     * Creates a new instrument record from a Stock object.
     */
    private String createInstrument(Stock stock) throws SQLException {
        String instrumentId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        
        database.execute(
            """
            INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            instrumentId,
            "EQUITY",  // Stocks are equities
            stock.getSymbol(),
            stock.getName(),
            null,  // No Minecraft material for stocks
            2,     // 2 decimal places for stock prices
            null,  // Not created by a player
            now
        );
        
        return instrumentId;
    }
    
    /**
     * Updates the instrument_state table with current Stock prices.
     * This ensures price changes are persisted to the database.
     * 
     * @param stock The Stock object with updated prices
     */
    public void syncPriceToDatabase(Stock stock) throws SQLException {
        String instrumentId = ensureInstrumentExists(stock);
        long now = System.currentTimeMillis();
        
        // Calculate 1h and 24h changes (simplified - use previous price for now)
        double change = stock.getPriceChangePercent();
        
        // Check if instrument_state exists
        Map<String, Object> existing = database.queryOne(
            "SELECT instrument_id FROM instrument_state WHERE instrument_id = ?", 
            instrumentId
        );
        
        if (existing == null) {
            // Create new instrument_state
            database.execute(
                """
                INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instrumentId,
                stock.getCurrentPrice(),
                stock.getDailyVolume(),
                change,  // Use current change as approximation
                change,  // Use current change as approximation
                stock.getVolatilityRating(),
                stock.getMarketCap(),
                now
            );
            logger.debug("Created instrument_state for " + stock.getSymbol());
        } else {
            // Update existing instrument_state
            database.execute(
                """
                UPDATE instrument_state 
                SET last_price = ?, last_volume = ?, change_1h = ?, change_24h = ?, volatility_24h = ?, market_cap = ?, updated_at = ?
                WHERE instrument_id = ?
                """,
                stock.getCurrentPrice(),
                stock.getDailyVolume(),
                change,  // Simplified
                change,  // Simplified
                stock.getVolatilityRating(),
                stock.getMarketCap(),
                now,
                instrumentId
            );
            logger.debug("Updated instrument_state for " + stock.getSymbol());
        }
        
        // Add to price history
        addPriceHistory(instrumentId, stock.getCurrentPrice(), stock.getDailyVolume());
    }
    
    /**
     * Adds an entry to the instrument_price_history table.
     */
    private void addPriceHistory(String instrumentId, double price, double volume) throws SQLException {
        String historyId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        
        database.execute(
            """
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            historyId,
            instrumentId,
            now,
            price,
            volume,
            "MARKET_UPDATE"
        );
    }
    
    /**
     * Loads all instruments from the database and returns them as Stock objects.
     * This allows StockMarketService to initialize from persisted data.
     * 
     * @return Map of symbol -> Stock
     */
    public Map<String, Stock> loadStocksFromDatabase() throws SQLException {
        Map<String, Stock> stocks = new HashMap<>();
        
        var results = database.query(
            """
            SELECT i.id, i.symbol, i.display_name, i.type, s.last_price, s.market_cap, s.volatility_24h
            FROM instruments i
            LEFT JOIN instrument_state s ON i.id = s.instrument_id
            WHERE i.type IN ('EQUITY', 'ITEM', 'CRYPTO', 'CUSTOM_CRYPTO')
            """
        );
        
        for (var row : results) {
            String symbol = (String) row.get("symbol");
            String displayName = (String) row.get("display_name");
            String type = (String) row.get("type");
            
            double price = row.get("last_price") != null ? 
                ((Number) row.get("last_price")).doubleValue() : 10.0;
            
            // Create Stock object from database data
            String sector = mapTypeToSector(type);
            Stock stock = new Stock(symbol, displayName, sector, price);
            
            // Set additional properties
            if (row.get("volatility_24h") != null) {
                stock.setVolatilityRating(((Number) row.get("volatility_24h")).doubleValue());
            }
            
            stocks.put(symbol, stock);
            symbolToInstrumentId.put(symbol, (String) row.get("id"));
        }
        
        logger.info("Loaded " + stocks.size() + " stocks from database");
        return stocks;
    }
    
    /**
     * Maps instrument type to a stock sector for compatibility.
     */
    private String mapTypeToSector(String type) {
        return switch (type) {
            case "CRYPTO", "CUSTOM_CRYPTO" -> "crypto";
            case "ITEM" -> "materials";
            case "EQUITY" -> "equity";
            default -> "general";
        };
    }
    
    /**
     * Gets the instrument ID for a given symbol.
     * Returns null if not found.
     */
    public String getInstrumentId(String symbol) {
        return symbolToInstrumentId.get(symbol);
    }
}
