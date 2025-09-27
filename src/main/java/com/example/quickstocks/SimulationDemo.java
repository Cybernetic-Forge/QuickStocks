package com.example.quickstocks;

import com.example.quickstocks.core.services.SimulationEngine;
import com.example.quickstocks.core.services.StockMarketService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * Demonstration application showing the simulation engine in action.
 */
public class SimulationDemo {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🚀 QuickStocks Simulation Engine Demo");
        System.out.println("=====================================");
        
        // Create database configuration
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("demo_simulation.db");
        
        System.out.println("📁 Database: demo_simulation.db");
        
        // Initialize database
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        
        try {
            Db database = databaseManager.getDb();
            
            // Create market service with demo stocks
            StockMarketService marketService = new StockMarketService();
            marketService.addStock("AAPL", "Apple Inc.", "technology", 150.0);
            marketService.addStock("GOOGL", "Alphabet Inc.", "technology", 2800.0);
            marketService.addStock("TSLA", "Tesla Inc.", "technology", 800.0);
            marketService.addStock("MSFT", "Microsoft Corp.", "technology", 400.0);
            marketService.addStock("AMZN", "Amazon.com Inc.", "consumer", 140.0);
            
            System.out.println("📊 Added 5 demo stocks to market");
            
            // Create simulation engine
            SimulationEngine simulationEngine = new SimulationEngine(marketService, database);
            
            // Show initial state
            showDatabaseStats(database);
            
            System.out.println("\n⏰ Starting simulation engine (5-second ticks)...");
            System.out.println("Press Enter to stop the simulation and see final results");
            
            // Start simulation
            simulationEngine.start();
            
            // Wait for user input
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            
            // Stop simulation
            simulationEngine.stop();
            System.out.println("⏹️  Simulation stopped");
            
            // Show final results
            System.out.println("\n📈 Final Results:");
            showDatabaseStats(database);
            showMarketState(database);
            showRecentHistory(database);
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n✅ Demo completed");
        }
    }
    
    private static void showDatabaseStats(Db database) throws SQLException {
        var instruments = database.query("SELECT COUNT(*) as count FROM instruments WHERE type = 'EQUITY'");
        var states = database.query("SELECT COUNT(*) as count FROM instrument_state");
        var history = database.query("SELECT COUNT(*) as count FROM instrument_price_history");
        
        int instrumentCount = ((Number) instruments.get(0).get("count")).intValue();
        int stateCount = ((Number) states.get(0).get("count")).intValue();
        int historyCount = ((Number) history.get(0).get("count")).intValue();
        
        System.out.println("📊 Database Stats:");
        System.out.println("  - Instruments: " + instrumentCount);
        System.out.println("  - States: " + stateCount);
        System.out.println("  - History entries: " + historyCount);
    }
    
    private static void showMarketState(Db database) throws SQLException {
        var results = database.query("""
            SELECT i.symbol, i.display_name, s.last_price, s.change_1h, s.change_24h, s.volatility_24h
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            WHERE i.type = 'EQUITY'
            ORDER BY i.symbol
            """);
        
        System.out.println("\n💰 Current Market State:");
        System.out.printf("%-6s %-20s %10s %8s %8s %10s%n", "Symbol", "Name", "Price", "1h %", "24h %", "Vol 24h");
        System.out.println("─".repeat(70));
        
        for (var row : results) {
            String symbol = (String) row.get("symbol");
            String name = (String) row.get("display_name");
            double price = ((Number) row.get("last_price")).doubleValue();
            double change1h = ((Number) row.get("change_1h")).doubleValue();
            double change24h = ((Number) row.get("change_24h")).doubleValue();
            double volatility = ((Number) row.get("volatility_24h")).doubleValue();
            
            System.out.printf("%-6s %-20s $%8.2f %7.2f%% %7.2f%% %9.4f%n", 
                symbol, name.substring(0, Math.min(name.length(), 20)), 
                price, change1h * 100, change24h * 100, volatility);
        }
    }
    
    private static void showRecentHistory(Db database) throws SQLException {
        var results = database.query("""
            SELECT i.symbol, h.price, h.reason, datetime(h.ts/1000, 'unixepoch', 'localtime') as time
            FROM instrument_price_history h
            JOIN instruments i ON h.instrument_id = i.id
            WHERE i.type = 'EQUITY'
            ORDER BY h.ts DESC
            LIMIT 10
            """);
        
        System.out.println("\n📋 Recent Price History (last 10 entries):");
        System.out.printf("%-6s %10s %-20s %s%n", "Symbol", "Price", "Reason", "Time");
        System.out.println("─".repeat(70));
        
        for (var row : results) {
            String symbol = (String) row.get("symbol");
            double price = ((Number) row.get("price")).doubleValue();
            String reason = (String) row.get("reason");
            String time = (String) row.get("time");
            
            System.out.printf("%-6s $%8.2f %-20s %s%n", 
                symbol, price, reason.substring(0, Math.min(reason.length(), 20)), time);
        }
    }
}