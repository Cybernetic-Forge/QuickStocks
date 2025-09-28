package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.core.services.WatchlistService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Demo application to test watchlist functionality.
 */
public class WatchlistDemo {
    
    public static void main(String[] args) {
        System.out.println("=== QuickStocks Watchlist Demo ===\n");
        
        try {
            // Initialize database
            DatabaseConfig config = new DatabaseConfig();
            config.setSqliteFile("quickstocks_watchlist_demo.db");
            
            DatabaseManager databaseManager = new DatabaseManager(config, true);
            databaseManager.initialize();
            
            // Initialize services
            Db db = databaseManager.getDb();
            QueryService queryService = new QueryService(db);
            WatchlistService watchlistService = new WatchlistService(databaseManager);
            
            // Create a test player UUID
            String playerUuid = UUID.randomUUID().toString();
            System.out.println("Test Player UUID: " + playerUuid);
            System.out.println();
            
            // Simulate adding instruments to database (normally done by seeding)
            addTestInstruments(db);
            
            // Test watchlist operations
            testWatchlistOperations(watchlistService, queryService, playerUuid);
            
            System.out.println("\n=== Demo Complete ===");
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void addTestInstruments(Db db) throws SQLException {
        System.out.println("Setting up test instruments...");
        
        // Add some test instruments if they don't exist
        String[] instruments = {
            "INSERT OR IGNORE INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, 'ITEM', 'MC_STONE', 'Stone', 0, ?)",
            "INSERT OR IGNORE INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, 'ITEM', 'MC_IRON_INGOT', 'Iron Ingot', 0, ?)",
            "INSERT OR IGNORE INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, 'EQUITY', 'AAPL', 'Apple Inc.', 2, ?)"
        };
        
        String[] symbols = {"MC_STONE", "MC_IRON_INGOT", "AAPL"};
        long now = System.currentTimeMillis();
        
        for (int i = 0; i < instruments.length; i++) {
            String instrumentId = UUID.randomUUID().toString();
            db.execute(instruments[i], instrumentId, now);
            
            // Add some test price data
            db.execute("INSERT OR IGNORE INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at) VALUES (?, ?, 100, ?, ?, 0.5, 1000000, ?)",
                instrumentId, 
                10.0 + (i * 5.0), // Different prices
                (i % 2 == 0 ? 2.5 : -1.5), // 1h change
                (i % 2 == 0 ? 5.0 : -3.0), // 24h change
                now
            );
        }
        
        System.out.println("Test instruments added: " + String.join(", ", symbols));
        System.out.println();
    }
    
    private static void testWatchlistOperations(WatchlistService watchlistService, QueryService queryService, String playerUuid) throws SQLException {
        System.out.println("=== Testing Watchlist Operations ===");
        
        // Test 1: Check empty watchlist
        System.out.println("1. Initial watchlist check:");
        List<WatchlistService.WatchlistItem> watchlist = watchlistService.getWatchlist(playerUuid);
        System.out.println("   Watchlist size: " + watchlist.size());
        System.out.println("   Expected: 0");
        System.out.println();
        
        // Test 2: Add instruments to watchlist
        System.out.println("2. Adding instruments to watchlist:");
        String[] symbolsToAdd = {"MC_STONE", "AAPL"};
        
        for (String symbol : symbolsToAdd) {
            String instrumentId = queryService.getInstrumentIdBySymbol(symbol);
            if (instrumentId != null) {
                boolean added = watchlistService.addToWatchlist(playerUuid, instrumentId);
                System.out.println("   Added " + symbol + ": " + added);
            } else {
                System.out.println("   Could not find instrument: " + symbol);
            }
        }
        System.out.println();
        
        // Test 3: Check watchlist after adding
        System.out.println("3. Watchlist after adding instruments:");
        watchlist = watchlistService.getWatchlist(playerUuid);
        System.out.println("   Watchlist size: " + watchlist.size());
        
        for (WatchlistService.WatchlistItem item : watchlist) {
            System.out.printf("   %s (%s): $%.2f, 24h: %+.2f%%, 1h: %+.2f%%\n",
                item.getSymbol(),
                item.getDisplayName(),
                item.getLastPrice(),
                item.getChange24h(),
                item.getChange1h()
            );
        }
        System.out.println();
        
        // Test 4: Try to add duplicate
        System.out.println("4. Testing duplicate addition:");
        String stoneId = queryService.getInstrumentIdBySymbol("MC_STONE");
        boolean duplicate = watchlistService.addToWatchlist(playerUuid, stoneId);
        System.out.println("   Added MC_STONE again: " + duplicate + " (should be false)");
        System.out.println();
        
        // Test 5: Check if instrument is in watchlist
        System.out.println("5. Testing watchlist membership:");
        boolean inWatchlist = watchlistService.isInWatchlist(playerUuid, stoneId);
        System.out.println("   MC_STONE in watchlist: " + inWatchlist + " (should be true)");
        System.out.println();
        
        // Test 6: Remove from watchlist
        System.out.println("6. Removing from watchlist:");
        boolean removed = watchlistService.removeFromWatchlist(playerUuid, stoneId);
        System.out.println("   Removed MC_STONE: " + removed + " (should be true)");
        
        watchlist = watchlistService.getWatchlist(playerUuid);
        System.out.println("   Watchlist size after removal: " + watchlist.size());
        System.out.println();
        
        // Test 7: Get watchlist count
        System.out.println("7. Watchlist count test:");
        int count = watchlistService.getWatchlistCount(playerUuid);
        System.out.println("   Watchlist count: " + count);
        System.out.println();
        
        // Test 8: Clear watchlist
        System.out.println("8. Clearing watchlist:");
        int cleared = watchlistService.clearWatchlist(playerUuid);
        System.out.println("   Items cleared: " + cleared);
        
        count = watchlistService.getWatchlistCount(playerUuid);
        System.out.println("   Final watchlist count: " + count + " (should be 0)");
    }
}