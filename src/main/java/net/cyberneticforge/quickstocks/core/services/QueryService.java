package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Query service for stock-related database operations.
 * Handles data access for commands and other query operations.
 * Now supports both traditional instruments and company shares (market stocks).
 */
public class QueryService {
    
    private final Db database;
    
    public QueryService(Db database) {
        this.database = database;
    }
    
    /**
     * Gets top N market performers (companies on market) based on balance growth.
     * This is the new method for market stocks which are now company shares.
     */
    public List<Map<String, Object>> getTopCompaniesOnMarket(int limit) throws SQLException {
        return database.query("""
            SELECT 
                c.id,
                c.name,
                c.symbol,
                c.balance,
                c.market_percentage,
                c.type,
                c.on_market
            FROM companies c
            WHERE c.on_market = 1
            ORDER BY c.balance DESC
            LIMIT ?
            """, limit);
    }
    
    /**
     * Finds a company (market stock) by symbol.
     * This is the new method for market operations.
     */
    public Optional<Map<String, Object>> findCompanyBySymbol(String symbol) throws SQLException {
        Map<String, Object> result = database.queryOne("""
            SELECT 
                c.id,
                c.name,
                c.symbol,
                c.balance,
                c.market_percentage,
                c.allow_buyout,
                c.type,
                c.on_market,
                c.created_at
            FROM companies c
            WHERE UPPER(c.symbol) = UPPER(?) AND c.on_market = 1
            """, symbol);
        
        return Optional.ofNullable(result);
    }
    
    /**
     * Gets all company symbols that are on the market (for tab completion).
     */
    public List<String> getMarketCompanySymbols() throws SQLException {
        return database.query("SELECT symbol FROM companies WHERE on_market = 1 AND symbol IS NOT NULL ORDER BY symbol")
                .stream()
                .map(row -> (String) row.get("symbol"))
                .toList();
    }
    
    /**
     * Gets company symbols matching a prefix (for tab completion).
     */
    public List<String> getMatchingCompanySymbols(String prefix) throws SQLException {
        String upperPrefix = prefix.toUpperCase() + "%";
        
        return database.query("""
            SELECT symbol FROM companies 
            WHERE on_market = 1 AND symbol IS NOT NULL AND UPPER(symbol) LIKE ?
            ORDER BY symbol
            """, upperPrefix)
                .stream()
                .map(row -> (String) row.get("symbol"))
                .toList();
    }
    
    /**
     * Gets company ID by symbol lookup (market stocks).
     */
    public String getCompanyIdBySymbol(String symbol) throws SQLException {
        Map<String, Object> result = database.queryOne(
            "SELECT id FROM companies WHERE UPPER(symbol) = UPPER(?) AND on_market = 1", 
            symbol
        );
        return result != null ? (String) result.get("id") : null;
    }
    
    /**
     * Gets current share price for a company (market stock).
     */
    public Double getCompanySharePrice(String companyId) throws SQLException {
        Map<String, Object> result = database.queryOne(
            "SELECT balance FROM companies WHERE id = ?", 
            companyId
        );
        if (result == null) return null;
        
        double balance = ((Number) result.get("balance")).doubleValue();
        // Share price = balance / 10000 shares (as per CompanyMarketService)
        return balance / 10000.0;
    }
    
    /**
     * Gets recent share transaction history for a company (from instruments infrastructure).
     */
    public List<Map<String, Object>> getRecentShareTransactions(String companyId, int limit) throws SQLException {
        String instrumentId = "COMPANY_" + companyId;
        return database.query("""
            SELECT 
                o.side as type,
                o.qty as shares,
                o.price,
                o.ts
            FROM orders o
            WHERE o.instrument_id = ?
            ORDER BY o.ts DESC
            LIMIT ?
            """, instrumentId, limit);
    }
    
    // ========================================================================
    // Legacy Instrument Methods (for items, crypto, etc. - NOT market stocks)
    // Market stocks are now company shares - use getTopCompaniesOnMarket() instead
    // ========================================================================
    
    /**
     * Gets top N gainers based on 24h change.
     * NOTE: This is for traditional instruments (items, crypto), NOT market stocks.
     * For market stocks, use getTopCompaniesOnMarket().
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
     * Gets display name for an instrument by ID.
     */
    public String getInstrumentDisplayName(String instrumentId) throws SQLException {
        return database.queryValue(
            "SELECT display_name FROM instruments WHERE id = ?", 
            instrumentId
        );
    }
    
    /**
     * Gets all instrument symbols for tab completion.
     */
    public List<String> getInstrumentSymbols() throws SQLException {
        return database.query("SELECT symbol FROM instruments ORDER BY symbol")
                .stream()
                .map(row -> (String) row.get("symbol"))
                .toList();
    }
}