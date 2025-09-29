package com.example.quickstocks;

import com.example.quickstocks.core.algorithms.PriceThresholdController;
import com.example.quickstocks.core.models.Stock;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PriceThresholdController to ensure it correctly applies 
 * dampening based on configuration and trading activity.
 */
public class PriceThresholdControllerTest {
    
    private PriceThresholdController controller;
    private DatabaseConfig config;
    private Stock testStock;
    
    @BeforeEach
    public void setUp() {
        // Create test configuration with known values
        config = new DatabaseConfig();
        config.setPriceThresholdEnabled(true);
        config.setMaxChangePercent(0.15);           // 15% max change
        config.setPriceMultiplierThreshold(2.0);    // 200% of initial price
        config.setDampeningFactor(0.5);             // 50% dampening
        config.setMinVolumeThreshold(10);           // 10 shares minimum
        config.setVolumeSensitivity(0.5);           // Volume sensitivity
        
        controller = new PriceThresholdController(config);
        testStock = new Stock("TEST", "Test Stock", "Technology", 100.0);
    }
    
    @Test
    public void testNoThresholdWhenDisabled() {
        config.setPriceThresholdEnabled(false);
        controller = new PriceThresholdController(config);
        
        testStock.updatePrice(500.0); // 500% of initial price
        
        double dampening = controller.calculateDampeningFactor(testStock, 0.20); // 20% impact
        assertEquals(1.0, dampening, "Should not apply dampening when disabled");
    }
    
    @Test
    public void testNoThresholdBelowPriceMultiplier() {
        testStock.updatePrice(150.0); // 150% of initial price (below 200% threshold)
        
        double dampening = controller.calculateDampeningFactor(testStock, 0.20); // 20% impact
        assertEquals(1.0, dampening, "Should not apply dampening below price threshold");
    }
    
    @Test
    public void testNoThresholdWithLowImpact() {
        testStock.updatePrice(300.0); // 300% of initial price (above threshold)
        
        double dampening = controller.calculateDampeningFactor(testStock, 0.10); // 10% impact (below 15% max)
        assertEquals(1.0, dampening, "Should not apply dampening when impact is within limits");
    }
    
    @Test
    public void testFullDampeningWithLowVolume() {
        testStock.updatePrice(300.0); // 300% of initial price (above threshold)
        
        // No trading activity recorded
        double dampening = controller.calculateDampeningFactor(testStock, 0.20); // 20% impact (above 15% max)
        assertEquals(0.5, dampening, "Should apply full dampening with low volume");
    }
    
    @Test
    public void testReducedDampeningWithHighVolume() {
        testStock.updatePrice(300.0); // 300% of initial price (above threshold)
        
        // Record high trading activity
        controller.recordTradingActivity("TEST", 20); // Above minimum threshold of 10
        
        double dampening = controller.calculateDampeningFactor(testStock, 0.20); // 20% impact
        assertTrue(dampening > 0.5, "Should reduce dampening with high trading volume");
        assertTrue(dampening <= 1.0, "Dampening should not exceed 1.0");
    }
    
    @Test
    public void testTradingActivityReset() {
        controller.recordTradingActivity("TEST", 50);
        assertEquals(50, controller.getTradingActivity("TEST"));
        
        controller.resetTradingActivity();
        assertEquals(0, controller.getTradingActivity("TEST"));
    }
    
    @Test
    public void testPriceMultiplierCalculation() {
        double initialMultiplier = controller.getPriceMultiplier(testStock);
        assertEquals(1.0, initialMultiplier, 0.01, "Initial multiplier should be 1.0");
        
        testStock.updatePrice(250.0);
        double updatedMultiplier = controller.getPriceMultiplier(testStock);
        assertEquals(2.5, updatedMultiplier, 0.01, "Multiplier should be 2.5 for 250% price");
    }
    
    @Test
    public void testVolumeBasedDampeningGradient() {
        testStock.updatePrice(300.0); // Above threshold
        
        // Test with exactly minimum volume
        controller.recordTradingActivity("TEST", 10);
        double dampeningAtMin = controller.calculateDampeningFactor(testStock, 0.20);
        
        // Test with higher volume
        controller.recordTradingActivity("TEST", 30); // Total 40
        double dampeningAtHigh = controller.calculateDampeningFactor(testStock, 0.20);
        
        assertTrue(dampeningAtHigh > dampeningAtMin, 
            "Higher volume should result in less dampening");
    }
}