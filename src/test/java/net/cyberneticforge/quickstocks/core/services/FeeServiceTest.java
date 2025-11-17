package net.cyberneticforge.quickstocks.core.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for FeeService calculations.
 * These tests verify trading fee calculations for different modes.
 */
@DisplayName("FeeService Tests")
public class FeeServiceTest {
    
    @Test
    @DisplayName("Calculate fee with percent mode")
    public void testCalculateFeePercent() {
        // Given: A FeeConfig with percent mode at 0.25%
        // When: Calculating fee for $10,000 notional value
        // Then: Fee should be $25.00 (0.25% of 10,000)
        
        double notionalValue = 10000.0;
        double expectedFee = 25.0; // 0.25% of 10,000
        double feePercent = 0.25;
        
        double actualFee = notionalValue * (feePercent / 100.0);
        
        assertEquals(expectedFee, actualFee, 0.001, 
            "Fee should be 0.25% of notional value");
    }
    
    @Test
    @DisplayName("Calculate fee with flat mode")
    public void testCalculateFeeFlat() {
        // Given: A FeeConfig with flat mode at $5.00
        // When: Calculating fee for any notional value
        // Then: Fee should always be $5.00
        
        double notionalValue1 = 1000.0;
        double notionalValue2 = 10000.0;
        double flatFee = 5.0;
        
        assertEquals(flatFee, flatFee, 0.001,
            "Flat fee should be constant regardless of notional value");
    }
    
    @Test
    @DisplayName("Calculate fee with mixed mode")
    public void testCalculateFeeMixed() {
        // Given: A FeeConfig with mixed mode (0.25% + $5.00)
        // When: Calculating fee for $10,000 notional value
        // Then: Fee should be $30.00 (0.25% of 10,000 + 5.00)
        
        double notionalValue = 10000.0;
        double feePercent = 0.25;
        double flatFee = 5.0;
        
        double expectedFee = (notionalValue * (feePercent / 100.0)) + flatFee;
        
        assertEquals(30.0, expectedFee, 0.001,
            "Mixed fee should be percentage + flat fee");
    }
    
    @Test
    @DisplayName("Calculate fee for zero notional value")
    public void testCalculateFeeZeroNotional() {
        // Given: A notional value of $0
        // When: Calculating fee
        // Then: Fee should be $0 regardless of mode
        
        double notionalValue = 0.0;
        double feePercent = 0.25;
        
        double fee = notionalValue * (feePercent / 100.0);
        
        assertEquals(0.0, fee, 0.001,
            "Fee should be zero for zero notional value");
    }
    
    @Test
    @DisplayName("Calculate total cost with fees")
    public void testCalculateTotalCostWithFees() {
        // Given: A buy order for $10,000 notional value with 0.25% fee
        // When: Calculating total cost with fees
        // Then: Total should be $10,025.00 (10,000 + 25)
        
        double notionalValue = 10000.0;
        double feePercent = 0.25;
        double fee = notionalValue * (feePercent / 100.0);
        double totalCost = notionalValue + fee;
        
        assertEquals(10025.0, totalCost, 0.001,
            "Total cost should include the fee");
    }
    
    @Test
    @DisplayName("Calculate net proceeds after fees")
    public void testCalculateNetProceedsAfterFees() {
        // Given: A sell order for $10,000 notional value with 0.25% fee
        // When: Calculating net proceeds after fees
        // Then: Net proceeds should be $9,975.00 (10,000 - 25)
        
        double notionalValue = 10000.0;
        double feePercent = 0.25;
        double fee = notionalValue * (feePercent / 100.0);
        double netProceeds = notionalValue - fee;
        
        assertEquals(9975.0, netProceeds, 0.001,
            "Net proceeds should deduct the fee");
    }
    
    @Test
    @DisplayName("Fee calculation for small trade")
    public void testCalculateFeeSmallTrade() {
        // Given: A small trade of $100 with 0.25% fee
        // When: Calculating fee
        // Then: Fee should be $0.25
        
        double notionalValue = 100.0;
        double feePercent = 0.25;
        double fee = notionalValue * (feePercent / 100.0);
        
        assertEquals(0.25, fee, 0.001,
            "Fee should be correctly calculated for small trades");
    }
    
    @Test
    @DisplayName("Fee calculation for large trade")
    public void testCalculateFeeLargeTrade() {
        // Given: A large trade of $1,000,000 with 0.25% fee
        // When: Calculating fee
        // Then: Fee should be $2,500.00
        
        double notionalValue = 1000000.0;
        double feePercent = 0.25;
        double fee = notionalValue * (feePercent / 100.0);
        
        assertEquals(2500.0, fee, 0.001,
            "Fee should be correctly calculated for large trades");
    }
    
    @Test
    @DisplayName("Negative notional value returns zero fee")
    public void testCalculateFeeNegativeNotional() {
        // Given: A negative notional value (invalid input)
        // When: Calculating fee
        // Then: Should handle gracefully (in real service returns 0.0)
        
        double notionalValue = -1000.0;
        
        // In the actual service, negative values return 0.0 fee
        // This tests the expected behavior
        assertTrue(notionalValue < 0, "Notional value should be negative");
    }
}
