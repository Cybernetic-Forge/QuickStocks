package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TradingServiceIdempotencyTest {
    
    @Mock
    private Db database;
    
    @Mock
    private WalletService walletService;
    
    @Mock
    private HoldingsService holdingsService;
    
    private TradingService tradingService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradingService = new TradingService(database, walletService, holdingsService);
    }
    
    @Test
    void testExecuteBuyOrder_IdempotencyKey_NewOrder() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        double qty = 10.0;
        String idempotencyKey = "unique-key-123";
        
        // Mock no existing order with this idempotency key
        when(database.queryRow(contains("client_idempotency"), eq(idempotencyKey)))
            .thenReturn(null);
        
        // Mock current price
        when(database.queryValue(contains("last_price"), eq(instrumentId)))
            .thenReturn(100.0);
        
        // Mock wallet has sufficient balance
        when(walletService.hasBalance(eq(playerUuid), eq(1000.0)))
            .thenReturn(true);
        when(walletService.removeBalance(eq(playerUuid), eq(1000.0)))
            .thenReturn(true);
        
        // Mock transaction execution
        doAnswer(invocation -> {
            Db.TransactionBlock block = invocation.getArgument(0);
            Db.TransactionDb transactionDb = mock(Db.TransactionDb.class);
            when(transactionDb.execute(anyString(), any())).thenReturn(1);
            block.execute(transactionDb);
            return null;
        }).when(database).executeTransaction(any());
        
        // Mock holdings update
        when(holdingsService.addHoldingWithVersioning(any(), eq(playerUuid), eq(instrumentId), eq(qty), eq(100.0)))
            .thenReturn(true);
        
        TradingService.TradeResult result = tradingService.executeBuyOrder(playerUuid, instrumentId, qty, idempotencyKey);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("BUY 10.00 shares"));
        assertFalse(result.getMessage().contains("[CACHED]"));
        
        verify(database).executeTransaction(any());
    }
    
    @Test
    void testExecuteBuyOrder_IdempotencyKey_DuplicateOrder() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        double qty = 10.0;
        String idempotencyKey = "duplicate-key-123";
        
        // Mock existing order with this idempotency key
        Map<String, Object> existingOrder = Map.of(
            "side", "BUY",
            "qty", 10.0,
            "price", 100.0
        );
        when(database.queryRow(contains("client_idempotency"), eq(idempotencyKey)))
            .thenReturn(existingOrder);
        
        TradingService.TradeResult result = tradingService.executeBuyOrder(playerUuid, instrumentId, qty, idempotencyKey);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("BUY 10.00 shares"));
        assertTrue(result.getMessage().contains("[CACHED]"));
        
        // Should not execute transaction for duplicate order
        verify(database, never()).executeTransaction(any());
        verify(walletService, never()).removeBalance(anyString(), anyDouble());
    }
    
    @Test
    void testExecuteSellOrder_IdempotencyKey_NewOrder() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        double qty = 5.0;
        String idempotencyKey = "unique-sell-key-456";
        
        // Mock no existing order with this idempotency key
        when(database.queryRow(contains("client_idempotency"), eq(idempotencyKey)))
            .thenReturn(null);
        
        // Mock current price
        when(database.queryValue(contains("last_price"), eq(instrumentId)))
            .thenReturn(100.0);
        
        // Mock sufficient holdings
        HoldingsService.Holding mockHolding = new HoldingsService.Holding(
            instrumentId, "AAPL", "Apple Inc.", 10.0, 90.0, 100.0, 1
        );
        when(holdingsService.getHoldingWithLock(eq(playerUuid), eq(instrumentId)))
            .thenReturn(mockHolding);
        
        // Mock transaction execution
        doAnswer(invocation -> {
            Db.TransactionBlock block = invocation.getArgument(0);
            Db.TransactionDb transactionDb = mock(Db.TransactionDb.class);
            when(transactionDb.execute(anyString(), any())).thenReturn(1);
            block.execute(transactionDb);
            return null;
        }).when(database).executeTransaction(any());
        
        // Mock holdings update
        when(holdingsService.removeHoldingWithVersioning(any(), eq(playerUuid), eq(instrumentId), eq(qty), eq(1)))
            .thenReturn(true);
        
        TradingService.TradeResult result = tradingService.executeSellOrder(playerUuid, instrumentId, qty, idempotencyKey);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("SELL 5.00 shares"));
        assertFalse(result.getMessage().contains("[CACHED]"));
        
        verify(database).executeTransaction(any());
    }
    
    @Test
    void testExecuteSellOrder_IdempotencyKey_DuplicateOrder() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        double qty = 5.0;
        String idempotencyKey = "duplicate-sell-key-789";
        
        // Mock existing order with this idempotency key
        Map<String, Object> existingOrder = Map.of(
            "side", "SELL",
            "qty", 5.0,
            "price", 100.0
        );
        when(database.queryRow(contains("client_idempotency"), eq(idempotencyKey)))
            .thenReturn(existingOrder);
        
        TradingService.TradeResult result = tradingService.executeSellOrder(playerUuid, instrumentId, qty, idempotencyKey);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("SELL 5.00 shares"));
        assertTrue(result.getMessage().contains("[CACHED]"));
        
        // Should not execute transaction for duplicate order
        verify(database, never()).executeTransaction(any());
        verify(holdingsService, never()).getHoldingWithLock(anyString(), anyString());
    }
    
    @Test
    void testExecuteBuyOrder_NoIdempotencyKey_BackwardCompatibility() throws SQLException {
        String playerUuid = "test-player";
        String instrumentId = "AAPL";
        double qty = 10.0;
        
        // Mock current price
        when(database.queryValue(contains("last_price"), eq(instrumentId)))
            .thenReturn(100.0);
        
        // Mock wallet has sufficient balance
        when(walletService.hasBalance(eq(playerUuid), eq(1000.0)))
            .thenReturn(true);
        when(walletService.removeBalance(eq(playerUuid), eq(1000.0)))
            .thenReturn(true);
        
        // Mock transaction execution
        doAnswer(invocation -> {
            Db.TransactionBlock block = invocation.getArgument(0);
            Db.TransactionDb transactionDb = mock(Db.TransactionDb.class);
            when(transactionDb.execute(anyString(), any())).thenReturn(1);
            block.execute(transactionDb);
            return null;
        }).when(database).executeTransaction(any());
        
        // Mock holdings update
        when(holdingsService.addHoldingWithVersioning(any(), eq(playerUuid), eq(instrumentId), eq(qty), eq(100.0)))
            .thenReturn(true);
        
        // Test the legacy method (no idempotency key)
        TradingService.TradeResult result = tradingService.executeBuyOrder(playerUuid, instrumentId, qty);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("BUY 10.00 shares"));
        
        verify(database).executeTransaction(any());
        // Should not check for existing orders when no idempotency key is provided
        verify(database, never()).queryRow(contains("client_idempotency"), any());
    }
}