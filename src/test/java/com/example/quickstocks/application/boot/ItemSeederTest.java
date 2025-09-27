package com.example.quickstocks.application.boot;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ItemSeeder functionality.
 */
class ItemSeederTest {
    
    private DatabaseManager databaseManager;
    private Db db;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Use in-memory SQLite for testing
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        db = databaseManager.getDb();
    }
    
    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testSeedItemsCreatesInstruments() throws SQLException {
        ItemSeeder seeder = new ItemSeeder(db);
        
        // Get initial count
        int initialCount = seeder.getItemInstrumentCount();
        
        // Run seeder
        seeder.seedItems();
        
        // Verify items were created
        int finalCount = seeder.getItemInstrumentCount();
        assertTrue(finalCount > initialCount, "Should have created new ITEM instruments");
        
        // Verify at least some expected items exist
        List<Map<String, Object>> results = db.query(
            "SELECT symbol, display_name, mc_material FROM instruments WHERE type = 'ITEM' ORDER BY symbol LIMIT 10"
        );
        
        assertFalse(results.isEmpty(), "Should have created ITEM instruments");
        
        // Check a few specific items
        boolean foundStone = false;
        boolean foundDiamond = false;
        
        for (Map<String, Object> item : results) {
            String symbol = (String) item.get("symbol");
            String displayName = (String) item.get("display_name");
            String mcMaterial = (String) item.get("mc_material");
            
            // Verify symbol format
            assertTrue(symbol.startsWith("MC_"), "Symbol should start with MC_");
            
            // Verify display name format
            assertFalse(displayName.contains("_"), "Display name should not contain underscores");
            assertTrue(Character.isUpperCase(displayName.charAt(0)), "Display name should be capitalized");
            
            // Check for specific items
            if ("MC_STONE".equals(symbol)) {
                foundStone = true;
                assertEquals("Stone", displayName);
                assertEquals("STONE", mcMaterial);
            } else if ("MC_DIAMOND".equals(symbol)) {
                foundDiamond = true;
                assertEquals("Diamond", displayName);
                assertEquals("DIAMOND", mcMaterial);
            }
        }
        
        assertTrue(foundStone, "Should have created MC_STONE instrument");
        assertTrue(foundDiamond, "Should have created MC_DIAMOND instrument");
    }
    
    @Test
    void testSeedItemsCreatesInstrumentState() throws SQLException {
        ItemSeeder seeder = new ItemSeeder(db);
        
        // Run seeder
        seeder.seedItems();
        
        // Verify instrument_state rows were created
        List<Map<String, Object>> stateResults = db.query("""
            SELECT s.last_price, s.updated_at, i.symbol 
            FROM instrument_state s 
            JOIN instruments i ON s.instrument_id = i.id 
            WHERE i.type = 'ITEM' 
            LIMIT 5
            """);
        
        assertFalse(stateResults.isEmpty(), "Should have created instrument_state rows for ITEM instruments");
        
        for (Map<String, Object> state : stateResults) {
            Double lastPrice = ((Number) state.get("last_price")).doubleValue();
            Long updatedAt = ((Number) state.get("updated_at")).longValue();
            String symbol = (String) state.get("symbol");
            
            assertEquals(1.00, lastPrice, 0.001, "Last price should be 1.00 for " + symbol);
            assertTrue(updatedAt > 0, "Updated at should be set for " + symbol);
        }
    }
    
    @Test
    void testSeedItemsIsIdempotent() throws SQLException {
        ItemSeeder seeder = new ItemSeeder(db);
        
        // Run seeder first time
        seeder.seedItems();
        int firstCount = seeder.getItemInstrumentCount();
        
        // Run seeder second time
        seeder.seedItems();
        int secondCount = seeder.getItemInstrumentCount();
        
        assertEquals(firstCount, secondCount, "Seeder should be idempotent - no new items on second run");
    }
    
    @Test
    void testSeedItemsFiltersCorrectly() throws SQLException {
        ItemSeeder seeder = new ItemSeeder(db);
        
        // Run seeder
        seeder.seedItems();
        
        // Verify no AIR or legacy items were created
        Integer airCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol = 'MC_AIR'");
        Integer legacyCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol LIKE 'MC_LEGACY_%'");
        Integer voidAirCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol = 'MC_VOID_AIR'");
        
        assertEquals(0, airCount.intValue(), "Should not create AIR instrument");
        assertEquals(0, legacyCount.intValue(), "Should not create legacy instruments");
        assertEquals(0, voidAirCount.intValue(), "Should not create VOID_AIR instrument");
        
        // Verify all created instruments have type = 'ITEM'
        List<Map<String, Object>> nonItemTypes = db.query("SELECT type FROM instruments WHERE type != 'ITEM'");
        
        // This should be ok - there might be other instrument types in the system
        // Just verify that all our seeded items are of type ITEM
        List<Map<String, Object>> mcItems = db.query("SELECT type FROM instruments WHERE symbol LIKE 'MC_%' AND type != 'ITEM'");
        assertTrue(mcItems.isEmpty(), "All MC_ prefixed instruments should be of type ITEM");
    }
    
    @Test
    void testInstrumentProperties() throws SQLException {
        ItemSeeder seeder = new ItemSeeder(db);
        
        // Run seeder
        seeder.seedItems();
        
        // Get a sample instrument
        Map<String, Object> instrument = db.queryOne(
            "SELECT * FROM instruments WHERE type = 'ITEM' AND symbol = 'MC_STONE'"
        );
        
        assertNotNull(instrument, "Should find MC_STONE instrument");
        assertEquals("ITEM", instrument.get("type"));
        assertEquals("MC_STONE", instrument.get("symbol"));
        assertEquals("Stone", instrument.get("display_name"));
        assertEquals("STONE", instrument.get("mc_material"));
        assertEquals(0, ((Number) instrument.get("decimals")).intValue());
        assertNotNull(instrument.get("id"));
        assertTrue(((Number) instrument.get("created_at")).longValue() > 0);
    }
}