package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Working test for SimulationEngine using manual ticks.
 */
class SimulationEngineWorkingTest {

    @Test
    void testManualTicks() throws SQLException {
        // Create a manual database config
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/working_simulation.db");

        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();

        try {
            Db database = databaseManager.getDb();

            // Create market service with test stocks
            StockMarketService marketService = new StockMarketService();
            marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
            marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
            marketService.addStock("TSLA", "Tesla Inc.", "technology", 800.0);

            // Create simulation engine
            SimulationEngine simulationEngine = new SimulationEngine(marketService, database);

            System.out.println("Executing 3 manual ticks...");

            // Execute 3 manual ticks
            for (int i = 1; i <= 3; i++) {
                System.out.println("Tick " + i + "...");
                simulationEngine.manualTick();
                
                // Small delay to ensure different timestamps
                try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
            }

            // Verify results
            var instruments = database.query("SELECT * FROM instruments WHERE type = 'EQUITY'");
            var states = database.query("SELECT * FROM instrument_state");
            var history = database.query("SELECT * FROM instrument_price_history");

            System.out.println("\nResults after 3 ticks:");
            System.out.println("- Instruments: " + instruments.size());
            System.out.println("- States: " + states.size());
            System.out.println("- History entries: " + history.size());

            // Assertions
            assertEquals(3, instruments.size(), "Should have 3 instruments");
            assertEquals(3, states.size(), "Should have 3 states (one per instrument)");
            assertEquals(9, history.size(), "Should have 9 history entries (3 ticks × 3 stocks)");

            // Show some sample data
            System.out.println("\nInstrument states:");
            for (var state : states) {
                var symbolQuery = database.query("SELECT symbol FROM instruments WHERE id = ?", state.get("instrument_id"));
                String symbol = (String) symbolQuery.get(0).get("symbol");
                System.out.println("- " + symbol + ": $" + state.get("last_price"));
            }

            // Show history count per instrument
            System.out.println("\nHistory counts per instrument:");
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
            String instrumentId = (String) instruments.get(0).get("id");
            double change1h = simulationEngine.getChangePercent(instrumentId, 60);
            double volatility = simulationEngine.getVolatility(instrumentId, 60);

            System.out.println("\nRolling window calculations:");
            System.out.println("- 1-hour change: " + (change1h * 100) + "%");
            System.out.println("- Volatility: " + volatility);

            System.out.println("\n✅ Simulation engine working correctly!");

        } finally {
            databaseManager.shutdown();
        }
    }
}