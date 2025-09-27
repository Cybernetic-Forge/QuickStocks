package com.example.quickstocks.infrastructure.db;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Manual demonstration of the database functionality.
 * This can be run to verify the database system works as expected.
 */
public class ManualDemo {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🚀 QuickStocks Database Demo");
        System.out.println("============================");
        
        // Load configuration (defaults to SQLite)
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        System.out.println("Database Provider: " + config.getProvider());
        System.out.println("SQLite File: " + config.getSqliteFile());
        
        // Initialize database system
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        
        try {
            Db db = databaseManager.getDb();
            
            // Create sample instruments
            System.out.println("\n📊 Creating sample instruments...");
            
            String stockId = UUID.randomUUID().toString();
            String cryptoId = UUID.randomUUID().toString();
            String itemId = UUID.randomUUID().toString();
            
            // Add sample stock
            db.execute("""
                INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, stockId, "EQUITY", "DEMO", "Demo Stock", 2, System.currentTimeMillis());
            
            // Add sample crypto
            db.execute("""
                INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, cryptoId, "CRYPTO", "DEMOC", "Demo Coin", 8, System.currentTimeMillis());
            
            // Add sample Minecraft item
            db.execute("""
                INSERT INTO instruments (id, type, symbol, display_name, mc_material, decimals, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, itemId, "ITEM", "MC_DIAMOND", "Diamond", "DIAMOND", 0, System.currentTimeMillis());
            
            // Add current state
            System.out.println("💰 Adding current prices...");
            
            db.execute("""
                INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, stockId, 100.50, 10000.0, 2.5, System.currentTimeMillis());
            
            db.execute("""
                INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, cryptoId, 50000.0, 1.5, -0.8, System.currentTimeMillis());
            
            db.execute("""
                INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_24h, updated_at)
                VALUES (?, ?, ?, ?, ?)
                """, itemId, 64.0, 1000.0, 0.0, System.currentTimeMillis());
            
            // Query and display results
            System.out.println("\n📈 Current Market Data:");
            System.out.println("----------------------");
            
            var results = db.query("""
                SELECT i.symbol, i.display_name, i.type, s.last_price, s.change_24h
                FROM instruments i
                JOIN instrument_state s ON i.id = s.instrument_id
                ORDER BY i.type, i.symbol
                """);
            
            for (var row : results) {
                System.out.printf("%-10s %-15s %8s $%10.2f %+6.2f%%\n",
                    row.get("symbol"),
                    row.get("display_name"),
                    row.get("type"),
                    ((Number) row.get("last_price")).doubleValue(),
                    ((Number) row.get("change_24h")).doubleValue());
            }
            
            System.out.println("\n✅ Demo completed successfully!");
            System.out.println("Database file created at: " + config.getSqliteFile());
            
        } finally {
            databaseManager.shutdown();
        }
    }
}