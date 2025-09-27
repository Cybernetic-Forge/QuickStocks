package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SimulationEngine to verify database persistence functionality.
 */
class SimulationEngineTest {
    
    private DatabaseManager databaseManager;
    private Db database;
    private StockMarketService marketService;
    private SimulationEngine simulationEngine;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory database for testing
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        database = databaseManager.getDb();
        
        // Create market service with test stocks
        marketService = new StockMarketService();
        marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
        marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
        marketService.addStock("TSLA", "Tesla Inc.", "technology", 800.0);
        
        // Create simulation engine
        simulationEngine = new SimulationEngine(marketService, database);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (simulationEngine != null && simulationEngine.isRunning()) {
            simulationEngine.stop();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testBasicPersistence() throws Exception {
        // Start simulation for a brief period
        simulationEngine.start();
        
        // Wait for at least one tick to complete
        Thread.sleep(6000); // Wait 6 seconds for one 5-second tick
        
        simulationEngine.stop();
        
        // Verify instruments were created
        var instruments = database.query("SELECT * FROM instruments WHERE type = 'EQUITY'");
        assertEquals(3, instruments.size(), "Should have created 3 instruments");
        
        // Verify instrument state exists
        var states = database.query("SELECT * FROM instrument_state");
        assertEquals(3, states.size(), "Should have state for all 3 instruments");
        
        // Verify price history exists
        var history = database.query("SELECT * FROM instrument_price_history");
        assertTrue(history.size() >= 3, "Should have at least one history entry per instrument");
        
        System.out.println("Created " + instruments.size() + " instruments");
        System.out.println("Created " + states.size() + " states");
        System.out.println("Created " + history.size() + " history entries");
    }
    
    @Test
    void testInstrumentMapping() throws Exception {
        // Test that stocks are properly mapped to instruments
        InstrumentPersistenceService instrumentService = new InstrumentPersistenceService(database);
        
        var stock = marketService.getStock("AAPL").orElseThrow();
        String instrumentId = instrumentService.ensureInstrument(stock);
        
        assertNotNull(instrumentId, "Should create instrument ID");
        
        // Verify instrument was created in database
        var results = database.query("SELECT * FROM instruments WHERE id = ?", instrumentId);
        assertEquals(1, results.size(), "Should find the created instrument");
        
        var instrument = results.get(0);
        assertEquals("AAPL", instrument.get("symbol"));
        assertEquals("Apple Inc.", instrument.get("display_name"));
        assertEquals("EQUITY", instrument.get("type"));
        assertEquals(2, instrument.get("decimals"));
    }
    
    @Test
    void testRollingWindowCalculations() throws Exception {
        // Add some test data to calculate rolling windows
        InstrumentPersistenceService instrumentService = new InstrumentPersistenceService(database);
        var stock = marketService.getStock("AAPL").orElseThrow();
        String instrumentId = instrumentService.ensureInstrument(stock);
        
        // Insert some historical data
        long now = System.currentTimeMillis();
        long oneHourAgo = now - (60 * 60 * 1000L);
        
        database.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """, 
            java.util.UUID.randomUUID().toString(), 
            instrumentId, 
            oneHourAgo, 
            140.0, 
            1000.0, 
            "TEST_DATA"
        );
        
        database.execute("""
            INSERT INTO instrument_price_history (id, instrument_id, ts, price, volume, reason)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            java.util.UUID.randomUUID().toString(),
            instrumentId,
            now - (30 * 60 * 1000L), // 30 minutes ago
            145.0,
            1000.0,
            "TEST_DATA"
        );
        
        // Test change percent calculation
        double change1h = simulationEngine.getChangePercent(instrumentId, 60);
        assertTrue(change1h > 0, "Should show positive change from 140 to 150");
        
        // Test volatility calculation
        double volatility = simulationEngine.getVolatility(instrumentId, 60);
        assertTrue(volatility >= 0, "Volatility should be non-negative");
        
        System.out.println("1-hour change: " + (change1h * 100) + "%");
        System.out.println("1-hour volatility: " + volatility);
    }
}