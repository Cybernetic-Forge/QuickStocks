package com.example.quickstocks.application.queries;

import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the QueryService functionality.
 */
public class QueryServiceTest {
    
    @TempDir
    Path tempDir;
    
    private DatabaseManager databaseManager;
    private QueryService queryService;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create test database
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(tempDir.resolve("test.db").toString());
        
        databaseManager = new DatabaseManager(config, true); // Enable seeding
        databaseManager.initialize();
        
        queryService = new QueryService(databaseManager.getDb());
        
        // Add some test data
        addTestInstruments();
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    private void addTestInstruments() throws Exception {
        Db db = databaseManager.getDb();
        
        // Insert test instruments with unique symbols
        db.execute("""
            INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_at)
            VALUES 
                ('test1', 'ITEM', 'TEST_STONE', 'Test Stone', 'STONE', 0, ?),
                ('test2', 'ITEM', 'TEST_IRON', 'Test Iron Ingot', 'IRON_INGOT', 0, ?),
                ('test3', 'EQUITY', 'TEST_AAPL', 'Test Apple Inc.', NULL, 2, ?)
            """, System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
        
        // Insert corresponding state records
        db.execute("""
            INSERT INTO instrument_state 
            (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
            VALUES 
                ('test1', 10.50, 1000, 0.05, 0.15, 0.2, 50000, ?),
                ('test2', 25.75, 500, -0.02, 0.08, 0.15, 30000, ?),
                ('test3', 150.25, 10000, 0.01, 0.25, 0.18, 2500000, ?)
            """, System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
        
        // Insert some price history
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            db.execute("""
                INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "hist_test1_" + i, "test1", now - (i * 60000), 10.0 + i * 0.1, 100, "Test");
        }
    }
    
    @Test
    void testGetTopGainersByChange24h() throws Exception {
        List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
        
        assertFalse(gainers.isEmpty(), "Should have gainers");
        assertTrue(gainers.size() >= 3, "Should have at least 3 instruments");
        
        // Should be ordered by change_24h DESC - find our test data
        Map<String, Object> topTestGainer = gainers.stream()
                .filter(g -> "TEST_AAPL".equals(g.get("symbol")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(topTestGainer, "Should find TEST_AAPL in gainers");
        assertEquals(0.25, ((Number) topTestGainer.get("change_24h")).doubleValue(), 0.001);
    }
    
    @Test
    void testFindBySymbol() throws Exception {
        // Test case-insensitive symbol lookup
        Optional<Map<String, Object>> result = queryService.findBySymbol("test_stone");
        assertTrue(result.isPresent(), "Should find TEST_STONE by lowercase");
        assertEquals("TEST_STONE", result.get().get("symbol"));
        assertEquals("Test Stone", result.get().get("display_name"));
        
        // Test exact match
        result = queryService.findBySymbol("TEST_AAPL");
        assertTrue(result.isPresent(), "Should find TEST_AAPL");
        assertEquals("TEST_AAPL", result.get().get("symbol"));
        
        // Test not found
        result = queryService.findBySymbol("NONEXISTENT");
        assertFalse(result.isPresent(), "Should not find nonexistent symbol");
    }
    
    @Test
    void testFindByMcMaterial() throws Exception {
        // Test case-insensitive material lookup
        Optional<Map<String, Object>> result = queryService.findByMcMaterial("stone");
        assertTrue(result.isPresent(), "Should find by lowercase material name");  
        // Could be TEST_STONE or MC_STONE from seeding
        assertNotNull(result.get().get("symbol"), "Should have a symbol");
        assertEquals("STONE", result.get().get("mc_material"));
        
        // Test exact match
        result = queryService.findByMcMaterial("IRON_INGOT");
        assertTrue(result.isPresent(), "Should find by exact material name");
        assertNotNull(result.get().get("symbol"), "Should have a symbol");
        
        // Test not found
        result = queryService.findByMcMaterial("NONEXISTENT_MATERIAL");
        assertFalse(result.isPresent(), "Should not find nonexistent material");
    }
    
    @Test
    void testGetRecentPriceHistory() throws Exception {
        List<Map<String, Object>> history = queryService.getRecentPriceHistory("test1", 3);
        
        assertFalse(history.isEmpty(), "Should have price history");
        assertEquals(3, history.size(), "Should limit to 3 entries");
        
        // Should be ordered by timestamp DESC (most recent first)
        Map<String, Object> latest = history.get(0);
        Map<String, Object> oldest = history.get(2);
        
        long latestTs = ((Number) latest.get("ts")).longValue();
        long oldestTs = ((Number) oldest.get("ts")).longValue();
        
        assertTrue(latestTs > oldestTs, "Should be ordered by timestamp DESC");
    }
    
    @Test
    void testGetAllSymbols() throws Exception {
        List<String> symbols = queryService.getAllSymbols();
        
        assertFalse(symbols.isEmpty(), "Should have symbols");
        assertTrue(symbols.contains("TEST_STONE"), "Should contain TEST_STONE");
        assertTrue(symbols.contains("TEST_IRON"), "Should contain TEST_IRON");
        assertTrue(symbols.contains("TEST_AAPL"), "Should contain TEST_AAPL");
    }
    
    @Test
    void testGetAllMaterials() throws Exception {
        List<String> materials = queryService.getAllMaterials();
        
        assertFalse(materials.isEmpty(), "Should have materials");
        assertTrue(materials.contains("STONE"), "Should contain STONE");
        assertTrue(materials.contains("IRON_INGOT"), "Should contain IRON_INGOT");
        assertFalse(materials.contains(null), "Should not contain null materials");
    }
    
    @Test
    void testGetMatchingSymbolsAndMaterials() throws Exception {
        // Test prefix matching
        List<String> matches = queryService.getMatchingSymbolsAndMaterials("TEST");
        assertFalse(matches.isEmpty(), "Should have matches");
        assertTrue(matches.contains("TEST_STONE") || matches.contains("TEST_IRON"), 
                  "Should contain symbols starting with TEST");
        
        // Test case-insensitive matching
        matches = queryService.getMatchingSymbolsAndMaterials("st");
        assertTrue(matches.size() > 0, "Should have matches for 'st'");
        
        // Test empty prefix
        matches = queryService.getMatchingSymbolsAndMaterials("");
        assertTrue(matches.size() >= 5, "Should return all symbols and materials for empty prefix");
    }
    
    @Test
    void testAcceptanceCriteria() throws Exception {
        // Test acceptance criteria: "/stocks renders 10 lines with correct ordering"
        List<Map<String, Object>> gainers = queryService.getTopGainersByChange24h(10);
        
        // Should be ordered by change_24h DESC
        for (int i = 0; i < gainers.size() - 1; i++) {
            double current = ((Number) gainers.get(i).get("change_24h")).doubleValue();
            double next = ((Number) gainers.get(i + 1).get("change_24h")).doubleValue();
            assertTrue(current >= next, "Should be ordered by change_24h DESC");
        }
        
        // Test acceptance criteria: "/stocks MC_STONE and /stocks stone both resolve to stone"
        // Use seeded data for this test
        Optional<Map<String, Object>> bySymbol = queryService.findBySymbol("MC_STONE");
        Optional<Map<String, Object>> byMaterial = queryService.findByMcMaterial("stone");
        
        assertTrue(bySymbol.isPresent(), "Should find by symbol MC_STONE");
        assertTrue(byMaterial.isPresent(), "Should find by material stone");
        
        // Both should resolve to stone material (though could be different instruments)
        String symbolMaterial = (String) bySymbol.get().get("mc_material");
        String materialMaterial = (String) byMaterial.get().get("mc_material");
        assertEquals("STONE", symbolMaterial, "Symbol lookup should have STONE material");
        assertEquals("STONE", materialMaterial, "Material lookup should have STONE material");
    }
}