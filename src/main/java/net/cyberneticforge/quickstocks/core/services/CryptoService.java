package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.core.model.Crypto;
import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    
    /**
     * Gets a cryptocurrency by ID.
     * 
     * @param cryptoId The cryptocurrency ID
     * @return Optional containing the crypto data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Crypto> getCryptoById(String cryptoId) throws SQLException {
        var instrumentRow = database.queryOne("""
            SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at
            FROM instruments
            WHERE id = ? AND (type = 'CRYPTO' OR type = 'CUSTOM_CRYPTO')
            """, cryptoId);
        
        if (instrumentRow == null) {
            return Optional.empty();
        }
        
        var stateRow = database.queryOne("""
            SELECT instrument_id, last_price, last_volume, change_1h, change_24h, 
                   volatility_24h, market_cap, updated_at
            FROM instrument_state
            WHERE instrument_id = ?
            """, cryptoId);
        
        if (stateRow == null) {
            return Optional.empty();
        }
        
        Instrument instrument = mapToInstrument(instrumentRow);
        InstrumentState state = mapToInstrumentState(stateRow);
        return Optional.of(new Crypto(instrument, state));
    }
    
    /**
     * Gets a cryptocurrency by symbol.
     * 
     * @param symbol The cryptocurrency symbol
     * @return Optional containing the crypto data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Crypto> getCryptoBySymbol(String symbol) throws SQLException {
        var instrumentRow = database.queryOne("""
            SELECT id, type, symbol, display_name, mc_material, decimals, created_by, created_at
            FROM instruments
            WHERE UPPER(symbol) = UPPER(?) AND (type = 'CRYPTO' OR type = 'CUSTOM_CRYPTO')
            """, symbol);
        
        if (instrumentRow == null) {
            return Optional.empty();
        }
        
        String instrumentId = (String) instrumentRow.get("id");
        var stateRow = database.queryOne("""
            SELECT instrument_id, last_price, last_volume, change_1h, change_24h, 
                   volatility_24h, market_cap, updated_at
            FROM instrument_state
            WHERE instrument_id = ?
            """, instrumentId);
        
        if (stateRow == null) {
            return Optional.empty();
        }
        
        Instrument instrument = mapToInstrument(instrumentRow);
        InstrumentState state = mapToInstrumentState(stateRow);
        return Optional.of(new Crypto(instrument, state));
    }
    
    /**
     * Gets all cryptocurrencies.
     * 
     * @return List of all cryptocurrencies
     * @throws SQLException if database error occurs
     */
    public List<Crypto> getAllCryptos() throws SQLException {
        var results = database.query("""
            SELECT i.id, i.type, i.symbol, i.display_name, i.mc_material, i.decimals, i.created_by, i.created_at,
                   s.instrument_id, s.last_price, s.last_volume, s.change_1h, s.change_24h, 
                   s.volatility_24h, s.market_cap, s.updated_at
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE i.type = 'CRYPTO' OR i.type = 'CUSTOM_CRYPTO'
            """);
        
        return results.stream()
                .map(row -> {
                    Instrument instrument = mapToInstrument(row);
                    InstrumentState state = mapToInstrumentState(row);
                    return new Crypto(instrument, state);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all cryptocurrencies created by a specific player.
     * 
     * @param playerUuid UUID of the player
     * @return List of cryptocurrencies created by the player
     * @throws SQLException if database error occurs
     */
    public List<Crypto> getCryptosByCreator(String playerUuid) throws SQLException {
        var results = database.query("""
            SELECT i.id, i.type, i.symbol, i.display_name, i.mc_material, i.decimals, i.created_by, i.created_at,
                   s.instrument_id, s.last_price, s.last_volume, s.change_1h, s.change_24h, 
                   s.volatility_24h, s.market_cap, s.updated_at
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE (i.type = 'CRYPTO' OR i.type = 'CUSTOM_CRYPTO') AND i.created_by = ?
            """, playerUuid);
        
        return results.stream()
                .map(row -> {
                    Instrument instrument = mapToInstrument(row);
                    InstrumentState state = mapToInstrumentState(row);
                    return new Crypto(instrument, state);
                })
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
}