package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {

    @Mock
    private Db database;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        analyticsService = new AnalyticsService(database, 0.94, 1440, 1440, 1440);
    }

    @Test
    void testGetChangePct_WithValidData() throws Exception {
        // Arrange
        String instrumentId = "test-instrument";
        int windowMinutes = 60;
        
        // Mock first query (old price)
        List<Map<String, Object>> oldPriceResults = new ArrayList<>();
        Map<String, Object> oldPriceRow = new HashMap<>();
        oldPriceRow.put("price", 100.0);
        oldPriceRow.put("ts", System.currentTimeMillis() - 60 * 60 * 1000L);
        oldPriceResults.add(oldPriceRow);
        
        // Mock second query (current price)
        List<Map<String, Object>> currentPriceResults = new ArrayList<>();
        Map<String, Object> currentPriceRow = new HashMap<>();
        currentPriceRow.put("price", 110.0);
        currentPriceResults.add(currentPriceRow);

        when(database.query(contains("ts >= ?"), eq(instrumentId), anyLong()))
            .thenReturn(oldPriceResults);
        when(database.query(contains("ORDER BY ts DESC"), eq(instrumentId)))
            .thenReturn(currentPriceResults);

        // Act
        double changePct = analyticsService.getChangePct(instrumentId, windowMinutes);

        // Assert
        assertEquals(0.1, changePct, 0.001); // 10% increase
    }

    @Test
    void testGetChangePct_WithNoData() throws Exception {
        // Arrange
        String instrumentId = "test-instrument";
        int windowMinutes = 60;
        
        when(database.query(any(String.class), any(), any()))
            .thenReturn(new ArrayList<>());

        // Act
        double changePct = analyticsService.getChangePct(instrumentId, windowMinutes);

        // Assert
        assertEquals(0.0, changePct);
    }

    @Test
    void testGetVolatilityEWMA_WithStablePrices() throws Exception {
        // Arrange - stable prices should give low volatility
        String instrumentId = "stable-instrument";
        int windowMinutes = 60;
        
        List<Map<String, Object>> priceHistory = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - 60 * 60 * 1000L;
        
        // Add stable price history (small variations)
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("price", 100.0 + Math.random() * 0.1); // Very small variations
            row.put("ts", baseTime + i * 60000);
            priceHistory.add(row);
        }

        when(database.query(contains("ORDER BY ts ASC"), eq(instrumentId), anyLong()))
            .thenReturn(priceHistory);

        // Act
        double volatility = analyticsService.getVolatilityEWMA(instrumentId, windowMinutes, 0.94);

        // Assert
        assertTrue(volatility < 0.01, "Stable prices should have very low volatility, got: " + volatility);
    }

    @Test
    void testGetVolatilityEWMA_WithVolatilePrices() throws Exception {
        // Arrange - volatile prices should give high volatility
        String instrumentId = "volatile-instrument";
        int windowMinutes = 60;
        
        List<Map<String, Object>> priceHistory = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - 60 * 60 * 1000L;
        
        // Add volatile price history (large swings)
        double[] prices = {100.0, 120.0, 80.0, 150.0, 60.0, 130.0, 90.0, 110.0};
        for (int i = 0; i < prices.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("price", prices[i]);
            row.put("ts", baseTime + i * 60000);
            priceHistory.add(row);
        }

        when(database.query(contains("ORDER BY ts ASC"), eq(instrumentId), anyLong()))
            .thenReturn(priceHistory);

        // Act
        double volatility = analyticsService.getVolatilityEWMA(instrumentId, windowMinutes, 0.94);

        // Assert
        assertTrue(volatility > 0.1, "Volatile prices should have high volatility, got: " + volatility);
    }

    @Test
    void testGetVolatilityEWMA_CompareStableVsVolatile() throws Exception {
        // This test verifies that EWMA volatility decreases when price is stable 
        // and rises after spikes (acceptance criteria)
        
        String stableInstrument = "stable";
        String volatileInstrument = "volatile";
        int windowMinutes = 60;
        
        // Setup stable price history
        List<Map<String, Object>> stablePrices = createPriceHistory(
            new double[]{100.0, 100.1, 99.9, 100.05, 99.95, 100.02, 99.98},
            60 * 60 * 1000L
        );
        
        // Setup volatile price history with spike
        List<Map<String, Object>> volatilePrices = createPriceHistory(
            new double[]{100.0, 100.1, 99.9, 130.0, 95.0, 125.0, 85.0}, // Big spike then drop
            60 * 60 * 1000L
        );

        when(database.query(contains("ORDER BY ts ASC"), eq(stableInstrument), anyLong()))
            .thenReturn(stablePrices);
        when(database.query(contains("ORDER BY ts ASC"), eq(volatileInstrument), anyLong()))
            .thenReturn(volatilePrices);

        // Act
        double stableVolatility = analyticsService.getVolatilityEWMA(stableInstrument, windowMinutes, 0.94);
        double volatileVolatility = analyticsService.getVolatilityEWMA(volatileInstrument, windowMinutes, 0.94);

        // Assert
        assertTrue(stableVolatility < volatileVolatility, 
            String.format("Stable volatility (%.6f) should be less than volatile volatility (%.6f)", 
                stableVolatility, volatileVolatility));
    }

    @Test
    void testGetCorrelation_PositiveCorrelation() throws Exception {
        // Arrange - two instruments moving in same direction
        String instrumentA = "A";
        String instrumentB = "B";
        int windowMinutes = 120;
        
        List<Map<String, Object>> pricesA = createPriceHistory(
            new double[]{100.0, 105.0, 110.0, 108.0, 112.0},
            120 * 60 * 1000L
        );
        
        List<Map<String, Object>> pricesB = createPriceHistory(
            new double[]{200.0, 210.0, 220.0, 216.0, 224.0}, // Similar pattern, different scale
            120 * 60 * 1000L
        );

        when(database.query(contains("ORDER BY ts ASC"), eq(instrumentA), anyLong()))
            .thenReturn(pricesA);
        when(database.query(contains("ORDER BY ts ASC"), eq(instrumentB), anyLong()))
            .thenReturn(pricesB);

        // Act
        double correlation = analyticsService.getCorrelation(instrumentA, instrumentB, windowMinutes);

        // Assert
        assertTrue(correlation > 0.5, "Positively correlated instruments should have correlation > 0.5, got: " + correlation);
    }

    @Test
    void testGetSharpe_WithPortfolioData() throws Exception {
        // Arrange - mock portfolio data with decent returns and volatility
        String playerUuid = "test-player";
        int windowDays = 30;
        double riskFree = 0.02; // 2% annual risk-free rate
        
        List<Map<String, Object>> sharpeData = new ArrayList<>();
        Map<String, Object> sharpeRow = new HashMap<>();
        sharpeRow.put("avg_return", 0.001); // 0.1% daily return
        sharpeRow.put("return_std_dev", 0.01); // 1% daily volatility
        sharpeRow.put("return_count", 10);
        sharpeRow.put("total_return", 0.10); // 10% total return
        sharpeData.add(sharpeRow);

        when(database.query(contains("sharpe_ratio_data"), eq(playerUuid)))
            .thenReturn(sharpeData);

        // Act
        double sharpe = analyticsService.getSharpe(playerUuid, windowDays, riskFree);

        // Assert
        assertTrue(sharpe > 0, "Player with positive excess returns should have positive Sharpe ratio, got: " + sharpe);
        // Expected: (0.001 - 0.02/365) / 0.01 ≈ (0.001 - 0.0000548) / 0.01 ≈ 0.094
        assertTrue(sharpe > 0.09 && sharpe < 0.1, "Sharpe ratio should be around 0.094, got: " + sharpe);
    }

    @Test
    void testGetSharpe_WithNoData() throws Exception {
        // Arrange
        String playerUuid = "no-data-player";
        int windowDays = 30;
        double riskFree = 0.02;
        
        when(database.query(contains("sharpe_ratio_data"), eq(playerUuid)))
            .thenReturn(new ArrayList<>());

        // Act
        double sharpe = analyticsService.getSharpe(playerUuid, windowDays, riskFree);

        // Assert
        assertEquals(0.0, sharpe, "Player with no data should have Sharpe ratio of 0.0");
    }

    @Test
    void testRecordPortfolioValue() throws Exception {
        // Arrange
        String playerUuid = "test-player";
        double totalValue = 1000.0;
        double cashBalance = 200.0;
        double holdingsValue = 800.0;

        // Act
        analyticsService.recordPortfolioValue(playerUuid, totalValue, cashBalance, holdingsValue);

        // Assert
        verify(database).execute(
            contains("INSERT INTO portfolio_history"),
            any(String.class), // id
            eq(playerUuid),
            any(Long.class), // ts
            eq(totalValue),
            eq(cashBalance),
            eq(holdingsValue),
            any(Long.class) // created_at
        );
    }

    @Test
    void testGetSharpeLeaderboard() throws Exception {
        // Arrange
        int limit = 5;
        double riskFreeRate = 0.02;
        
        List<Map<String, Object>> leaderboardData = new ArrayList<>();
        
        // Add some mock leaderboard data
        Map<String, Object> player1 = new HashMap<>();
        player1.put("player_uuid", "player1");
        player1.put("avg_return", 0.002);
        player1.put("return_std_dev", 0.01);
        player1.put("sharpe_ratio", 0.194);
        leaderboardData.add(player1);
        
        Map<String, Object> player2 = new HashMap<>();
        player2.put("player_uuid", "player2");
        player2.put("avg_return", 0.001);
        player2.put("return_std_dev", 0.01);
        player2.put("sharpe_ratio", 0.094);
        leaderboardData.add(player2);

        when(database.query(contains("sharpe_ratio_data"), anyDouble(), eq(limit)))
            .thenReturn(leaderboardData);

        // Act
        List<Map<String, Object>> leaderboard = analyticsService.getSharpeLeaderboard(limit, riskFreeRate);

        // Assert
        assertEquals(2, leaderboard.size());
        assertEquals("player1", leaderboard.get(0).get("player_uuid"));
        assertTrue(((Number) leaderboard.get(0).get("sharpe_ratio")).doubleValue() > 
                  ((Number) leaderboard.get(1).get("sharpe_ratio")).doubleValue());
    }

    @Test
    void testDefaultValues() {
        // Assert
        assertEquals(0.94, analyticsService.getDefaultLambda(), 0.001);
        assertEquals(1440, analyticsService.getDefaultChangeWindow());
        assertEquals(1440, analyticsService.getDefaultVolatilityWindow());
        assertEquals(1440, analyticsService.getDefaultCorrelationWindow());
    }

    private List<Map<String, Object>> createPriceHistory(double[] prices, long windowMs) {
        List<Map<String, Object>> history = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - windowMs;
        
        for (int i = 0; i < prices.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("price", prices[i]);
            row.put("ts", baseTime + (i * windowMs / prices.length));
            history.add(row);
        }
        
        return history;
    }
}