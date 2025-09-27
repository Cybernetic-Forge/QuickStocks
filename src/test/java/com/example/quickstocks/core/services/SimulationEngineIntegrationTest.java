package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify the simulation engine works with database persistence.
 * This test is designed to be run manually with appropriate setup.
 */
class SimulationEngineIntegrationTest {
    
    @Test
    void testManualSimulationRun() throws SQLException, InterruptedException {
        // Create a manual database config to avoid YAML parsing issues
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/test_simulation.db");
        
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        
        try {
            Db database = databaseManager.getDb();
            
            // Create market service with some test stocks
            StockMarketService marketService = new StockMarketService();
            marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
            marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
            marketService.addStock("TSLA", "Tesla Inc.", "technology", 800.0);
            
            // Create and start simulation engine
            SimulationEngine simulationEngine = new SimulationEngine(marketService, database);
            
            System.out.println("Starting simulation for 15 seconds...");
            simulationEngine.start();
            
            // Let it run for a few ticks
            Thread.sleep(15000); // 15 seconds = 3 ticks (5s each)
            
            simulationEngine.stop();
            System.out.println("Simulation stopped");
            
            // Verify database contents
            var instruments = database.query("SELECT * FROM instruments WHERE type = 'EQUITY'");
            var states = database.query("SELECT * FROM instrument_state");
            var history = database.query("SELECT * FROM instrument_price_history");
            
            System.out.println("Results:");
            System.out.println("- Instruments created: " + instruments.size());
            System.out.println("- States maintained: " + states.size());
            System.out.println("- History entries: " + history.size());
            
            // Basic assertions
            assertEquals(3, instruments.size(), "Should have 3 instruments");
            assertEquals(3, states.size(), "Should have 3 states");
            assertTrue(history.size() >= 9, "Should have at least 3 history entries per instrument (3 ticks × 3 stocks)");
            
            // Show some sample data
            System.out.println("\nSample instrument state:");
            for (var state : states) {
                System.out.println("- " + database.query(
                    "SELECT symbol FROM instruments WHERE id = ?", 
                    state.get("instrument_id")
                ).get(0).get("symbol") + ": $" + state.get("last_price"));
            }
            
            System.out.println("\nPrice history count by instrument:");
            var historyCounts = database.query("""
                SELECT i.symbol, COUNT(h.id) as count 
                FROM instruments i 
                LEFT JOIN instrument_price_history h ON i.id = h.instrument_id 
                WHERE i.type = 'EQUITY'
                GROUP BY i.id, i.symbol
                """);
            
            for (var count : historyCounts) {
                System.out.println("- " + count.get("symbol") + ": " + count.get("count") + " entries");
            }
            
            // Test rolling window calculations
            System.out.println("\nTesting rolling window calculations:");
            String instrumentId = (String) instruments.get(0).get("id");
            double change1h = simulationEngine.getChangePercent(instrumentId, 60);
            double volatility = simulationEngine.getVolatility(instrumentId, 60);
            
            System.out.println("- 1-hour change: " + (change1h * 100) + "%");
            System.out.println("- Volatility: " + volatility);
            
            assertTrue(history.size() > 0, "Should have price history for calculations");
            
        } finally {
            databaseManager.shutdown();
        }
    }
}