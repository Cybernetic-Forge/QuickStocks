package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditServiceTest {
    
    @Mock
    private Db database;
    
    @Mock
    private HoldingsService holdingsService;
    
    private AuditService auditService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditService = new AuditService(database, holdingsService);
    }
    
    @Test
    void testAuditPlayerHoldings_NoDiscrepancies() throws SQLException {
        String playerUuid = "test-player-uuid";
        
        // Mock holdings data
        List<Map<String, Object>> holdingsResults = Arrays.asList(
            createHoldingRow(playerUuid, "AAPL")
        );
        when(database.query(contains("user_holdings"), eq(playerUuid))).thenReturn(holdingsResults);
        
        // Mock order data
        List<Map<String, Object>> orderResults = Arrays.asList(
            createOrderRow("AAPL", "BUY", 10.0)
        );
        when(database.query(contains("orders"), eq(playerUuid))).thenReturn(orderResults);
        
        // Mock current holdings
        Map<String, Object> currentHolding = Map.of(
            "instrument_id", "AAPL",
            "qty", 10.0
        );
        when(database.query(contains("user_holdings WHERE player_uuid"), eq(playerUuid)))
            .thenReturn(Arrays.asList(currentHolding));
        
        AuditService.AuditPlayerResult result = auditService.auditPlayerHoldings(playerUuid, false);
        
        assertEquals(playerUuid, result.playerUuid);
        assertTrue(result.discrepancies.isEmpty());
        assertEquals(0, result.repairsApplied);
    }
    
    @Test
    void testAuditPlayerHoldings_WithDiscrepancy() throws SQLException {
        String playerUuid = "test-player-uuid";
        
        // Mock order data - player bought 10 shares
        List<Map<String, Object>> orderResults = Arrays.asList(
            createOrderRow("AAPL", "BUY", 10.0)
        );
        when(database.query(contains("orders"), eq(playerUuid))).thenReturn(orderResults);
        
        // Mock current holdings - player has 5 shares (discrepancy)
        Map<String, Object> currentHolding = Map.of(
            "instrument_id", "AAPL",
            "qty", 5.0
        );
        when(database.query(contains("user_holdings WHERE player_uuid"), eq(playerUuid)))
            .thenReturn(Arrays.asList(currentHolding));
        
        AuditService.AuditPlayerResult result = auditService.auditPlayerHoldings(playerUuid, false);
        
        assertEquals(playerUuid, result.playerUuid);
        assertEquals(1, result.discrepancies.size());
        
        AuditService.Discrepancy discrepancy = result.discrepancies.get(0);
        assertEquals("AAPL", discrepancy.instrumentId);
        assertEquals(10.0, discrepancy.expectedQty, 0.001);
        assertEquals(5.0, discrepancy.actualQty, 0.001);
        assertEquals(-5.0, discrepancy.difference, 0.001);
    }
    
    @Test
    void testCalculateExpectedHoldings_BuyAndSell() throws SQLException {
        String playerUuid = "test-player-uuid";
        
        // Mock order history: Buy 10, Sell 3, Buy 5 = 12 expected
        List<Map<String, Object>> orderResults = Arrays.asList(
            createOrderRow("AAPL", "BUY", 10.0),
            createOrderRow("AAPL", "SELL", 3.0),
            createOrderRow("AAPL", "BUY", 5.0)
        );
        when(database.query(contains("orders"), eq(playerUuid))).thenReturn(orderResults);
        
        // Mock current holdings - empty for this test
        when(database.query(contains("user_holdings WHERE player_uuid"), eq(playerUuid)))
            .thenReturn(Collections.emptyList());
        
        AuditService.AuditPlayerResult result = auditService.auditPlayerHoldings(playerUuid, false);
        
        // Should expect 12 shares but have 0 (discrepancy of -12)
        assertEquals(1, result.discrepancies.size());
        assertEquals(12.0, result.discrepancies.get(0).expectedQty, 0.001);
        assertEquals(0.0, result.discrepancies.get(0).actualQty, 0.001);
    }
    
    @Test
    void testAuditAllHoldings_MultiplePlayersWithIssues() throws SQLException {
        // Mock players with holdings
        when(database.query(contains("DISTINCT player_uuid FROM user_holdings")))
            .thenReturn(Arrays.asList(
                Map.of("player_uuid", "player1"),
                Map.of("player_uuid", "player2")
            ));
        
        // Mock players with orders  
        when(database.query(contains("DISTINCT player_uuid FROM orders")))
            .thenReturn(Arrays.asList(
                Map.of("player_uuid", "player1"),
                Map.of("player_uuid", "player2"),
                Map.of("player_uuid", "player3")
            ));
        
        // Mock orders for each player
        when(database.query(contains("orders"), eq("player1")))
            .thenReturn(Arrays.asList(createOrderRow("AAPL", "BUY", 10.0)));
        when(database.query(contains("orders"), eq("player2")))
            .thenReturn(Arrays.asList(createOrderRow("GOOGL", "BUY", 5.0)));
        when(database.query(contains("orders"), eq("player3")))
            .thenReturn(Arrays.asList(createOrderRow("MSFT", "BUY", 15.0)));
        
        // Mock current holdings (all empty to create discrepancies)
        when(database.query(contains("user_holdings WHERE player_uuid"), anyString()))
            .thenReturn(Collections.emptyList());
        
        // Mock audit log insertion
        when(database.execute(contains("INSERT INTO audit_log"), any(), any(), any(), any(), any()))
            .thenReturn(1);
        
        AuditService.AuditResult result = auditService.auditAllHoldings(false);
        
        assertEquals(3, result.totalPlayersChecked);
        assertEquals(3, result.playersWithIssues);
        assertEquals(3, result.totalDiscrepancies);
        assertEquals(0, result.totalRepairs);
    }
    
    private Map<String, Object> createHoldingRow(String playerUuid, String instrumentId) {
        Map<String, Object> row = new HashMap<>();
        row.put("player_uuid", playerUuid);
        row.put("instrument_id", instrumentId);
        row.put("qty", 10.0);
        row.put("avg_cost", 100.0);
        return row;
    }
    
    private Map<String, Object> createOrderRow(String instrumentId, String side, double qty) {
        Map<String, Object> row = new HashMap<>();
        row.put("instrument_id", instrumentId);
        row.put("side", side);
        row.put("qty", qty);
        return row;
    }
}