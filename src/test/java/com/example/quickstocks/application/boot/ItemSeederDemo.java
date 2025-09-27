package com.example.quickstocks.application.boot;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Manual demonstration of the ItemSeeder functionality.
 * This can be run to verify the seeder works as expected.
 */
public class ItemSeederDemo {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🚀 ItemSeeder Demo");
        System.out.println("==================");
        
        // Load configuration (defaults to SQLite)
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        System.out.println("Database Provider: " + config.getProvider());
        System.out.println("SQLite File: " + config.getSqliteFile());
        
        // Initialize database system (this will run migrations and seed items)
        DatabaseManager databaseManager = new DatabaseManager(config);
        databaseManager.initialize();
        
        try {
            Db db = databaseManager.getDb();
            
            // Verify items were seeded
            System.out.println("\n📊 Item Seeding Results:");
            
            ItemSeeder itemSeeder = new ItemSeeder(db);
            int itemCount = itemSeeder.getItemInstrumentCount();
            System.out.println("Total ITEM instruments: " + itemCount);
            
            // Show some sample items
            System.out.println("\n📈 Sample ITEM Instruments:");
            System.out.println("Symbol           Display Name        Material         Price");
            System.out.println("------------------------------------------------------------");
            
            var results = db.query("""
                SELECT i.symbol, i.display_name, i.mc_material, s.last_price
                FROM instruments i
                JOIN instrument_state s ON i.id = s.instrument_id
                WHERE i.type = 'ITEM'
                ORDER BY i.symbol
                LIMIT 15
                """);
            
            for (var row : results) {
                System.out.printf("%-16s %-18s %-16s $%.2f%n",
                    row.get("symbol"),
                    row.get("display_name"),
                    row.get("mc_material"),
                    ((Number) row.get("last_price")).doubleValue());
            }
            
            // Verify idempotent behavior
            System.out.println("\n🔄 Testing Idempotent Behavior:");
            System.out.println("Running seeder again...");
            
            int countBefore = itemSeeder.getItemInstrumentCount();
            itemSeeder.seedItems();
            int countAfter = itemSeeder.getItemInstrumentCount();
            
            System.out.println("Items before: " + countBefore);
            System.out.println("Items after:  " + countAfter);
            System.out.println("Idempotent: " + (countBefore == countAfter ? "✅ YES" : "❌ NO"));
            
            // Show filtering results
            System.out.println("\n🚫 Filtering Results:");
            Integer airCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol = 'MC_AIR'");
            Integer legacyCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol LIKE 'MC_LEGACY_%'");
            Integer voidAirCount = db.queryValue("SELECT COUNT(*) FROM instruments WHERE symbol = 'MC_VOID_AIR'");
            
            System.out.println("AIR instruments: " + airCount + " (should be 0)");
            System.out.println("Legacy instruments: " + legacyCount + " (should be 0)");
            System.out.println("VOID_AIR instruments: " + voidAirCount + " (should be 0)");
            
            System.out.println("\n✅ Demo completed successfully!");
            System.out.println("Database file created at: " + config.getSqliteFile());
            
        } finally {
            databaseManager.shutdown();
        }
    }
}