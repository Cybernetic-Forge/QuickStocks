package com.example.quickstocks.infrastructure.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to debug migration issues.
 */
public class SimpleMigrationTest {
    
    @TempDir
    File tempDir;
    
    private DatabaseManager databaseManager;
    private DatabaseConfig config;
    
    @BeforeEach
    void setUp() {
        config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(new File(tempDir, "test.db").getAbsolutePath());
        
        databaseManager = new DatabaseManager(config, false); // Disable auto-seeding for tests
    }
    
    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
    
    @Test
    void testBasicInit() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        
        // Verify database file was created
        assertTrue(new File(config.getSqliteFile()).exists(), "Database file should be created");
        
        // Verify we can get a DB connection
        Db db = databaseManager.getDb();
        assertNotNull(db, "DB should not be null");
        
        // Just test basic connection
        Map<String, Object> result = db.queryOne("SELECT 1 as test");
        assertNotNull(result, "Should get a result");
        assertEquals(1, result.get("test"), "Should return 1");
    }
    
    @Test
    void testManualTableCreation() throws SQLException {
        // Initialize database
        databaseManager.initialize();
        Db db = databaseManager.getDb();
        
        // Manually create a simple table first
        db.execute("""
            CREATE TABLE IF NOT EXISTS test_table (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL
            )
            """);
        
        // Test table exists
        assertTrue(db.tableExists("test_table"), "Test table should exist");
        
        // Insert a record
        db.execute("INSERT INTO test_table (id, name) VALUES (?, ?)", "test-id", "test-name");
        
        // Query it back
        Map<String, Object> result = db.queryOne("SELECT * FROM test_table WHERE id = ?", "test-id");
        assertNotNull(result, "Should find the record");
        assertEquals("test-id", result.get("id"), "ID should match");
        assertEquals("test-name", result.get("name"), "Name should match");
    }
}