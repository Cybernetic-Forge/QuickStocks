package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.CryptoService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstration of the crypto creation functionality.
 */
public class CryptoCreationDemo {
    
    public static void main(String[] args) throws SQLException {
        System.out.println("🪙 QuickStocks Crypto Creation Demo");
        System.out.println("====================================\n");
        
        // Create database configuration
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile("/tmp/crypto_demo.db");
        
        // Initialize database
        DatabaseManager databaseManager = new DatabaseManager(config, true);
        databaseManager.initialize();
        
        try {
            CryptoService cryptoService = new CryptoService(databaseManager.getDb());
            QueryService queryService = new QueryService(databaseManager.getDb());
            
            System.out.println("📊 Demonstrating crypto creation functionality:\n");
            
            // Simulate different players creating cryptos
            String player1 = UUID.randomUUID().toString();
            String player2 = UUID.randomUUID().toString();
            
            // Demo 1: Create a valid crypto
            System.out.println("1️⃣  Creating valid crypto:");
            System.out.println("   Command: /crypto create MYCOIN \"My Custom Coin\"");
            System.out.println("   Player: " + player1.substring(0, 8) + "...");
            
            try {
                String instrumentId1 = cryptoService.createCustomCrypto("MYCOIN", "My Custom Coin", player1);
                System.out.println("   ✅ Success! Created instrument: " + instrumentId1);
                
                // Show the created crypto details
                var crypto1 = queryService.findBySymbol("MYCOIN");
                if (crypto1.isPresent()) {
                    displayCryptoInfo(crypto1.get());
                }
            } catch (Exception e) {
                System.out.println("   ❌ Error: " + e.getMessage());
            }
            
            System.out.println("\n" + "─".repeat(60) + "\n");
            
            // Demo 2: Create another crypto with different parameters
            System.out.println("2️⃣  Creating another crypto:");
            System.out.println("   Command: /crypto create GOLD \"Digital Gold Token\"");
            System.out.println("   Player: " + player2.substring(0, 8) + "...");
            
            try {
                String instrumentId2 = cryptoService.createCustomCrypto("GOLD", "Digital Gold Token", player2);
                System.out.println("   ✅ Success! Created instrument: " + instrumentId2);
                
                var crypto2 = queryService.findBySymbol("GOLD");
                if (crypto2.isPresent()) {
                    displayCryptoInfo(crypto2.get());
                }
            } catch (Exception e) {
                System.out.println("   ❌ Error: " + e.getMessage());
            }
            
            System.out.println("\n" + "─".repeat(60) + "\n");
            
            // Demo 3: Try to create duplicate symbol
            System.out.println("3️⃣  Attempting to create duplicate symbol:");
            System.out.println("   Command: /crypto create MYCOIN \"Another Coin\"");
            System.out.println("   Player: " + player2.substring(0, 8) + "...");
            
            try {
                cryptoService.createCustomCrypto("MYCOIN", "Another Coin", player2);
                System.out.println("   ❌ This shouldn't succeed!");
            } catch (Exception e) {
                System.out.println("   ✅ Correctly rejected: " + e.getMessage());
            }
            
            System.out.println("\n" + "─".repeat(60) + "\n");
            
            // Demo 4: Try invalid symbols
            System.out.println("4️⃣  Testing invalid symbols:");
            
            String[] invalidSymbols = {"A", "VERYLONGSYMBOL", "", "test-coin!@#"};
            String[] expectedResults = {"Too short", "Too long", "Empty", "Will be normalized to TESTCOIN"};
            
            for (int i = 0; i < invalidSymbols.length; i++) {
                System.out.println("   Symbol: \"" + invalidSymbols[i] + "\" - " + expectedResults[i]);
                try {
                    String result = cryptoService.createCustomCrypto(invalidSymbols[i], "Test Coin", player1);
                    System.out.println("   ✅ Success (normalized): " + result);
                } catch (Exception e) {
                    System.out.println("   ❌ Rejected: " + e.getMessage());
                }
                System.out.println();
            }
            
            System.out.println("─".repeat(60) + "\n");
            
            // Demo 5: Show all created cryptos
            System.out.println("5️⃣  All created custom cryptos:");
            var allCryptos = databaseManager.getDb().query("SELECT * FROM instruments WHERE type = 'CUSTOM_CRYPTO'");
            
            if (allCryptos.isEmpty()) {
                System.out.println("   No custom cryptos found.");
            } else {
                System.out.println("   Found " + allCryptos.size() + " custom crypto(s):");
                for (var crypto : allCryptos) {
                    System.out.println("   • " + crypto.get("symbol") + " (" + crypto.get("display_name") + ")");
                    System.out.println("     Created by: " + crypto.get("created_by"));
                    System.out.println("     ID: " + crypto.get("id"));
                    System.out.println();
                }
            }
            
            System.out.println("─".repeat(60) + "\n");
            
            // Demo 6: Permission simulation
            System.out.println("6️⃣  Permission system simulation:");
            System.out.println("   Player with permission 'maksy.stocks.crypto.create': CAN create crypto");
            System.out.println("   Player without permission: CANNOT create crypto");
            System.out.println("   (This would be handled by the CryptoCommand class in-game)");
            
        } finally {
            databaseManager.shutdown();
            System.out.println("\n🎉 Demo completed successfully!");
        }
    }
    
    private static void displayCryptoInfo(Map<String, Object> crypto) {
        System.out.println("     📄 Crypto Details:");
        System.out.println("     • Symbol: " + crypto.get("symbol"));
        System.out.println("     • Name: " + crypto.get("display_name"));
        System.out.println("     • Type: " + crypto.get("type"));
        System.out.println("     • Decimals: " + crypto.get("decimals"));
        System.out.println("     • Starting Price: $" + crypto.get("last_price"));
        System.out.println("     • Created: " + new java.util.Date((Long) crypto.get("created_at")));
    }
}