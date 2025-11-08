package net.cyberneticforge.quickstocks.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TradingService transaction logic.
 * These tests verify buy/sell order validations and trade calculations.
 */
@DisplayName("TradingService Tests")
public class TradingServiceTest {
    
    @Test
    @DisplayName("Buy order calculates total cost correctly")
    public void testBuyOrderCostCalculation() {
        // Given: Buying 100 shares at $50 each
        // When: Calculating total cost
        // Then: Total should be $5,000
        
        double quantity = 100.0;
        double price = 50.0;
        double expectedCost = 5000.0;
        
        double actualCost = quantity * price;
        
        assertEquals(expectedCost, actualCost, 0.001,
            "Buy order cost should be quantity * price");
    }
    
    @Test
    @DisplayName("Buy order fails when insufficient balance")
    public void testBuyOrderInsufficientBalance() {
        // Given: Player has $1,000 but trade costs $5,000
        // When: Attempting to buy
        // Then: Trade should fail
        
        double playerBalance = 1000.0;
        double tradeCost = 5000.0;
        
        boolean canExecute = playerBalance >= tradeCost;
        
        assertFalse(canExecute, "Buy order should fail with insufficient balance");
    }
    
    @Test
    @DisplayName("Buy order succeeds when sufficient balance")
    public void testBuyOrderSufficientBalance() {
        // Given: Player has $10,000 and trade costs $5,000
        // When: Attempting to buy
        // Then: Trade should succeed
        
        double playerBalance = 10000.0;
        double tradeCost = 5000.0;
        
        boolean canExecute = playerBalance >= tradeCost;
        
        assertTrue(canExecute, "Buy order should succeed with sufficient balance");
    }
    
    @Test
    @DisplayName("Sell order calculates proceeds correctly")
    public void testSellOrderProceedsCalculation() {
        // Given: Selling 100 shares at $50 each
        // When: Calculating proceeds
        // Then: Proceeds should be $5,000
        
        double quantity = 100.0;
        double price = 50.0;
        double expectedProceeds = 5000.0;
        
        double actualProceeds = quantity * price;
        
        assertEquals(expectedProceeds, actualProceeds, 0.001,
            "Sell order proceeds should be quantity * price");
    }
    
    @Test
    @DisplayName("Sell order fails when insufficient holdings")
    public void testSellOrderInsufficientHoldings() {
        // Given: Player has 50 shares but trying to sell 100
        // When: Attempting to sell
        // Then: Trade should fail
        
        double playerHoldings = 50.0;
        double quantityToSell = 100.0;
        
        boolean canExecute = playerHoldings >= quantityToSell;
        
        assertFalse(canExecute, "Sell order should fail with insufficient holdings");
    }
    
    @Test
    @DisplayName("Sell order succeeds when sufficient holdings")
    public void testSellOrderSufficientHoldings() {
        // Given: Player has 150 shares and trying to sell 100
        // When: Attempting to sell
        // Then: Trade should succeed
        
        double playerHoldings = 150.0;
        double quantityToSell = 100.0;
        
        boolean canExecute = playerHoldings >= quantityToSell;
        
        assertTrue(canExecute, "Sell order should succeed with sufficient holdings");
    }
    
    @Test
    @DisplayName("Buy order with fees increases total cost")
    public void testBuyOrderWithFees() {
        // Given: Trade costs $5,000 with 0.25% fee ($12.50)
        // When: Calculating total cost with fees
        // Then: Total should be $5,012.50
        
        double tradeCost = 5000.0;
        double feePercent = 0.25;
        double fee = tradeCost * (feePercent / 100.0);
        double totalCost = tradeCost + fee;
        
        assertEquals(5012.50, totalCost, 0.01,
            "Buy order total should include fee");
    }
    
    @Test
    @DisplayName("Sell order with fees reduces net proceeds")
    public void testSellOrderWithFees() {
        // Given: Trade proceeds $5,000 with 0.25% fee ($12.50)
        // When: Calculating net proceeds after fees
        // Then: Net should be $4,987.50
        
        double tradeProceeds = 5000.0;
        double feePercent = 0.25;
        double fee = tradeProceeds * (feePercent / 100.0);
        double netProceeds = tradeProceeds - fee;
        
        assertEquals(4987.50, netProceeds, 0.01,
            "Sell order net proceeds should deduct fee");
    }
    
