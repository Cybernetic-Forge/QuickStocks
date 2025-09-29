package com.example.quickstocks.core.services;

import com.example.quickstocks.core.config.TradingConfig;
import com.example.quickstocks.core.model.OrderRequest;
import com.example.quickstocks.core.model.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the trading economy features including fees, slippage, and validation.
 */
public class TradingEconomyTest {

    private TradingConfig tradingConfig;
    private FeeService feeService;
    private SlippageService slippageService;

    @BeforeEach
    void setUp() {
        tradingConfig = new TradingConfig();
        feeService = new FeeService(tradingConfig.getFee());
        slippageService = new SlippageService(tradingConfig.getSlippage());
    }

    @Test
    void testFeeCalculationPercent() {
        // Default config is 0.25% fee
        double notional = 10000.0; // $10,000 trade
        double expectedFee = 10000.0 * 0.0025; // $25

        double actualFee = feeService.calculateFee(notional);
        assertEquals(expectedFee, actualFee, 0.01, "Fee calculation should be accurate");
    }

    @Test
    void testFeeCalculationFlat() {
        TradingConfig.FeeConfig feeConfig = tradingConfig.getFee();
        feeConfig.setMode("flat");
        feeConfig.setFlat(5.0);
        
        FeeService flatFeeService = new FeeService(feeConfig);
        
        double fee1 = flatFeeService.calculateFee(1000.0);
        double fee2 = flatFeeService.calculateFee(100000.0);
        
        assertEquals(5.0, fee1, "Flat fee should be constant regardless of trade size");
        assertEquals(5.0, fee2, "Flat fee should be constant regardless of trade size");
    }

    @Test
    void testFeeCalculationMixed() {
        TradingConfig.FeeConfig feeConfig = tradingConfig.getFee();
        feeConfig.setMode("mixed");
        feeConfig.setPercent(0.1); // 0.1%
        feeConfig.setFlat(2.0);    // $2
        
        FeeService mixedFeeService = new FeeService(feeConfig);
        
        double notional = 10000.0; // $10,000 trade
        double expectedFee = (10000.0 * 0.001) + 2.0; // $10 + $2 = $12
        
        double actualFee = mixedFeeService.calculateFee(notional);
        assertEquals(expectedFee, actualFee, 0.01, "Mixed fee should combine percentage and flat components");
    }

    @Test
    void testSlippageCalculationLinear() {
        // Default slippage config: linear mode with k=0.0005
        double referencePrice = 100.0;
        double qty = 1000.0;
        
        // For buy order, price should increase
        double buyPrice = slippageService.calculateExecutionPrice(referencePrice, qty, "BUY");
        assertTrue(buyPrice > referencePrice, "Buy orders should have upward price impact");
        
        // For sell order, price should decrease
        double sellPrice = slippageService.calculateExecutionPrice(referencePrice, qty, "SELL");
        assertTrue(sellPrice < referencePrice, "Sell orders should have downward price impact");
        
        // Impact should scale with quantity
        double smallQtyPrice = slippageService.calculateExecutionPrice(referencePrice, 100.0, "BUY");
        assertTrue(smallQtyPrice < buyPrice, "Larger orders should have more price impact");
    }

    @Test
    void testSlippageCalculationSqrt() {
        TradingConfig.SlippageConfig slippageConfig = tradingConfig.getSlippage();
        slippageConfig.setMode("sqrtImpact");
        slippageConfig.setK(0.001);
        
        SlippageService sqrtSlippageService = new SlippageService(slippageConfig);
        
        double referencePrice = 100.0;
        double qty1 = 100.0;
        double qty2 = 400.0; // 4x quantity
        
        double price1 = sqrtSlippageService.calculateExecutionPrice(referencePrice, qty1, "BUY");
        double price2 = sqrtSlippageService.calculateExecutionPrice(referencePrice, qty2, "BUY");
        
        // Sqrt impact should grow slower than linear
        double impact1 = price1 - referencePrice;
        double impact2 = price2 - referencePrice;
        
        assertTrue(impact2 > impact1, "Larger quantity should have more impact");
        assertTrue(impact2 < impact1 * 4, "Sqrt impact should grow slower than linear");
    }

    @Test
    void testSlippageNoneMode() {
        TradingConfig.SlippageConfig slippageConfig = tradingConfig.getSlippage();
        slippageConfig.setMode("none");
        
        SlippageService noSlippageService = new SlippageService(slippageConfig);
        
        double referencePrice = 100.0;
        double executionPrice = noSlippageService.calculateExecutionPrice(referencePrice, 1000.0, "BUY");
        
        assertEquals(referencePrice, executionPrice, "No slippage mode should return reference price unchanged");
    }

    @Test
    void testOrderRequestValidation() {
        // Valid market order
        OrderRequest validOrder = OrderRequest.marketOrder("player123", "AAPL", "BUY", 100.0);
        assertDoesNotThrow(validOrder::validate, "Valid market order should pass validation");

        // Valid limit order
        OrderRequest validLimitOrder = OrderRequest.limitOrder("player123", "AAPL", "BUY", 100.0, 150.0);
        assertDoesNotThrow(validLimitOrder::validate, "Valid limit order should pass validation");

        // Valid stop order
        OrderRequest validStopOrder = OrderRequest.stopOrder("player123", "AAPL", "SELL", 100.0, 95.0);
        assertDoesNotThrow(validStopOrder::validate, "Valid stop order should pass validation");

        // Invalid orders
        assertThrows(IllegalArgumentException.class, () -> {
            OrderRequest.marketOrder(null, "AAPL", "BUY", 100.0).validate();
        }, "Null player UUID should fail validation");

        assertThrows(IllegalArgumentException.class, () -> {
            OrderRequest.marketOrder("player123", "AAPL", "INVALID", 100.0).validate();
        }, "Invalid side should fail validation");

        assertThrows(IllegalArgumentException.class, () -> {
            OrderRequest.marketOrder("player123", "AAPL", "BUY", -100.0).validate();
        }, "Negative quantity should fail validation");

        assertThrows(IllegalArgumentException.class, () -> {
            OrderRequest.limitOrder("player123", "AAPL", "BUY", 100.0, -150.0).validate();
        }, "Negative limit price should fail validation");
    }

    @Test
    void testTotalCostCalculation() {
        double notionalValue = 10000.0;
        double totalCost = feeService.calculateTotalCostWithFees(notionalValue);
        double expectedTotal = notionalValue + feeService.calculateFee(notionalValue);
        
        assertEquals(expectedTotal, totalCost, 0.01, "Total cost should include fees");
    }

    @Test
    void testNetProceedsCalculation() {
        double notionalValue = 10000.0;
        double netProceeds = feeService.calculateNetProceedsAfterFees(notionalValue);
        double expectedNet = notionalValue - feeService.calculateFee(notionalValue);
        
        assertEquals(expectedNet, netProceeds, 0.01, "Net proceeds should deduct fees");
    }
}