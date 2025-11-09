package net.cyberneticforge.quickstocks.core.services.features.market;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.config.TradingCfg;

/**
 * Service for calculating slippage impact on order execution prices.
 * Slippage represents the difference between expected and actual execution price
 * due to market impact and liquidity constraints.
 */
@SuppressWarnings("unused")
public class SlippageService {
    
    private final TradingCfg.SlippageConfig slippageConfig = QuickStocksPlugin.getTradingCfg().getSlippageConfig();
    
    /**
     * Calculates the execution price with slippage applied.
     * 
     * @param referencePrice The reference market price
     * @param qty The order quantity
     * @param side The order side (BUY or SELL)
     * @return The adjusted execution price including slippage
     */
    public double calculateExecutionPrice(double referencePrice, double qty, String side) {
        if (referencePrice <= 0 || qty <= 0) {
            return referencePrice;
        }
        
        String mode = slippageConfig.getMode().toLowerCase();
        if ("none".equals(mode)) {
            return referencePrice;
        }
        
        double impact = calculatePriceImpact(qty, mode);
        
        // Apply impact based on trade direction
        // Buy orders push price up, sell orders push price down
        if ("BUY".equalsIgnoreCase(side)) {
            return referencePrice * (1.0 + impact);
        } else {
            return referencePrice * (1.0 - impact);
        }
    }
    
    /**
     * Calculates the price impact as a percentage based on quantity and mode.
     * 
     * @param qty The order quantity
     * @param mode The slippage calculation mode
     * @return The price impact as a decimal (e.g., 0.01 = 1%)
     */
    private double calculatePriceImpact(double qty, String mode) {
        double k = slippageConfig.getK();

        return switch (mode) {
            case "linear" -> k * qty;
            case "sqrtimpact" -> k * Math.sqrt(qty);
            default ->
                // Default to linear if mode is unknown
                    k * qty;
        };
    }
    
    /**
     * Gets the slippage amount in currency units.
     * 
     * @param referencePrice The reference market price
     * @param executionPrice The actual execution price with slippage
     * @param qty The order quantity
     * @return The total slippage cost/benefit
     */
    public double getSlippageAmount(double referencePrice, double executionPrice, double qty) {
        return Math.abs(executionPrice - referencePrice) * qty;
    }
}