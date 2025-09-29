package com.example.quickstocks.application.queries;

import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Query service for stock-related database operations.
 * Handles data access for commands and other query operations.
 */
public class QueryService {
    
    private final Db database;
    
    public QueryService(Db database) {
        this.database = database;
    }
    
    /**
     * Gets top N gainers based on 24h change.
     */
    public List<Map<String, Object>> getTopGainers(int limit) throws SQLException {
        return getTopGainersByChange24h(limit);
    }
    
    /**
     * Gets top N gainers based on 24h change.
     */
    public List<Map<String, Object>> getTopGainersByChange24h(int limit) throws SQLException {
        return database.query("""
            SELECT 
                i.symbol,
                i.display_name,
                i.type,
                s.last_price,
                s.change_1h,
                s.change_24h,
                s.volatility_24h,
                s.market_cap
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            ORDER BY s.change_24h DESC
            LIMIT ?
            """, limit);
    }
    
    /**
     * Finds an instrument by exact symbol match (case-insensitive).
     */
    public Optional<Map<String, Object>> findBySymbol(String symbol) throws SQLException {
        Map<String, Object> result = database.queryOne("""
            SELECT 
                i.id,
                i.symbol,
                i.display_name,
                i.type,
                i.mc_material,
                s.last_price,
                s.change_1h,
                s.change_24h,
                s.volatility_24h,
                s.market_cap
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE UPPER(i.symbol) = UPPER(?)
            """, symbol);
        
        return Optional.ofNullable(result);
    }
    
    /**
     * Finds an instrument by Minecraft material name (case-insensitive).
     */
    public Optional<Map<String, Object>> findByMcMaterial(String mcMaterial) throws SQLException {
        Map<String, Object> result = database.queryOne("""
            SELECT 
                i.id,
                i.symbol,
                i.display_name,
                i.type,
                i.mc_material,
                s.last_price,
                s.change_1h,
                s.change_24h,
                s.volatility_24h,
                s.market_cap
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE UPPER(i.mc_material) = UPPER(?)
            """, mcMaterial);
        
        return Optional.ofNullable(result);
    }
    
    /**
     * Gets recent price history for an instrument (last N entries).
     */
    public List<Map<String, Object>> getRecentPriceHistory(String instrumentId, int limit) throws SQLException {
        return database.query("""
            SELECT 
                ts,
                price,
                volume,
                reason
            FROM instrument_price_history
            WHERE instrument_id = ?
            ORDER BY ts DESC
            LIMIT ?
            """, instrumentId, limit);
    }
    
    /**
     * Gets all symbols for tab completion.
     */
    public List<String> getAllSymbols() throws SQLException {
        return database.query("SELECT symbol FROM instruments ORDER BY symbol")
                .stream()
                .map(row -> (String) row.get("symbol"))
                .toList();
    }
    
    /**
     * Gets all materials for tab completion.
     */
    public List<String> getAllMaterials() throws SQLException {
        return database.query("SELECT DISTINCT mc_material FROM instruments WHERE mc_material IS NOT NULL ORDER BY mc_material")
                .stream()
                .map(row -> (String) row.get("mc_material"))
                .toList();
    }
    
    /**
     * Gets symbols and materials matching a prefix (for tab completion).
     */
    public List<String> getMatchingSymbolsAndMaterials(String prefix) throws SQLException {
        String upperPrefix = prefix.toUpperCase() + "%";
        
        List<String> symbols = database.query("""
            SELECT symbol FROM instruments 
            WHERE UPPER(symbol) LIKE ?
            ORDER BY symbol
            """, upperPrefix)
                .stream()
                .map(row -> (String) row.get("symbol"))
                .toList();
        
        List<String> materials = database.query("""
            SELECT DISTINCT mc_material FROM instruments 
            WHERE mc_material IS NOT NULL AND UPPER(mc_material) LIKE ?
            ORDER BY mc_material
            """, upperPrefix)
                .stream()
                .map(row -> (String) row.get("mc_material"))
                .toList();
        
        // Combine and remove duplicates while preserving order
        return java.util.stream.Stream.concat(symbols.stream(), materials.stream())
                .distinct()
                .toList();
    }
    
    /**
     * Gets instrument ID by symbol lookup.
     */
    public String getInstrumentIdBySymbol(String symbol) throws SQLException {
        Map<String, Object> result = database.queryOne(
            "SELECT id FROM instruments WHERE UPPER(symbol) = UPPER(?)", 
            symbol
        );
        return result != null ? (String) result.get("id") : null;
    }
    
    /**
     * Gets current price for an instrument.
     */
    public Double getCurrentPrice(String instrumentId) throws SQLException {
        return database.queryValue(
            "SELECT last_price FROM instrument_state WHERE instrument_id = ?", 
            instrumentId
        );
    }
    
    /**
     * Gets total number of orders in the system.
     */
    public int getTotalOrderCount() throws SQLException {
        Integer count = database.queryValue("SELECT COUNT(*) FROM orders");
        return count != null ? count : 0;
    }
    
    /**
     * Gets total number of unique players who have traded.
     */
    public int getTotalPlayerCount() throws SQLException {
        Integer count = database.queryValue("SELECT COUNT(DISTINCT player_uuid) FROM orders");
        return count != null ? count : 0;
    }
}