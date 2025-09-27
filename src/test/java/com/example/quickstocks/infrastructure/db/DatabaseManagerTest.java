package com.example.quickstocks.infrastructure.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the database infrastructure components.
 */
public class DatabaseManagerTest {
    
    @TempDir
    File tempDir;
    
    private DatabaseManager databaseManager;
    private DatabaseConfig config;
    
    @BeforeEach
    void setUp() {
        // Configure SQLite for testing
        config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(new File(tempDir, "test.db").getAbsolutePath());
        
        databaseManager = new DatabaseManager(config);
    }
    
    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testDatabaseInitialization() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        
        // Verify database file was created
        assertTrue(new File(config.getSqliteFile()).exists(), "Database file should be created");
        
        // Verify tables exist
        Db db = databaseManager.getDb();
        assertTrue(db.tableExists("instruments"), "instruments table should exist");
        assertTrue(db.tableExists("instrument_state"), "instrument_state table should exist");
        assertTrue(db.tableExists("instrument_price_history"), "instrument_price_history table should exist");
        assertTrue(db.tableExists("schema_version"), "schema_version table should exist");
    }
    
    @Test
    void testMigrations() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        
        // Check migration was applied
        MigrationRunner migrationRunner = databaseManager.getMigrationRunner();
        int currentVersion = migrationRunner.getCurrentVersion();
        assertEquals(1, currentVersion, "Migration V1 should be applied");
        
        // Verify schema_version table has our migration
        Db db = databaseManager.getDb();
        List<Map<String, Object>> migrations = db.query("SELECT * FROM schema_version WHERE success = true");
        assertEquals(1, migrations.size(), "Should have one successful migration");
        
        Map<String, Object> migration = migrations.get(0);
        assertEquals(1, migration.get("version"), "Migration version should be 1");
        assertEquals("init", migration.get("name"), "Migration name should be 'init'");
        // SQLite stores boolean as integer, so check for truthy value
        Object successValue = migration.get("success");
        assertTrue(successValue.equals(1) || successValue.equals(true), "Migration should be successful");
    }
    
    @Test
    void testInstrumentOperations() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // Insert test instrument
        String instrumentId = UUID.randomUUID().toString();
        String symbol = "TEST_STOCK";
        String displayName = "Test Stock";
        String type = "EQUITY";
        long createdAt = System.currentTimeMillis();
        
        int affectedRows = db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, instrumentId, type, symbol, displayName, 2, createdAt);
        
        assertEquals(1, affectedRows, "Should insert one row");
        
        // Query the instrument back
        Map<String, Object> instrument = db.queryOne(
            "SELECT * FROM instruments WHERE symbol = ?", symbol);
        
        assertNotNull(instrument, "Should find the instrument");
        assertEquals(instrumentId, instrument.get("id"), "ID should match");
        assertEquals(symbol, instrument.get("symbol"), "Symbol should match");
        assertEquals(displayName, instrument.get("display_name"), "Display name should match");
        assertEquals(type, instrument.get("type"), "Type should match");
        assertEquals(2, instrument.get("decimals"), "Decimals should match");
    }
    
    @Test
    void testInstrumentStateOperations() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // First create an instrument
        String instrumentId = UUID.randomUUID().toString();
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, instrumentId, "EQUITY", "TEST", "Test", 2, System.currentTimeMillis());
        
        // Insert instrument state
        double price = 100.50;
        double volume = 1000.0;
        double change24h = 5.25;
        long updatedAt = System.currentTimeMillis();
        
        int affectedRows = db.execute("""
            INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """, instrumentId, price, volume, change24h, updatedAt);
        
        assertEquals(1, affectedRows, "Should insert one row");
        
        // Query the state back
        Map<String, Object> state = db.queryOne(
            "SELECT * FROM instrument_state WHERE instrument_id = ?", instrumentId);
        
        assertNotNull(state, "Should find the instrument state");
        assertEquals(instrumentId, state.get("instrument_id"), "Instrument ID should match");
        assertEquals(price, (Double) state.get("last_price"), 0.001, "Price should match");
        assertEquals(volume, (Double) state.get("last_volume"), 0.001, "Volume should match");
        assertEquals(change24h, (Double) state.get("change_24h"), 0.001, "Change should match");
    }
    
    @Test
    void testPriceHistoryOperations() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // First create an instrument
        String instrumentId = UUID.randomUUID().toString();
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """, instrumentId, "EQUITY", "TEST", "Test", 2, System.currentTimeMillis());
        
        // Insert price history records
        String historyId1 = UUID.randomUUID().toString();
        String historyId2 = UUID.randomUUID().toString();
        long ts1 = System.currentTimeMillis() - 3600000; // 1 hour ago
        long ts2 = System.currentTimeMillis();
        
        db.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """, historyId1, instrumentId, ts1, 95.0, 500.0, "MARKET_OPEN");
        
        db.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """, historyId2, instrumentId, ts2, 100.0, 750.0, "EARNINGS_REPORT");
        
        // Query price history
        List<Map<String, Object>> history = db.query("""
            SELECT * FROM instrument_price_history 
            WHERE instrument_id = ? 
            ORDER BY ts ASC
            """, instrumentId);
        
        assertEquals(2, history.size(), "Should have 2 history records");
        
        Map<String, Object> record1 = history.get(0);
        assertEquals(95.0, (Double) record1.get("price"), 0.001, "First price should match");
        assertEquals("MARKET_OPEN", record1.get("reason"), "First reason should match");
        
        Map<String, Object> record2 = history.get(1);
        assertEquals(100.0, (Double) record2.get("price"), 0.001, "Second price should match");
        assertEquals("EARNINGS_REPORT", record2.get("reason"), "Second reason should match");
    }
    
    @Test
    void testTransactionRollback() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        String instrumentId = UUID.randomUUID().toString();
        
        // Test transaction that should rollback
        assertThrows(SQLException.class, () -> {
            db.executeTransaction(txDb -> {
                // Insert valid instrument
                txDb.execute("""
                    INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, instrumentId, "EQUITY", "TEST", "Test", 2, System.currentTimeMillis());
                
                // This should fail due to constraint violation (duplicate symbol)
                txDb.execute("""
                    INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID().toString(), "EQUITY", "TEST", "Test2", 2, System.currentTimeMillis());
            });
        });
        
        // Verify nothing was committed
        Map<String, Object> result = db.queryOne("SELECT COUNT(*) as count FROM instruments");
        assertEquals(0, ((Number) result.get("count")).intValue(), "No instruments should be inserted due to rollback");
    }
}