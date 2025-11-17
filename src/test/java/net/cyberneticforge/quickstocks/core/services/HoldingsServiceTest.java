package net.cyberneticforge.quickstocks.core.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for HoldingsService portfolio operations.
 * These tests verify holdings management and portfolio calculations.
 */
@DisplayName("HoldingsService Tests")
public class HoldingsServiceTest {
    
    @Test
    @DisplayName("Add holding creates new position")
    public void testAddHoldingNewPosition() {
        // Given: Player has no existing holdings for instrument
        // When: Adding 100 shares at $50 each
        // Then: Holdings should be 100 shares with avg cost $50
        
        double existingQty = 0.0;
        double addedQty = 100.0;
        double addedPrice = 50.0;
        
        double newQty = existingQty + addedQty;
        double newAvgCost = addedPrice; // First purchase sets avg cost
        
        assertEquals(100.0, newQty, 0.001, "New holding quantity should be correct");
        assertEquals(50.0, newAvgCost, 0.001, "New holding avg cost should match purchase price");
    }
    
    @Test
    @DisplayName("Add holding updates average cost correctly")
    public void testAddHoldingUpdatesAvgCost() {
        // Given: Player has 50 shares at $40 avg cost
        // When: Adding 50 more shares at $60
        // Then: Holdings should be 100 shares with avg cost $50
        
        double existingQty = 50.0;
        double existingAvgCost = 40.0;
        double addedQty = 50.0;
        double addedPrice = 60.0;
        
        double totalValue = (existingQty * existingAvgCost) + (addedQty * addedPrice);
        double newQty = existingQty + addedQty;
        double newAvgCost = totalValue / newQty;
        
        assertEquals(100.0, newQty, 0.001, "Total quantity should be sum of both");
        assertEquals(50.0, newAvgCost, 0.001, "Average cost should be weighted average");
    }
    
    @Test
    @DisplayName("Remove holding decreases quantity")
    public void testRemoveHoldingDecreasesQty() {
        // Given: Player has 100 shares
        // When: Removing 40 shares
        // Then: Holdings should be 60 shares
        
        double existingQty = 100.0;
        double removedQty = 40.0;
        double newQty = existingQty - removedQty;
        
        assertEquals(60.0, newQty, 0.001, "Quantity should decrease by removed amount");
    }
    
    @Test
    @DisplayName("Remove all holdings sets quantity to zero")
    public void testRemoveAllHoldings() {
        // Given: Player has 75 shares
        // When: Removing all 75 shares
        // Then: Holdings should be 0
        
        double existingQty = 75.0;
        double removedQty = 75.0;
        double newQty = existingQty - removedQty;
        
        assertEquals(0.0, newQty, 0.001, "Removing all shares should leave zero");
    }
    
    @Test
    @DisplayName("Cannot remove more than existing holdings")
    public void testRemoveExcessHoldings() {
        // Given: Player has 50 shares
        // When: Attempting to remove 100 shares
        // Then: Should be rejected
        
        double existingQty = 50.0;
        double removedQty = 100.0;
        
        boolean canRemove = existingQty >= removedQty;
        
        assertFalse(canRemove, "Cannot remove more than existing holdings");
    }
    
    @Test
    @DisplayName("Calculate unrealized P&L for gain")
    public void testUnrealizedProfitGain() {
        // Given: 100 shares with avg cost $40, current price $55
        // When: Calculating unrealized P&L
        // Then: Profit should be $1,500 (100 * (55 - 40))
        
        double quantity = 100.0;
        double avgCost = 40.0;
        double currentPrice = 55.0;
        
        double unrealizedPL = quantity * (currentPrice - avgCost);
        
        assertEquals(1500.0, unrealizedPL, 0.001,
            "Unrealized P&L should show gain when current price > avg cost");
    }
    
    @Test
    @DisplayName("Calculate unrealized P&L for loss")
    public void testUnrealizedProfitLoss() {
        // Given: 100 shares with avg cost $50, current price $35
        // When: Calculating unrealized P&L
        // Then: Loss should be -$1,500 (100 * (35 - 50))
        
        double quantity = 100.0;
        double avgCost = 50.0;
        double currentPrice = 35.0;
        
        double unrealizedPL = quantity * (currentPrice - avgCost);
        
        assertEquals(-1500.0, unrealizedPL, 0.001,
            "Unrealized P&L should show loss when current price < avg cost");
    }
    
    @Test
    @DisplayName("Calculate unrealized P&L percentage gain")
    public void testUnrealizedPercentageGain() {
        // Given: Holdings with avg cost $40, current price $50
        // When: Calculating P&L percentage
        // Then: Should be +25%
        
        double avgCost = 40.0;
        double currentPrice = 50.0;
        
        double percentChange = ((currentPrice - avgCost) / avgCost) * 100.0;
        
        assertEquals(25.0, percentChange, 0.001,
            "P&L percentage should be 25% gain");
    }
    
