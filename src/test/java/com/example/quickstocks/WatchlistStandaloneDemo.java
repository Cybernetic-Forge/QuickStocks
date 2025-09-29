package com.example.quickstocks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Standalone demo to test watchlist functionality without Bukkit dependencies.
 */
public class WatchlistStandaloneDemo {
    
    private Connection connection;
    
    public static void main(String[] args) {
        WatchlistStandaloneDemo demo = new WatchlistStandaloneDemo();
        
        try {
            demo.setupDatabase();
            demo.addTestData();
            demo.testWatchlistOperations();
            System.out.println("\n=== All tests passed! ===");
        } catch (SQLException e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            demo.cleanup();
        }
    }
    
    private void setupDatabase() throws SQLException {
        System.out.println("=== QuickStocks Watchlist Standalone Demo ===\n");
        System.out.println("Setting up in-memory database...");
        
        // Create in-memory SQLite database
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        
        // Create tables
        createTables();
        System.out.println("Database initialized.");
    }
    
    private void createTables() throws SQLException {
        // Create instruments table
        executeStatement("""
            CREATE TABLE instruments (
              id            TEXT PRIMARY KEY,
              type          TEXT NOT NULL,
              symbol        TEXT NOT NULL UNIQUE,
              display_name  TEXT NOT NULL,
              mc_material   TEXT,
              decimals      INTEGER NOT NULL DEFAULT 0,
              created_by    TEXT,
              created_at    INTEGER NOT NULL
            )
            """);
        
        // Create instrument_state table
        executeStatement("""
            CREATE TABLE instrument_state (
              instrument_id TEXT PRIMARY KEY,
              last_price    REAL NOT NULL,
              last_volume   REAL NOT NULL DEFAULT 0,
              change_1h     REAL NOT NULL DEFAULT 0,
              change_24h    REAL NOT NULL DEFAULT 0,
              volatility_24h REAL NOT NULL DEFAULT 0,
              market_cap    REAL NOT NULL DEFAULT 0,
              updated_at    INTEGER NOT NULL,
              FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
            )
            """);
        
        // Create watchlists table
        executeStatement("""
            CREATE TABLE user_watchlists (
              player_uuid   TEXT NOT NULL,
              instrument_id TEXT NOT NULL,
              added_at      INTEGER NOT NULL,
              PRIMARY KEY (player_uuid, instrument_id),
              FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
            )
            """);
    }
    
    private void addTestData() throws SQLException {
        System.out.println("Adding test instruments...");
        
        // Add test instruments
        String[][] instruments = {
            {"stone-id", "ITEM", "MC_STONE", "Stone"},
            {"iron-id", "ITEM", "MC_IRON_INGOT", "Iron Ingot"},
            {"apple-id", "EQUITY", "AAPL", "Apple Inc."},
            {"gold-id", "ITEM", "MC_GOLD_NUGGET", "Gold Nugget"}
        };
        
        long now = System.currentTimeMillis();
        
        for (String[] instrument : instruments) {
            // Add instrument
            executeUpdate(
                "INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, 0, ?)",
                instrument[0], instrument[1], instrument[2], instrument[3], now
            );
            
            // Add price data
            double price = 10.0 + Math.random() * 90.0; // Random price between 10-100
            double change1h = (Math.random() - 0.5) * 10.0; // Random change -5% to +5%
            double change24h = (Math.random() - 0.5) * 20.0; // Random change -10% to +10%
            
            executeUpdate(
                "INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at) VALUES (?, ?, 100, ?, ?, 0.5, 1000000, ?)",
                instrument[0], price, change1h, change24h, now
            );
        }
        
        System.out.println("Test instruments added: MC_STONE, MC_IRON_INGOT, AAPL, MC_GOLD_NUGGET\n");
    }
    
