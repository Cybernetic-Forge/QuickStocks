package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.config.TradingConfig;

/**
 * Service for calculating trading fees based on configuration.
 */
public class FeeService {
    
    private final TradingConfig.FeeConfig feeConfig;
    
    public FeeService(TradingConfig.FeeConfig feeConfig) {
        this.feeConfig = feeConfig;
    }
    
    /**
     * Calculates the trading fee for a given notional value.
     * 
     * @param notionalValue The total value of the trade (qty * price)
     * @return The fee amount to be charged
     */
    public double calculateFee(double notionalValue) {
        if (notionalValue <= 0) {
            return 0.0;
        }

        return switch (feeConfig.getMode().toLowerCase()) {
            case "percent" -> notionalValue * (feeConfig.getPercent() / 100.0);
            case "flat" -> feeConfig.getFlat();
            case "mixed" -> notionalValue * (feeConfig.getPercent() / 100.0) + feeConfig.getFlat();
            default ->
                // Default to percentage if mode is unknown
                    notionalValue * (feeConfig.getPercent() / 100.0);
        };
    }
    
    /**
     * Calculates the total cost including fees for a buy order.
     * 
     * @param notionalValue The base trade value (qty * price)
     * @return The total cost including fees
     */
    public double calculateTotalCostWithFees(double notionalValue) {
        return notionalValue + calculateFee(notionalValue);
    }
    
    /**
     * Calculates the net proceeds after fees for a sell order.
     * 
     * @param notionalValue The base trade value (qty * price)
     * @return The net proceeds after deducting fees
     */
    public double calculateNetProceedsAfterFees(double notionalValue) {
        return notionalValue - calculateFee(notionalValue);
    }
}