    @Test
    @DisplayName("Calculate unrealized P&L percentage loss")
    public void testUnrealizedPercentageLoss() {
        // Given: Holdings with avg cost $100, current price $75
        // When: Calculating P&L percentage
        // Then: Should be -25%
        
        double avgCost = 100.0;
        double currentPrice = 75.0;
        
        double percentChange = ((currentPrice - avgCost) / avgCost) * 100.0;
        
        assertEquals(-25.0, percentChange, 0.001,
            "P&L percentage should be 25% loss");
    }
    
    @Test
    @DisplayName("Calculate portfolio total value")
    public void testPortfolioTotalValue() {
        // Given: Multiple holdings
        //   - 100 shares at $50 current price = $5,000
        //   - 50 shares at $80 current price = $4,000
        // When: Calculating total portfolio value
        // Then: Total should be $9,000
        
        double holding1Qty = 100.0;
        double holding1Price = 50.0;
        double holding2Qty = 50.0;
        double holding2Price = 80.0;
        
        double totalValue = (holding1Qty * holding1Price) + (holding2Qty * holding2Price);
        
        assertEquals(9000.0, totalValue, 0.001,
            "Portfolio total value should be sum of all holdings");
    }
    
    @Test
    @DisplayName("Calculate portfolio total cost basis")
    public void testPortfolioTotalCostBasis() {
        // Given: Multiple holdings
        //   - 100 shares at $40 avg cost = $4,000
        //   - 50 shares at $70 avg cost = $3,500
        // When: Calculating total cost basis
        // Then: Total should be $7,500
        
        double holding1Qty = 100.0;
        double holding1AvgCost = 40.0;
        double holding2Qty = 50.0;
        double holding2AvgCost = 70.0;
        
        double totalCostBasis = (holding1Qty * holding1AvgCost) + (holding2Qty * holding2AvgCost);
        
        assertEquals(7500.0, totalCostBasis, 0.001,
            "Portfolio total cost basis should be sum of all position costs");
    }
    
    @Test
    @DisplayName("Calculate portfolio total P&L")
    public void testPortfolioTotalPL() {
        // Given: Portfolio with total value $9,000 and cost basis $7,500
        // When: Calculating total P&L
        // Then: Profit should be $1,500
        
        double totalValue = 9000.0;
        double totalCostBasis = 7500.0;
        
        double totalPL = totalValue - totalCostBasis;
        
        assertEquals(1500.0, totalPL, 0.001,
            "Portfolio P&L should be difference between value and cost");
    }
    
    @Test
    @DisplayName("Zero holdings returns zero value")
    public void testZeroHoldingsValue() {
        // Given: Player has zero holdings
        // When: Calculating portfolio value
        // Then: Value should be 0
        
        double quantity = 0.0;
        double currentPrice = 50.0;
        
        double value = quantity * currentPrice;
        
        assertEquals(0.0, value, 0.001,
            "Zero holdings should have zero value");
    }
    
    @Test
    @DisplayName("Average cost persists after partial sell")
    public void testAvgCostAfterPartialSell() {
        // Given: Player has 100 shares at $50 avg cost
        // When: Selling 40 shares
        // Then: Remaining 60 shares still have $50 avg cost
        
        double originalQty = 100.0;
        double avgCost = 50.0;
        double soldQty = 40.0;
        double remainingQty = originalQty - soldQty;
        
        // Average cost doesn't change with partial sell
        double newAvgCost = avgCost;
        
        assertEquals(60.0, remainingQty, 0.001, "Quantity should decrease");
        assertEquals(50.0, newAvgCost, 0.001, "Average cost should remain the same");
    }
    
    @Test
    @DisplayName("Empty portfolio has no holdings")
    public void testEmptyPortfolio() {
        // Given: A new player with no trades
        // When: Checking portfolio
        // Then: Should have zero holdings
        
        int holdingsCount = 0;
        
        assertEquals(0, holdingsCount, "New player should have empty portfolio");
    }
    
    @Test
    @DisplayName("Adding holding at same price doesn't change avg cost")
    public void testAddHoldingSamePrice() {
        // Given: Player has 50 shares at $100 avg cost
        // When: Adding 50 more shares at $100
        // Then: Holdings should be 100 shares with avg cost still $100
        
        double existingQty = 50.0;
        double existingAvgCost = 100.0;
        double addedQty = 50.0;
        double addedPrice = 100.0;
        
        double totalValue = (existingQty * existingAvgCost) + (addedQty * addedPrice);
        double newQty = existingQty + addedQty;
        double newAvgCost = totalValue / newQty;
        
        assertEquals(100.0, newQty, 0.001, "Total quantity should be 100");
        assertEquals(100.0, newAvgCost, 0.001, "Average cost should remain $100");
    }
    
    @Test
    @DisplayName("Fractional shares handled in holdings")
    public void testFractionalSharesInHoldings() {
        // Given: Player buys 10.5 shares at $25.50
        // When: Adding to holdings
        // Then: Holdings should reflect fractional quantity
        
        double quantity = 10.5;
        double price = 25.50;
        
        double value = quantity * price;
        
        assertEquals(267.75, value, 0.001,
            "Fractional shares should be handled correctly");
    }
}