    private void testWatchlistOperations() throws SQLException {
        System.out.println("=== Testing Watchlist Operations ===\n");
        
        String playerUuid = "test-player-" + UUID.randomUUID();
        System.out.println("Test Player UUID: " + playerUuid + "\n");
        
        // Test 1: Check empty watchlist
        System.out.println("1. Testing empty watchlist:");
        List<WatchlistItem> watchlist = getWatchlist(playerUuid);
        System.out.println("   Watchlist size: " + watchlist.size() + " (expected: 0)");
        assert watchlist.size() == 0 : "Expected empty watchlist";
        System.out.println("   ✓ Empty watchlist test passed\n");
        
        // Test 2: Add instruments to watchlist
        System.out.println("2. Adding instruments to watchlist:");
        String stoneId = getInstrumentIdBySymbol("MC_STONE");
        String appleId = getInstrumentIdBySymbol("AAPL");
        
        boolean added1 = addToWatchlist(playerUuid, stoneId);
        boolean added2 = addToWatchlist(playerUuid, appleId);
        
        System.out.println("   Added MC_STONE: " + added1 + " (expected: true)");
        System.out.println("   Added AAPL: " + added2 + " (expected: true)");
        assert added1 && added2 : "Expected successful additions";
        System.out.println("   ✓ Addition test passed\n");
        
        // Test 3: Check watchlist after adding
        System.out.println("3. Checking watchlist after additions:");
        watchlist = getWatchlist(playerUuid);
        System.out.println("   Watchlist size: " + watchlist.size() + " (expected: 2)");
        assert watchlist.size() == 2 : "Expected 2 items in watchlist";
        
        for (WatchlistItem item : watchlist) {
            System.out.printf("   %s (%s): $%.2f, 24h: %+.2f%%, 1h: %+.2f%%\n",
                item.symbol, item.displayName, item.lastPrice, item.change24h, item.change1h);
        }
        System.out.println("   ✓ Watchlist content test passed\n");
        
        // Test 4: Test duplicate addition
        System.out.println("4. Testing duplicate addition:");
        boolean duplicate = addToWatchlist(playerUuid, stoneId);
        System.out.println("   Added MC_STONE again: " + duplicate + " (expected: false)");
        assert !duplicate : "Expected duplicate addition to fail";
        System.out.println("   ✓ Duplicate prevention test passed\n");
        
        // Test 5: Test membership check
        System.out.println("5. Testing watchlist membership:");
        boolean inWatchlist = isInWatchlist(playerUuid, stoneId);
        boolean notInWatchlist = isInWatchlist(playerUuid, getInstrumentIdBySymbol("MC_GOLD_NUGGET"));
        
        System.out.println("   MC_STONE in watchlist: " + inWatchlist + " (expected: true)");
        System.out.println("   MC_GOLD_NUGGET in watchlist: " + notInWatchlist + " (expected: false)");
        assert inWatchlist && !notInWatchlist : "Expected correct membership results";
        System.out.println("   ✓ Membership test passed\n");
        
        // Test 6: Remove from watchlist
        System.out.println("6. Testing removal from watchlist:");
        boolean removed = removeFromWatchlist(playerUuid, stoneId);
        System.out.println("   Removed MC_STONE: " + removed + " (expected: true)");
        assert removed : "Expected successful removal";
        
        watchlist = getWatchlist(playerUuid);
        System.out.println("   Watchlist size after removal: " + watchlist.size() + " (expected: 1)");
        assert watchlist.size() == 1 : "Expected 1 item after removal";
        System.out.println("   ✓ Removal test passed\n");
        
        // Test 7: Test watchlist count
        System.out.println("7. Testing watchlist count:");
        int count = getWatchlistCount(playerUuid);
        System.out.println("   Watchlist count: " + count + " (expected: 1)");
        assert count == 1 : "Expected count of 1";
        System.out.println("   ✓ Count test passed\n");
        
        // Test 8: Clear watchlist
        System.out.println("8. Testing watchlist clearing:");
        int cleared = clearWatchlist(playerUuid);
        System.out.println("   Items cleared: " + cleared + " (expected: 1)");
        assert cleared == 1 : "Expected 1 item cleared";
        
        count = getWatchlistCount(playerUuid);
        System.out.println("   Final watchlist count: " + count + " (expected: 0)");
        assert count == 0 : "Expected empty watchlist after clearing";
        System.out.println("   ✓ Clear test passed");
    }
    
    // Watchlist service methods (simplified for demo)
    
    private boolean addToWatchlist(String playerUuid, String instrumentId) throws SQLException {
        if (isInWatchlist(playerUuid, instrumentId)) {
            return false;
        }
        
        return executeUpdate(
            "INSERT INTO user_watchlists (player_uuid, instrument_id, added_at) VALUES (?, ?, ?)",
            playerUuid, instrumentId, System.currentTimeMillis()
        ) > 0;
    }
    
    private boolean removeFromWatchlist(String playerUuid, String instrumentId) throws SQLException {
        return executeUpdate(
            "DELETE FROM user_watchlists WHERE player_uuid = ? AND instrument_id = ?",
            playerUuid, instrumentId
        ) > 0;
    }
    
    private boolean isInWatchlist(String playerUuid, String instrumentId) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT 1 FROM user_watchlists WHERE player_uuid = ? AND instrument_id = ?")) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, instrumentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    private List<WatchlistItem> getWatchlist(String playerUuid) throws SQLException {
        List<WatchlistItem> items = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT 
                w.instrument_id,
                w.added_at,
                i.symbol,
                i.display_name,
                i.type,
                s.last_price,
                s.change_24h,
                s.change_1h,
                s.volatility_24h
            FROM user_watchlists w
            JOIN instruments i ON w.instrument_id = i.id
            LEFT JOIN instrument_state s ON w.instrument_id = s.instrument_id
            WHERE w.player_uuid = ?
            ORDER BY w.added_at DESC
            """)) {
            
            stmt.setString(1, playerUuid);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    WatchlistItem item = new WatchlistItem(
                        rs.getString("instrument_id"),
                        rs.getString("symbol"),
                        rs.getString("display_name"),
                        rs.getString("type"),
                        rs.getLong("added_at"),
                        rs.getDouble("last_price"),
                        rs.getDouble("change_24h"),
                        rs.getDouble("change_1h"),
                        rs.getDouble("volatility_24h")
                    );
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    private int getWatchlistCount(String playerUuid) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM user_watchlists WHERE player_uuid = ?")) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
    
    private int clearWatchlist(String playerUuid) throws SQLException {
        return executeUpdate("DELETE FROM user_watchlists WHERE player_uuid = ?", playerUuid);
    }
    
    private String getInstrumentIdBySymbol(String symbol) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
            "SELECT id FROM instruments WHERE UPPER(symbol) = UPPER(?)")) {
            stmt.setString(1, symbol);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("id") : null;
            }
        }
    }
    
    // Utility methods
    
    private void executeStatement(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    private int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }
    
    private void cleanup() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    
    // Simple data class for watchlist items
    static class WatchlistItem {
        final String instrumentId;
        final String symbol;
        final String displayName;
        final String type;
        final long addedAt;
        final double lastPrice;
        final double change24h;
        final double change1h;
        final double volatility24h;
        
        WatchlistItem(String instrumentId, String symbol, String displayName, String type,
                     long addedAt, double lastPrice, double change24h, double change1h, double volatility24h) {
            this.instrumentId = instrumentId;
            this.symbol = symbol;
            this.displayName = displayName;
            this.type = type;
            this.addedAt = addedAt;
            this.lastPrice = lastPrice;
            this.change24h = change24h;
            this.change1h = change1h;
            this.volatility24h = volatility24h;
        }
    }
}