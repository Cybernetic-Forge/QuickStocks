package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CryptoService functionality.
 */
public class CryptoServiceTest {
    
    private DatabaseManager databaseManager;
    private Db database;
    private CryptoService cryptoService;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Create temp database for testing
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/crypto_service_test_" + System.currentTimeMillis() + ".db");
        
        databaseManager = new DatabaseManager(config, false); // No seeding for clean tests
        databaseManager.initialize();
        database = databaseManager.getDb();
        
        cryptoService = new CryptoService(database);
    }
    
    @AfterEach
    void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testCreateCustomCrypto_Success() throws SQLException {
        String playerUuid = UUID.randomUUID().toString();
        String symbol = "TESTCOIN";
        String displayName = "Test Coin";
        
        String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, playerUuid);
        
        assertNotNull(instrumentId);
        assertFalse(instrumentId.trim().isEmpty());
        
        // Verify instrument was created
        var instruments = database.query("SELECT * FROM instruments WHERE id = ?", instrumentId);
        assertEquals(1, instruments.size());
        
        Map<String, Object> instrument = instruments.get(0);
        assertEquals("CUSTOM_CRYPTO", instrument.get("type"));
        assertEquals(symbol, instrument.get("symbol"));
        assertEquals(displayName, instrument.get("display_name"));
        assertEquals(playerUuid, instrument.get("created_by"));
        assertEquals(8, instrument.get("decimals")); // Crypto should have 8 decimals
        
        // Verify instrument state was created
        var states = database.query("SELECT * FROM instrument_state WHERE instrument_id = ?", instrumentId);
        assertEquals(1, states.size());
        
        Map<String, Object> state = states.get(0);
        assertEquals(1.0, ((Number) state.get("last_price")).doubleValue(), 0.001);
        assertEquals(0.0, ((Number) state.get("last_volume")).doubleValue(), 0.001);
        
        // Verify price history was created
        var history = database.query("SELECT * FROM instrument_price_history WHERE instrument_id = ?", instrumentId);
        assertEquals(1, history.size());
        
        Map<String, Object> historyEntry = history.get(0);
        assertEquals(1.0, ((Number) historyEntry.get("price")).doubleValue(), 0.001);
        assertEquals("Initial crypto creation", historyEntry.get("reason"));
    }
    
    @Test
    void testCreateCustomCrypto_NormalizeSymbol() throws SQLException {
        String playerUuid = UUID.randomUUID().toString();
        String symbol = "test-coin!@#"; // Should be normalized to TESTCOIN
        String displayName = "Test Coin";
        
        String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, playerUuid);
        
        var instruments = database.query("SELECT symbol FROM instruments WHERE id = ?", instrumentId);
        assertEquals("TESTCOIN", instruments.get(0).get("symbol"));
    }
    
    @Test
    void testCreateCustomCrypto_DuplicateSymbol() throws SQLException {
        String playerUuid = UUID.randomUUID().toString();
        String symbol = "DUPLICATE";
        String displayName = "First Coin";
        
        // Create first crypto
        cryptoService.createCustomCrypto(symbol, displayName, playerUuid);
        
        // Try to create duplicate
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, "Second Coin", playerUuid)
        );
        
        assertTrue(exception.getMessage().contains("already exists"));
    }
    
    @Test
    void testCreateCustomCrypto_InvalidSymbol() {
        String playerUuid = UUID.randomUUID().toString();
        String displayName = "Test Coin";
        
        // Test empty symbol
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto("", displayName, playerUuid)
        );
        
        // Test null symbol
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(null, displayName, playerUuid)
        );
        
        // Test too short symbol
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto("A", displayName, playerUuid)
        );
        
        // Test too long symbol
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto("VERYLONGSYMBOL", displayName, playerUuid)
        );
    }
    
    @Test
    void testCreateCustomCrypto_InvalidDisplayName() {
        String playerUuid = UUID.randomUUID().toString();
        String symbol = "TESTCOIN";
        
        // Test empty display name
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, "", playerUuid)
        );
        
        // Test null display name
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, null, playerUuid)
        );
        
        // Test whitespace-only display name
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, "   ", playerUuid)
        );
    }
    
    @Test
    void testCreateCustomCrypto_InvalidCreatedBy() {
        String symbol = "TESTCOIN";
        String displayName = "Test Coin";
        
        // Test empty created by
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, displayName, "")
        );
        
        // Test null created by
        assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto(symbol, displayName, null)
        );
    }
    
    @Test
    void testCreateCustomCrypto_CaseInsensitiveDuplicateCheck() throws SQLException {
        String playerUuid = UUID.randomUUID().toString();
        String displayName = "Test Coin";
        
        // Create first crypto with uppercase
        cryptoService.createCustomCrypto("TESTCOIN", displayName, playerUuid);
        
        // Try to create with lowercase - should fail
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            cryptoService.createCustomCrypto("testcoin", "Another Coin", playerUuid)
        );
        
        assertTrue(exception.getMessage().contains("already exists"));
    }
    
    @Test
    void testCreateCustomCrypto_TrimDisplayName() throws SQLException {
        String playerUuid = UUID.randomUUID().toString();
        String symbol = "TESTCOIN";
        String displayName = "  Test Coin  "; // With leading/trailing spaces
        
        String instrumentId = cryptoService.createCustomCrypto(symbol, displayName, playerUuid);
        
        var instruments = database.query("SELECT display_name FROM instruments WHERE id = ?", instrumentId);
        assertEquals("Test Coin", instruments.get(0).get("display_name"));
    }
}