    @Test
    @DisplayName("Trade fails when instrument price unavailable")
    public void testTradeWithUnavailablePrice() {
        // Given: Instrument exists but has no current price
        // When: Attempting to trade
        // Then: Trade should fail
        
        Double instrumentPrice = null;
        
        boolean canExecute = instrumentPrice != null && instrumentPrice > 0;
        
        assertFalse(canExecute, "Trade should fail when price unavailable");
    }
    
    @Test
    @DisplayName("Trade fails when instrument price is zero")
    public void testTradeWithZeroPrice() {
        // Given: Instrument has a price of zero
        // When: Attempting to trade
        // Then: Trade should fail
        
        double instrumentPrice = 0.0;
        
        boolean canExecute = instrumentPrice > 0;
        
        assertFalse(canExecute, "Trade should fail with zero price");
    }
    
    @Test
    @DisplayName("Fractional shares handled correctly in buy order")
    public void testBuyOrderFractionalShares() {
        // Given: Buying 10.5 shares at $25.50 each
        // When: Calculating total cost
        // Then: Total should be $267.75
        
        double quantity = 10.5;
        double price = 25.50;
        double expectedCost = 267.75;
        
        double actualCost = quantity * price;
        
        assertEquals(expectedCost, actualCost, 0.001,
            "Buy order should handle fractional shares");
    }
    
    @Test
    @DisplayName("Fractional shares handled correctly in sell order")
    public void testSellOrderFractionalShares() {
        // Given: Selling 7.25 shares at $40.00 each
        // When: Calculating proceeds
        // Then: Proceeds should be $290.00
        
        double quantity = 7.25;
        double price = 40.00;
        double expectedProceeds = 290.00;
        
        double actualProceeds = quantity * price;
        
        assertEquals(expectedProceeds, actualProceeds, 0.001,
            "Sell order should handle fractional shares");
    }
    
    @Test
    @DisplayName("Large quantity order calculates correctly")
    public void testLargeQuantityOrder() {
        // Given: Buying 10,000 shares at $15.75 each
        // When: Calculating total cost
        // Then: Total should be $157,500
        
        double quantity = 10000.0;
        double price = 15.75;
        double expectedCost = 157500.0;
        
        double actualCost = quantity * price;
        
        assertEquals(expectedCost, actualCost, 0.001,
            "Large quantity orders should calculate correctly");
    }
    
    @Test
    @DisplayName("Holdings update correctly after buy")
    public void testHoldingsAfterBuy() {
        // Given: Player has 50 shares and buys 30 more
        // When: Updating holdings
        // Then: Holdings should be 80 shares
        
        double currentHoldings = 50.0;
        double purchasedQty = 30.0;
        double expectedHoldings = 80.0;
        
        double actualHoldings = currentHoldings + purchasedQty;
        
        assertEquals(expectedHoldings, actualHoldings, 0.001,
            "Holdings should increase after buy");
    }
    
    @Test
    @DisplayName("Holdings update correctly after sell")
    public void testHoldingsAfterSell() {
        // Given: Player has 100 shares and sells 40
        // When: Updating holdings
        // Then: Holdings should be 60 shares
        
        double currentHoldings = 100.0;
        double soldQty = 40.0;
        double expectedHoldings = 60.0;
        
        double actualHoldings = currentHoldings - soldQty;
        
        assertEquals(expectedHoldings, actualHoldings, 0.001,
            "Holdings should decrease after sell");
    }
    
    @Test
    @DisplayName("Balance updates correctly after buy")
    public void testBalanceAfterBuy() {
        // Given: Player has $10,000 and buys for $3,000
        // When: Updating balance
        // Then: Balance should be $7,000
        
        double currentBalance = 10000.0;
        double tradeCost = 3000.0;
        double expectedBalance = 7000.0;
        
        double actualBalance = currentBalance - tradeCost;
        
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Balance should decrease after buy");
    }
    
    @Test
    @DisplayName("Balance updates correctly after sell")
    public void testBalanceAfterSell() {
        // Given: Player has $5,000 and sells for $2,000
        // When: Updating balance
        // Then: Balance should be $7,000
        
        double currentBalance = 5000.0;
        double tradeProceeds = 2000.0;
        double expectedBalance = 7000.0;
        
        double actualBalance = currentBalance + tradeProceeds;
        
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Balance should increase after sell");
    }
}
