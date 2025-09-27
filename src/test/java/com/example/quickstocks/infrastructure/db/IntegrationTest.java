package com.example.quickstocks.infrastructure.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that demonstrates the full database workflow.
 */
public class IntegrationTest {
    
    @TempDir
    File tempDir;
    
    private DatabaseManager databaseManager;
    
    @BeforeEach
    void setUp() {
        // Create a plugins directory structure like in a real Minecraft server
        File pluginsDir = new File(tempDir, "plugins/QuickStocks");
        pluginsDir.mkdirs();
        
        // Configure for the expected path
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(new File(pluginsDir, "data.db").getAbsolutePath());
        
        databaseManager = new DatabaseManager(config, false); // Disable auto-seeding for tests
    }
    
    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testFullWorkflow() throws SQLException {
        // Initialize database system
        databaseManager.initialize();
        
        // Verify database file was created in the expected location
        File expectedDbFile = new File(tempDir, "plugins/QuickStocks/data.db");
        assertTrue(expectedDbFile.exists(), "Database file should be created at expected path");
        
        Db db = databaseManager.getDb();
        
        // Test creating instruments (stocks, crypto, items)
        String stockId = UUID.randomUUID().toString();
        String cryptoId = UUID.randomUUID().toString();
        String itemId = UUID.randomUUID().toString();
        
        // Add different types of instruments
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, stockId, "EQUITY", "AAPL", "Apple Inc.", 2, System.currentTimeMillis());
        
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, cryptoId, "CRYPTO", "BTC", "Bitcoin", 8, System.currentTimeMillis());
        
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, itemId, "ITEM", "MC_STONE", "Stone", "STONE", 0, System.currentTimeMillis());
        
        // Add current state for each instrument
        db.execute("""
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """, stockId, 150.25, 1000000.0, 2.5, System.currentTimeMillis());
        
        db.execute("""
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """, cryptoId, 45000.0, 500.0, -1.2, System.currentTimeMillis());
        
        db.execute("""
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """, itemId, 1.0, 50000.0, 0.0, System.currentTimeMillis());
        
        // Add some price history
        long now = System.currentTimeMillis();
        db.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID().toString(), stockId, now - 3600000, 147.50, 800000.0, "MARKET_OPEN");
        
        db.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID().toString(), cryptoId, now - 3600000, 46000.0, 300.0, "WHALE_MOVEMENT");
        
        // Query and verify the data
        
        // Get all instruments
        var instruments = db.query("SELECT * FROM instruments ORDER BY type, symbol");
        assertEquals(3, instruments.size(), "Should have 3 instruments");
        
        // Verify stock
        Map<String, Object> stock = instruments.stream()
            .filter(i -> "EQUITY".equals(i.get("type")))
            .findFirst().orElse(null);
        assertNotNull(stock, "Should find stock");
        assertEquals("AAPL", stock.get("symbol"), "Stock symbol should match");
        assertEquals("Apple Inc.", stock.get("display_name"), "Stock name should match");
        assertEquals(2, stock.get("decimals"), "Stock decimals should match");
        
        // Verify crypto
        Map<String, Object> crypto = instruments.stream()
            .filter(i -> "CRYPTO".equals(i.get("type")))
            .findFirst().orElse(null);
        assertNotNull(crypto, "Should find crypto");
        assertEquals("BTC", crypto.get("symbol"), "Crypto symbol should match");
        assertEquals(8, crypto.get("decimals"), "Crypto decimals should match");
        
        // Verify item
        Map<String, Object> item = instruments.stream()
            .filter(i -> "ITEM".equals(i.get("type")))
            .findFirst().orElse(null);
        assertNotNull(item, "Should find item");
        assertEquals("MC_STONE", item.get("symbol"), "Item symbol should match");
        assertEquals("STONE", item.get("mc_material"), "Item material should match");
        assertEquals(0, item.get("decimals"), "Item decimals should match");
        
        // Test complex query - get instruments with current state
        var instrumentsWithState = db.query("""
            SELECT i.symbol, i.display_name, i.type, s.last_price, s.change_24h
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            ORDER BY i.symbol
            """);
        
        assertEquals(3, instrumentsWithState.size(), "Should have 3 instruments with state");
        
        // Test price history query
        var priceHistory = db.query("""
            SELECT COUNT(*) as count
            FROM instrument_price_history
            """);
        
        assertEquals(1, priceHistory.size(), "Should have one row from count query");
        assertEquals(2, ((Number) priceHistory.get(0).get("count")).intValue(), "Should have 2 price history records");
        
        // Test migration idempotency - running migrations again should not fail
        MigrationRunner migrationRunner = databaseManager.getMigrationRunner();
        migrationRunner.runMigrations(); // Should not throw or create duplicate data
        
        assertEquals(1, migrationRunner.getCurrentVersion(), "Should still be at version 1");
        
        System.out.println("✅ Integration test completed successfully!");
        System.out.println("Database created at: " + expectedDbFile.getAbsolutePath());
        System.out.println("Schema version: " + migrationRunner.getCurrentVersion());
        System.out.println("Total instruments: " + instruments.size());
    }
}