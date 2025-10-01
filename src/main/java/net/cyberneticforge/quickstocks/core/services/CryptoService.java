package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Service for managing custom cryptocurrency instruments created by players.
 */
public class CryptoService {
    
    private static final Logger logger = Logger.getLogger(CryptoService.class.getName());
    
    private final Db database;
    
    public CryptoService(Db database) {
        this.database = database;
    }
    
    /**
     * Creates a new custom cryptocurrency instrument.
     * 
     * @param symbol The cryptocurrency symbol (e.g., "MYCOIN")
     * @param displayName The human-readable name (e.g., "My Custom Coin")
     * @param createdBy The UUID of the player creating the crypto
     * @return The instrument ID of the created crypto
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if symbol already exists or parameters are invalid
     */
    public String createCustomCrypto(String symbol, String displayName, String createdBy) throws SQLException {
        // Validate inputs
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be empty");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by UUID cannot be empty");
        }
        
        // Normalize symbol - uppercase and alphanumeric only
        symbol = symbol.toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (symbol.length() < 2 || symbol.length() > 10) {
            throw new IllegalArgumentException("Symbol must be 2-10 alphanumeric characters");
        }
        
        // Check if symbol already exists
        if (symbolExists(symbol)) {
            throw new IllegalArgumentException("Symbol '" + symbol + "' already exists");
        }
        
        try {
            String instrumentId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();
            
            // Create the instrument
            database.execute("""
                INSERT INTO instruments 
                (id, type, symbol, display_name, mc_material, decimals, created_by, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instrumentId,
                "CUSTOM_CRYPTO",
                symbol,
                displayName.trim(),
                null, // No Minecraft material for custom crypto
                8, // 8 decimal places for crypto prices
                createdBy,
                now
            );
            
            // Initialize the instrument state with a starting price of $1.00
            database.execute("""
                INSERT INTO instrument_state 
                (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                instrumentId,
                1.0, // Starting price of $1.00
                0.0, // No initial volume
                0.0, // No initial change
                0.0, // No initial change
                0.0, // No initial volatility
                0.0, // No initial market cap
                now
            );
            
            // Add initial price history entry
            database.execute("""
                INSERT INTO instrument_price_history 
                (id, instrument_id, ts, price, volume, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                instrumentId,
                now,
                1.0, // Starting price
                0.0, // No initial volume
                "Initial crypto creation"
            );
            
            logger.info("Created custom crypto: " + symbol + " (" + displayName + ") -> " + instrumentId + " by player " + createdBy);
            return instrumentId;
            
        } catch (SQLException e) {
            logger.severe("Failed to create custom crypto " + symbol + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Checks if a symbol already exists in the database.
     */
    private boolean symbolExists(String symbol) throws SQLException {
        var result = database.queryValue("SELECT COUNT(*) FROM instruments WHERE UPPER(symbol) = UPPER(?)", symbol);
        return ((Number) result).intValue() > 0;
    }
}