package net.cyberneticforge.quickstocks.core.services.features.market;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Crypto;
import net.cyberneticforge.quickstocks.core.model.Instrument;
import net.cyberneticforge.quickstocks.core.model.InstrumentState;
import net.cyberneticforge.quickstocks.infrastructure.db.Db;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing custom cryptocurrency instruments created by players.
 */
public class CryptoService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final Db database = QuickStocksPlugin.getDatabaseManager().getDb();
    
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
        return createCustomCrypto(symbol, displayName, createdBy, null, true);
    }
    
    /**
     * Creates a new custom cryptocurrency instrument with balance validation.
     * 
     * @param symbol The cryptocurrency symbol (e.g., "MYCOIN")
     * @param displayName The human-readable name (e.g., "My Custom Coin")
     * @param createdBy The UUID of the player creating the crypto
     * @param companyId The company ID if created by a company (null for personal)
     * @param checkBalance Whether to check and deduct balance
     * @return The instrument ID of the created crypto
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if symbol already exists or parameters are invalid
     */
    public String createCustomCrypto(String symbol, String displayName, String createdBy, String companyId, boolean checkBalance) throws SQLException {
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
        
        // Get crypto configuration
        var cryptoCfg = QuickStocksPlugin.getCryptoCfg();
        
        // Check if crypto creation is enabled
        if (!cryptoCfg.isEnabled()) {
            throw new IllegalArgumentException("Cryptocurrency creation is disabled");
        }
        
        // Fire cancellable event before creating crypto
        try {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(createdBy));
            if (player != null) {
                net.cyberneticforge.quickstocks.api.events.CryptoCreateEvent event = 
                    new net.cyberneticforge.quickstocks.api.events.CryptoCreateEvent(
                        player, symbol, displayName
                    );
                org.bukkit.Bukkit.getPluginManager().callEvent(event);
                
                if (event.isCancelled()) {
                    throw new IllegalArgumentException("Cryptocurrency creation cancelled by event handler");
                }
            }
        } catch (IllegalArgumentException e) {
            throw e; // Rethrow cancellation
        } catch (Exception e) {
            logger.debug("Could not fire CryptoCreateEvent: " + e.getMessage());
        }
        
        // Validate and check limits
        if (companyId == null) {
            // Personal crypto creation
            if (!cryptoCfg.getPersonalConfig().isEnabled()) {
                throw new IllegalArgumentException("Personal cryptocurrency creation is disabled");
            }
            
            // Check max per player limit
            int maxPerPlayer = cryptoCfg.getPersonalConfig().getMaxPerPlayer();
            if (maxPerPlayer > 0) {
                int count = countCryptosByCreator(createdBy);
                if (count >= maxPerPlayer) {
                    throw new IllegalArgumentException("You have reached the maximum limit of " + maxPerPlayer + " cryptocurrencies");
                }
            }
            
            // Check and deduct balance
            if (checkBalance) {
                double cost = cryptoCfg.getPersonalConfig().getCreationCost();
                double balance = QuickStocksPlugin.getWalletService().getBalance(createdBy);
                if (balance < cost) {
                    throw new IllegalArgumentException("Insufficient funds. Required: $" + String.format("%.2f", cost) + 
                        ", Available: $" + String.format("%.2f", balance));
                }
                QuickStocksPlugin.getWalletService().removeBalance(createdBy, cost);
                logger.info("Deducted $" + String.format("%.2f", cost) + " from player " + createdBy + " for crypto creation");
            }
        } else {
            // Company crypto creation
            if (!cryptoCfg.getCompanyConfig().isEnabled()) {
                throw new IllegalArgumentException("Company cryptocurrency creation is disabled");
            }
            
            // Check max per company limit
            int maxPerCompany = cryptoCfg.getCompanyConfig().getMaxPerCompany();
            if (maxPerCompany > 0) {
                int count = countCryptosByCompany(companyId);
                if (count >= maxPerCompany) {
                    throw new IllegalArgumentException("Company has reached the maximum limit of " + maxPerCompany + " cryptocurrencies");
                }
            }
            
            // Check company balance
            if (checkBalance) {
                var companyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(companyId);
                if (companyOpt.isEmpty()) {
                    throw new IllegalArgumentException("Company not found");
                }
                var company = companyOpt.get();
                
                // Get threshold for company type
                double threshold = cryptoCfg.getCompanyConfig().getBalanceThresholds()
                    .getOrDefault(company.getType(), cryptoCfg.getCompanyConfig().getBalanceThreshold());
                
                if (company.getBalance() < threshold) {
                    throw new IllegalArgumentException("Company needs at least $" + String.format("%.2f", threshold) + 
                        " balance to create cryptocurrency. Current: $" + String.format("%.2f", company.getBalance()));
                }
            }
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
            
            // Get crypto defaults from config
            double startingPrice = cryptoCfg.getDefaultsConfig().getStartingPrice();
            int decimals = cryptoCfg.getDefaultsConfig().getDecimals();
            
            // Create the instrument
            database.execute("""
                INSERT INTO instruments\s
                (id, type, symbol, display_name, mc_material, decimals, created_by, created_at, company_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
               \s""",
                instrumentId,
                "CUSTOM_CRYPTO",
                symbol,
                displayName.trim(),
                null, // No Minecraft material for custom crypto
                decimals,
                createdBy,
                now,
                companyId
            );
            
            // Initialize the instrument state with configured starting price
            database.execute("""
                INSERT INTO instrument_state\s
                (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
               \s""",
                instrumentId,
                startingPrice,
                cryptoCfg.getDefaultsConfig().getInitialVolume(),
                0.0, // No initial change
                0.0, // No initial change
                0.0, // No initial volatility
                0.0, // No initial market cap
                now
            );
            
            // Add initial price history entry
            database.execute("""
                INSERT INTO instrument_price_history\s
                (id, instrument_id, ts, price, volume, reason)
                VALUES (?, ?, ?, ?, ?, ?)
               \s""",
                UUID.randomUUID().toString(),
                instrumentId,
                now,
                startingPrice,
                cryptoCfg.getDefaultsConfig().getInitialVolume(),
                companyId != null ? "Initial company crypto creation" : "Initial crypto creation"
            );
            
            String ownerType = companyId != null ? "company " + companyId : "player " + createdBy;
            logger.info("Created custom crypto: " + symbol + " (" + displayName + ") -> " + instrumentId + " by " + ownerType);
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
     * Counts the number of cryptocurrencies created by a player.
     */
    private int countCryptosByCreator(String playerUuid) throws SQLException {
        var result = database.queryValue(
            "SELECT COUNT(*) FROM instruments WHERE (type = 'CRYPTO' OR type = 'CUSTOM_CRYPTO') AND created_by = ? AND company_id IS NULL",
            playerUuid
        );
        return ((Number) result).intValue();
    }
    
    /**
     * Counts the number of cryptocurrencies created by a company.
     */
    private int countCryptosByCompany(String companyId) throws SQLException {
        var result = database.queryValue(
            "SELECT COUNT(*) FROM instruments WHERE (type = 'CRYPTO' OR type = 'CUSTOM_CRYPTO') AND company_id = ?",
            companyId
        );
        return ((Number) result).intValue();
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
            SELECT instrument_id, last_price, last_volume, change_1h, change_24h,\s
                   volatility_24h, market_cap, updated_at
            FROM instrument_state
            WHERE instrument_id = ?
           \s""", cryptoId);
        
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
            SELECT instrument_id, last_price, last_volume, change_1h, change_24h,\s
                   volatility_24h, market_cap, updated_at
            FROM instrument_state
            WHERE instrument_id = ?
           \s""", instrumentId);
        
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
                   s.instrument_id, s.last_price, s.last_volume, s.change_1h, s.change_24h,\s
                   s.volatility_24h, s.market_cap, s.updated_at
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE i.type = 'CRYPTO' OR i.type = 'CUSTOM_CRYPTO'
           \s""");
        
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
                   s.instrument_id, s.last_price, s.last_volume, s.change_1h, s.change_24h,\s
                   s.volatility_24h, s.market_cap, s.updated_at
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE (i.type = 'CRYPTO' OR i.type = 'CUSTOM_CRYPTO') AND i.created_by = ?
           \s""", playerUuid);
        
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