package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for InstrumentPersistenceService to verify instrument mapping works correctly.
 */
class InstrumentPersistenceServiceTest {

    private DatabaseManager databaseManager;
    private Db database;
    private InstrumentPersistenceService instrumentService;
    private StockMarketService marketService;

    @BeforeEach
    void setUp() throws SQLException {
        // Create a manual database config to avoid YAML parsing issues
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/test_instruments.db");

        databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        database = databaseManager.getDb();
        
        instrumentService = new InstrumentPersistenceService(database);
        marketService = new StockMarketService();
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    @Test
    void testEnsureInstrument() throws SQLException {
        // Create a test stock
        marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
        var stock = marketService.getStock("AAPL").orElseThrow();

        // Ensure the instrument
        String instrumentId = instrumentService.ensureInstrument(stock);
        assertNotNull(instrumentId, "Should create instrument ID");

        // Verify in database
        var results = database.query("SELECT * FROM instruments WHERE id = ?", instrumentId);
        assertEquals(1, results.size(), "Should find the created instrument");

        var instrument = results.get(0);
        assertEquals("AAPL", instrument.get("symbol"));
        assertEquals("Apple Inc.", instrument.get("display_name"));
        assertEquals("EQUITY", instrument.get("type"));
        assertEquals(2, instrument.get("decimals"));

        System.out.println("✅ Created instrument: " + instrumentId + " for " + stock.getSymbol());
    }

    @Test
    void testEnsureInstrumentIdempotent() throws SQLException {
        // Create a test stock
        marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
        var stock = marketService.getStock("GOOGL").orElseThrow();

        // Ensure the instrument twice
        String instrumentId1 = instrumentService.ensureInstrument(stock);
        String instrumentId2 = instrumentService.ensureInstrument(stock);

        assertEquals(instrumentId1, instrumentId2, "Should return same ID for same stock");

        // Verify only one record in database
        var results = database.query("SELECT * FROM instruments WHERE symbol = 'GOOGL'");
        assertEquals(1, results.size(), "Should have only one instrument record");

        System.out.println("✅ Idempotent instrument creation works");
    }

    @Test
    void testMultipleInstruments() throws SQLException {
        // Create multiple stocks
        marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
        marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
        marketService.addStock("TSLA", "Tesla Inc.", "technology", 800.0);

        // Ensure all instruments
        var stocks = marketService.getAllStocks();
        assertEquals(3, stocks.size(), "Should have 3 stocks");

        for (var stock : stocks) {
            String instrumentId = instrumentService.ensureInstrument(stock);
            assertNotNull(instrumentId, "Should create ID for " + stock.getSymbol());
        }

        // Verify all in database
        var instruments = database.query("SELECT * FROM instruments WHERE type = 'EQUITY'");
        assertEquals(3, instruments.size(), "Should have 3 instruments in database");

        System.out.println("✅ Created " + instruments.size() + " instruments");
        for (var instrument : instruments) {
            System.out.println("  - " + instrument.get("symbol") + " -> " + instrument.get("id"));
        }
    }
}