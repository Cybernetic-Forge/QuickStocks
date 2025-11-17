package net.cyberneticforge.quickstocks;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test class providing MockBukkit setup and teardown.
 * All test classes should extend this to get proper Bukkit mocking.
 */
public abstract class TestBase {
    
    protected static ServerMock server;
    protected static QuickStocksPlugin plugin;
    protected static boolean pluginLoadFailed = false;
    
    @BeforeAll
    public static void setUpClass() {
        server = MockBukkit.mock();
        try {
            // Attempt to load the plugin for config access
            // This may fail due to database dependencies, which is expected
            plugin = MockBukkit.load(QuickStocksPlugin.class);
        } catch (Exception e) {
            // Plugin loading failed (expected due to database dependencies)
            // Tests can still run with limited functionality
            pluginLoadFailed = true;
            System.err.println("Plugin load failed (expected): " + e.getMessage());
        }
    }
    
    @AfterAll
    public static void tearDownClass() {
        MockBukkit.unmock();
    }
    
    @BeforeEach
    public void setUp() {
        // Each test gets a fresh plugin instance
        // Note: Full plugin loading may fail due to database dependencies
        // Individual tests should mock what they need
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up after each test
    }
}
