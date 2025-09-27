package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

/**
 * Debug test to isolate simulation engine persistence issues.
 */
class SimulationEngineDebugTest {

    @Test
    void testSingleTick() throws SQLException {
        // Create a manual database config
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/debug_simulation.db");

        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();

        try {
            Db database = databaseManager.getDb();

            // Create market service with test stocks
            StockMarketService marketService = new StockMarketService();
            marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
            System.out.println("Added AAPL to market service");

            // Create simulation engine
            SimulationEngine simulationEngine = new SimulationEngine(marketService, database);

            // Instead of starting the full scheduler, manually execute one tick
            System.out.println("Executing manual tick...");

            // Run a single market update
            marketService.updateAllStockPrices();
            System.out.println("Updated stock prices");

            // Create instrument persistence service and ensure the stock exists
            InstrumentPersistenceService instrumentService = new InstrumentPersistenceService(database);
            var stock = marketService.getStock("AAPL").orElseThrow();
            String instrumentId = instrumentService.ensureInstrument(stock);
            System.out.println("Ensured instrument: " + instrumentId);

            // Manually test the persistence
            try {
                database.executeTransaction(db -> {
                    long currentTime = System.currentTimeMillis();

                    // UPSERT instrument_state
                    db.execute("""
                        INSERT OR REPLACE INTO instrument_state 
                        (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        instrumentId,
                        stock.getCurrentPrice(),
                        stock.getDailyVolume(),
                        0.0, // change_1h
                        0.0, // change_24h
                        0.0, // volatility_24h
                        stock.getMarketCap(),
                        currentTime
                    );

                    // INSERT instrument_price_history
                    db.execute("""
                        INSERT INTO instrument_price_history 
                        (id, instrument_id, ts, price, volume, reason)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                        java.util.UUID.randomUUID().toString(),
                        instrumentId,
                        currentTime,
                        stock.getCurrentPrice(),
                        stock.getDailyVolume(),
                        "MANUAL_TEST"
                    );

                    System.out.println("✅ Manual transaction completed");
                });

            } catch (Exception e) {
                System.err.println("❌ Transaction failed: " + e.getMessage());
                e.printStackTrace();
            }

            // Verify results
            var instruments = database.query("SELECT * FROM instruments WHERE type = 'EQUITY'");
            var states = database.query("SELECT * FROM instrument_state");
            var history = database.query("SELECT * FROM instrument_price_history");

            System.out.println("\nResults:");
            System.out.println("- Instruments: " + instruments.size());
            System.out.println("- States: " + states.size());
            System.out.println("- History entries: " + history.size());

            // Show details
            if (!instruments.isEmpty()) {
                System.out.println("\nInstruments:");
                for (var inst : instruments) {
                    System.out.println("  " + inst.get("symbol") + " -> " + inst.get("id"));
                }
            }

            if (!states.isEmpty()) {
                System.out.println("\nStates:");
                for (var state : states) {
                    System.out.println("  ID: " + state.get("instrument_id") + ", Price: $" + state.get("last_price"));
                }
            }

            if (!history.isEmpty()) {
                System.out.println("\nHistory:");
                for (var hist : history) {
                    System.out.println("  ID: " + hist.get("instrument_id") + ", Price: $" + hist.get("price") + ", Reason: " + hist.get("reason"));
                }
            }

        } finally {
            databaseManager.shutdown();
        }
    }
}