package com.example.quickstocks.infrastructure;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import com.example.quickstocks.infrastructure.web.HealthCheckService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * Demo application showing the health check service and new features in action.
 */
public class HealthCheckDemo {
    
    public static void main(String[] args) throws SQLException, IOException {
        System.out.println("🚀 QuickStocks Health Check & Features Demo");
        System.out.println("==========================================");
        
        // Load configuration with new features
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        config.setSqliteFile("/tmp/health_check_demo.db");
        
        System.out.println("📋 Configuration:");
        System.out.println("  Database Provider: " + config.getProvider());
        System.out.println("  SQLite File: " + config.getSqliteFile());
        System.out.println("  History Enabled: " + config.isHistoryEnabled());
        System.out.println("  Top List Window: " + config.getTopListWindowHours() + " hours");
        
        // Initialize database with indices
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        
        try {
            Db db = databaseManager.getDb();
            
            // Demonstrate indices and performance features
            demonstrateIndicesAndPerformance(db);
            
            // Start health check service
            int port = 8080;
            HealthCheckService healthService = new HealthCheckService(db, port);
            healthService.start();
            
            System.out.println("\n🌐 Health Check Service Started!");
            System.out.println("  Endpoint: http://localhost:" + port + "/stocks/pingdb");
            System.out.println("\n💡 Try these commands in another terminal:");
            System.out.println("  curl http://localhost:" + port + "/stocks/pingdb");
            System.out.println("  curl -s http://localhost:" + port + "/stocks/pingdb | jq .");
            
            System.out.println("\n⏸️  Press Enter to stop the demo...");
            Scanner scanner = new Scanner(System.in);
            scanner.nextLine();
            
            healthService.stop();
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n✅ Demo completed successfully!");
        }
    }
    
    private static void demonstrateIndicesAndPerformance(Db db) throws SQLException {
        System.out.println("\n📊 Demonstrating Indices and Performance Features:");
        
        // Create some test data to show the indices working
        System.out.println("  Creating test instruments...");
        
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN", "META", "NVDA", "NFLX", "CRM", "ORCL"};
        String[] names = {"Apple Inc.", "Alphabet Inc.", "Microsoft Corp.", "Tesla Inc.", "Amazon.com Inc.", 
                         "Meta Platforms", "NVIDIA Corp.", "Netflix Inc.", "Salesforce Inc.", "Oracle Corp."};
        double[] changes = {5.25, -2.75, 3.50, -1.25, 7.80, 2.15, -4.60, 1.90, 6.40, -0.85};
        
        for (int i = 0; i < symbols.length; i++) {
            String instrumentId = UUID.randomUUID().toString();
            
            // Insert instrument
            db.execute("INSERT OR REPLACE INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                instrumentId, "EQUITY", symbols[i], names[i], 2, System.currentTimeMillis());
            
            // Insert state with performance data
            double price = 100 + (Math.random() * 500);
            db.execute("INSERT OR REPLACE INTO instrument_state (instrument_id, last_price, change_24h, updated_at) VALUES (?, ?, ?, ?)",
                instrumentId, price, changes[i], System.currentTimeMillis());
        }
        
        System.out.println("  ✅ Created " + symbols.length + " test instruments");
        
        // Demonstrate the top performers query (uses the change_24h index)
        System.out.println("\n📈 Top 5 Performers (24h change) - using optimized index:");
        List<Map<String, Object>> topPerformers = db.query("""
            SELECT i.symbol, i.display_name, s.last_price, s.change_24h
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            ORDER BY s.change_24h DESC
            LIMIT 5
            """);
        
        System.out.println("    Symbol  | Name                 | Price   | Change");
        System.out.println("    --------|---------------------|---------|--------");
        for (Map<String, Object> row : topPerformers) {
            System.out.printf("    %-7s | %-19s | $%6.2f | %+5.2f%%\n",
                row.get("symbol"),
                row.get("display_name"),
                ((Number) row.get("last_price")).doubleValue(),
                ((Number) row.get("change_24h")).doubleValue());
        }
        
        // Demonstrate the bottom performers
        System.out.println("\n📉 Bottom 3 Performers (24h change):");
        List<Map<String, Object>> bottomPerformers = db.query("""
            SELECT i.symbol, i.display_name, s.last_price, s.change_24h
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            ORDER BY s.change_24h ASC
            LIMIT 3
            """);
        
        System.out.println("    Symbol  | Name                 | Price   | Change");
        System.out.println("    --------|---------------------|---------|--------");
        for (Map<String, Object> row : bottomPerformers) {
            System.out.printf("    %-7s | %-19s | $%6.2f | %+5.2f%%\n",
                row.get("symbol"),
                row.get("display_name"),
                ((Number) row.get("last_price")).doubleValue(),
                ((Number) row.get("change_24h")).doubleValue());
        }
    }
}