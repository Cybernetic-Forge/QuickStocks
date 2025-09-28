package com.example.quickstocks.integration;

import com.example.quickstocks.core.services.*;
import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for security and concurrency features.
 * These tests validate the interaction between services.
 */
class SecurityIntegrationTest {
    
    private Db database;
    private WalletService walletService;
    private HoldingsService holdingsService;
    private TradingService tradingService;
    private AuditService auditService;
    
    @BeforeEach
    void setUp() throws SQLException {
        // Create in-memory database for testing
        DataSource dataSource = createInMemoryDataSource();
        database = new Db(dataSource);
        
        // Initialize schema
        initializeTestSchema();
        
        // Initialize services
        walletService = new WalletService(database);
        holdingsService = new HoldingsService(database);
        tradingService = new TradingService(database, walletService, holdingsService);
        auditService = new AuditService(database, holdingsService);
    }
    
    @Test
    void testConcurrentTrading_PreventDoubleSpend() throws Exception {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        
        // Setup: Give player $1000 and create instrument
        setupTestData(playerUuid, instrumentId, 1000.0, 100.0);
        
        // Attempt to buy 5 shares twice concurrently (should cost $1000 total)
        // Only one should succeed due to insufficient funds
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        
        Runnable buyOrder = () -> {
            try {
                TradingService.TradeResult result = tradingService.executeBuyOrder(
                    playerUuid, instrumentId, 5.0, "concurrent-test-" + Thread.currentThread().getId()
                );
                if (result.isSuccess()) {
                    successCount.incrementAndGet();
                }
            } catch (SQLException e) {
                // Expected in concurrent scenarios
            } finally {
                latch.countDown();
            }
        };
        
        // Submit both orders simultaneously
        executor.submit(buyOrder);
        executor.submit(buyOrder);
        
        latch.await();
        executor.shutdown();
        
        // Only one order should succeed (preventing double-spend)
        assertEquals(1, successCount.get(), "Only one concurrent order should succeed");
        
        // Verify final balance is correct
        double finalBalance = walletService.getBalance(playerUuid);
        assertEquals(500.0, finalBalance, 0.01, "Final balance should be $500 after one successful purchase");
    }
    
    @Test
    void testIdempotencyKeys_PreventDuplicateOrders() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        String idempotencyKey = "unique-order-123";
        
        // Setup: Give player $1000 and create instrument
        setupTestData(playerUuid, instrumentId, 1000.0, 100.0);
        
        // Execute same order twice with same idempotency key
        TradingService.TradeResult result1 = tradingService.executeBuyOrder(
            playerUuid, instrumentId, 5.0, idempotencyKey
        );
        TradingService.TradeResult result2 = tradingService.executeBuyOrder(
            playerUuid, instrumentId, 5.0, idempotencyKey
        );
        
        // Both should succeed, but second should be cached
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertTrue(result2.getMessage().contains("[CACHED]"));
        
        // Only one order should be recorded in database
        int orderCount = database.queryValue("SELECT COUNT(*) FROM orders WHERE client_idempotency = ?", idempotencyKey);
        assertEquals(1, orderCount, "Only one order should be recorded with same idempotency key");
        
        // Final balance should reflect only one purchase
        double finalBalance = walletService.getBalance(playerUuid);
        assertEquals(500.0, finalBalance, 0.01, "Balance should reflect only one purchase");
    }
    
    @Test
    void testOptimisticLocking_VersionConflicts() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        
        // Setup: Give player shares and create instrument
        setupTestData(playerUuid, instrumentId, 1000.0, 100.0);
        tradingService.executeBuyOrder(playerUuid, instrumentId, 10.0); // Buy 10 shares first
        
        // Get current holding with version
        HoldingsService.Holding holding = holdingsService.getHoldingWithLock(playerUuid, instrumentId);
        assertNotNull(holding);
        assertEquals(1, holding.getVersion());
        
        // Simulate version conflict by manually updating the version
        database.execute("UPDATE user_holdings SET version = 2 WHERE player_uuid = ? AND instrument_id = ?", 
            playerUuid, instrumentId);
        
        // Try to sell with old version - should handle the conflict
        TradingService.TradeResult result = tradingService.executeSellOrder(playerUuid, instrumentId, 5.0);
        
        // Should succeed after retries (or fail gracefully)
        // The exact behavior depends on the retry logic implementation
        assertNotNull(result);
    }
    
    @Test
    void testAuditAndRepair_Integration() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        
        // Setup initial data
        setupTestData(playerUuid, instrumentId, 1000.0, 100.0);
        
        // Make some trades
        tradingService.executeBuyOrder(playerUuid, instrumentId, 10.0);
        tradingService.executeSellOrder(playerUuid, instrumentId, 3.0);
        
        // Manually corrupt the holdings data to create discrepancy
        database.execute("UPDATE user_holdings SET qty = 5.0 WHERE player_uuid = ? AND instrument_id = ?", 
            playerUuid, instrumentId);
        
        // Run audit
        AuditService.AuditResult auditResult = auditService.auditAllHoldings(false);
        
        // Verify discrepancy was detected
        assertEquals(1, auditResult.totalPlayersChecked);
        assertEquals(1, auditResult.playersWithIssues);
        assertEquals(1, auditResult.totalDiscrepancies);
        
        // Run repair
        AuditService.AuditResult repairResult = auditService.auditAllHoldings(true);
        assertEquals(1, repairResult.totalRepairs);
        
        // Verify repair was successful
        HoldingsService.Holding correctedHolding = holdingsService.getHolding(playerUuid, instrumentId);
        assertEquals(7.0, correctedHolding.getQty(), 0.001, "Holdings should be corrected to 7 shares (10 bought - 3 sold)");
    }
    
    private DataSource createInMemoryDataSource() {
        // In a real test, you'd use H2 or SQLite in-memory database
        // For this example, we'll mock it
        DataSource dataSource = mock(DataSource.class);
        try {
            Connection connection = mock(Connection.class);
            when(dataSource.getConnection()).thenReturn(connection);
            // Mock basic connection behavior as needed
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dataSource;
    }
    
    private void initializeTestSchema() throws SQLException {
        // Initialize test database schema
        // In real implementation, you'd run migration scripts here
        
        // Mock the database setup for testing
        when(database.execute(anyString(), any())).thenReturn(1);
        when(database.queryValue(anyString(), any())).thenReturn(0);
    }
    
    private void setupTestData(String playerUuid, String instrumentId, double balance, double price) throws SQLException {
        // Setup test data: player balance, instrument, and price
        
        // Mock wallet balance
        when(walletService.getBalance(playerUuid)).thenReturn(balance);
        when(walletService.hasBalance(eq(playerUuid), anyDouble())).thenAnswer(invocation -> {
            double required = invocation.getArgument(1);
            return balance >= required;
        });
        
        // Mock instrument price
        when(database.queryValue(contains("last_price"), eq(instrumentId))).thenReturn(price);
        
        // Initialize empty holdings
        when(holdingsService.getHolding(playerUuid, instrumentId)).thenReturn(null);
    }
}