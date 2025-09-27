package com.example.quickstocks.infrastructure;

import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.infrastructure.db.Db;
import com.example.quickstocks.infrastructure.web.HealthCheckService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for health check service and new configuration features.
 */
public class HealthCheckAndConfigTest {
    
    @TempDir
    File tempDir;
    
    private DatabaseManager databaseManager;
    private DatabaseConfig config;
    private HealthCheckService healthCheckService;
    private final int testPort = 18080; // Use a high port for testing
    
    @BeforeEach
    void setUp() {
        // Create test database configuration
        config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(new File(tempDir, "test.db").getAbsolutePath());
        
        // Disable seeding for clean tests
        databaseManager = new DatabaseManager(config, false);
    }
    
    @AfterEach
    void tearDown() {
        if (healthCheckService != null) {
            healthCheckService.stop();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testConfigurationLoading() {
        // Test that configuration includes new features
        DatabaseConfig loadedConfig = ConfigLoader.loadDatabaseConfig();
        
        // Verify feature defaults
        assertTrue(loadedConfig.isHistoryEnabled(), "History should be enabled by default");
        assertEquals(24, loadedConfig.getTopListWindowHours(), "Top list window should be 24 hours by default");
    }
    
    @Test
    void testDatabaseIndicesCreation() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // Create some test instruments to verify indices
        String instrumentId1 = UUID.randomUUID().toString();
        String instrumentId2 = UUID.randomUUID().toString();
        String instrumentId3 = UUID.randomUUID().toString();
        
        // Insert test instruments
        db.execute("INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            instrumentId1, "EQUITY", "TEST1", "Test Stock 1", 2, System.currentTimeMillis());
        db.execute("INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            instrumentId2, "EQUITY", "TEST2", "Test Stock 2", 2, System.currentTimeMillis());
        db.execute("INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            instrumentId3, "EQUITY", "TEST3", "Test Stock 3", 2, System.currentTimeMillis());
        
        // Insert instrument states with different change_24h values
        db.execute("INSERT INTO instrument_state (instrument_id, last_price, change_24h, updated_at) VALUES (?, ?, ?, ?)",
            instrumentId1, 100.0, 5.25, System.currentTimeMillis());
        db.execute("INSERT INTO instrument_state (instrument_id, last_price, change_24h, updated_at) VALUES (?, ?, ?, ?)",
            instrumentId2, 200.0, -2.75, System.currentTimeMillis());
        db.execute("INSERT INTO instrument_state (instrument_id, last_price, change_24h, updated_at) VALUES (?, ?, ?, ?)",
            instrumentId3, 300.0, 8.50, System.currentTimeMillis());
        
        // Test that we can query the top performers efficiently (this would use the index)
        List<Map<String, Object>> topPerformers = db.query("""
            SELECT i.symbol, i.display_name, s.last_price, s.change_24h
            FROM instruments i
            JOIN instrument_state s ON i.id = s.instrument_id
            ORDER BY s.change_24h DESC
            LIMIT 10
            """);
        
        assertEquals(3, topPerformers.size(), "Should return 3 test instruments");
        
        // Verify ordering (highest change_24h first)
        Map<String, Object> first = topPerformers.get(0);
        assertEquals(8.50, (Double) first.get("change_24h"), 0.01, "First should be highest change");
        assertEquals("TEST3", first.get("symbol"), "First should be TEST3");
        
        Map<String, Object> second = topPerformers.get(1);
        assertEquals(5.25, (Double) second.get("change_24h"), 0.01, "Second should be middle change");
        assertEquals("TEST1", second.get("symbol"), "Second should be TEST1");
        
        Map<String, Object> third = topPerformers.get(2);
        assertEquals(-2.75, (Double) third.get("change_24h"), 0.01, "Third should be lowest change");
        assertEquals("TEST2", third.get("symbol"), "Third should be TEST2");
    }
    
    @Test
    void testHealthCheckServiceHealthy() throws SQLException, IOException, InterruptedException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // Start health check service
        healthCheckService = new HealthCheckService(db, testPort);
        healthCheckService.start();
        
        // Give the server a moment to start
        Thread.sleep(500);
        
        // Test health check endpoint
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/stocks/pingdb"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(200, response.statusCode(), "Health check should return 200 for healthy database");
        
        String responseBody = response.body();
        assertTrue(responseBody.contains("\"status\": \"healthy\""), "Response should indicate healthy status");
        assertTrue(responseBody.contains("\"database\": \"connected\""), "Response should indicate database is connected");
        assertTrue(responseBody.contains("✅"), "Response should contain green check mark");
    }
    
    @Test
    void testHealthCheckServiceUnhealthy() throws IOException, InterruptedException {
        // Create a health check service with a null/broken database
        Db brokenDb = null;
        healthCheckService = new HealthCheckService(brokenDb, testPort);
        healthCheckService.start();
        
        // Give the server a moment to start
        Thread.sleep(500);
        
        // Test health check endpoint
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + testPort + "/stocks/pingdb"))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(500, response.statusCode(), "Health check should return 500 for broken database");
        
        String responseBody = response.body();
        assertTrue(responseBody.contains("\"status\": \"error\""), "Response should indicate error status");
        assertTrue(responseBody.contains("❌"), "Response should contain red cross");
    }
    
    @Test
    void testUniqueSymbolConstraint() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // Insert first instrument
        String instrumentId1 = UUID.randomUUID().toString();
        db.execute("INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            instrumentId1, "EQUITY", "UNIQUE_SYMBOL", "First Stock", 2, System.currentTimeMillis());
        
        // Try to insert another instrument with the same symbol - should fail
        String instrumentId2 = UUID.randomUUID().toString();
        assertThrows(SQLException.class, () -> {
            db.execute("INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                instrumentId2, "EQUITY", "UNIQUE_SYMBOL", "Second Stock", 2, System.currentTimeMillis());
        }, "Should not allow duplicate symbols");
    }
}