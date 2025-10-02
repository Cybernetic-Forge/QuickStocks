package net.cyberneticforge.quickstocks.api.managers;

import net.cyberneticforge.quickstocks.core.services.CryptoService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API Manager for cryptocurrency operations.
 * Provides a high-level interface for external plugins to interact with the crypto system.
 */
public class CryptoManager {
    
    private final CryptoService cryptoService;
    
    public CryptoManager(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }
    
    /**
     * Creates a custom cryptocurrency.
     * 
     * @param playerUuid UUID of the player creating the crypto
     * @param symbol Symbol for the cryptocurrency
     * @param displayName Display name for the cryptocurrency
     * @return The ID of the created cryptocurrency
     * @throws SQLException if database error occurs
     */
    public String createCrypto(String playerUuid, String symbol, String displayName) throws SQLException {
        return cryptoService.createCrypto(playerUuid, symbol, displayName);
    }
    
    /**
     * Gets a cryptocurrency by ID.
     * 
     * @param cryptoId The cryptocurrency ID
     * @return Optional containing the crypto data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Map<String, Object>> getCrypto(String cryptoId) throws SQLException {
        return cryptoService.getCryptoById(cryptoId);
    }
    
    /**
     * Gets a cryptocurrency by symbol.
     * 
     * @param symbol The cryptocurrency symbol
     * @return Optional containing the crypto data if found
     * @throws SQLException if database error occurs
     */
    public Optional<Map<String, Object>> getCryptoBySymbol(String symbol) throws SQLException {
        return cryptoService.getCryptoBySymbol(symbol);
    }
    
    /**
     * Gets all cryptocurrencies.
     * 
     * @return List of all cryptocurrencies
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getAllCryptos() throws SQLException {
        return cryptoService.getAllCryptos();
    }
    
    /**
     * Gets all cryptocurrencies created by a specific player.
     * 
     * @param playerUuid UUID of the player
     * @return List of cryptocurrencies created by the player
     * @throws SQLException if database error occurs
     */
    public List<Map<String, Object>> getCryptosByCreator(String playerUuid) throws SQLException {
        return cryptoService.getCryptosByCreator(playerUuid);
    }
}
