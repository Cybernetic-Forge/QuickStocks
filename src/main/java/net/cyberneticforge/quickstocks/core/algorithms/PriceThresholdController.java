package net.cyberneticforge.quickstocks.core.algorithms;

import net.cyberneticforge.quickstocks.core.model.Stock;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Controls price growth thresholds to prevent excessive stock price increases
 * when there is low trading activity. Provides configurable dampening based on
 * volume and price thresholds.
 */
@SuppressWarnings("unused")
public class PriceThresholdController {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final DatabaseConfig config;
    
    // Track initial prices for each stock to calculate growth multipliers
    private final Map<String, Double> initialPrices = new ConcurrentHashMap<>();
    
    // Track recent trading activity for each stock
    private final Map<String, Integer> recentTradingActivity = new ConcurrentHashMap<>();
    
    public PriceThresholdController(DatabaseConfig config) {
        this.config = config;
    }
    
    /**
     * Records the initial price of a stock for threshold calculations.
     * Should be called when a stock is first created or loaded.
     */
    public void recordInitialPrice(Stock stock) {
        String symbol = stock.getSymbol();
        if (!initialPrices.containsKey(symbol)) {
            initialPrices.put(symbol, stock.getCurrentPrice());
            recentTradingActivity.put(symbol, 0);
            logger.debug("Recorded initial price for " + symbol + ": $" + stock.getCurrentPrice());
        }
    }
    
    /**
     * Records trading activity for a stock (buy/sell transactions).
     * This affects how threshold dampening is applied.
     */
    public void recordTradingActivity(String symbol, int volume) {
        recentTradingActivity.merge(symbol, volume, Integer::sum);
    }
    
    /**
     * Resets trading activity counters. Should be called periodically
     * (e.g., every few minutes) to track recent activity.
     */
    public void resetTradingActivity() {
        recentTradingActivity.replaceAll((k, v) -> 0);
    }
    
    /**
     * Calculates the dampening factor to apply to price impact based on
     * current price thresholds and trading activity.
     * 
     * @param stock The stock to evaluate
     * @param proposedImpact The proposed price impact (as percentage change)
     * @return The dampening factor to multiply the impact by (0.0 to 1.0)
     */
    public double calculateDampeningFactor(Stock stock, double proposedImpact) {
        if (!config.isPriceThresholdEnabled()) {
            return 1.0; // No dampening if disabled
        }
        
        String symbol = stock.getSymbol();
        double currentPrice = stock.getCurrentPrice();
        
        // Ensure we have initial price recorded
        recordInitialPrice(stock);
        double initialPrice = initialPrices.get(symbol);
        
        // Calculate current price multiplier
        double priceMultiplier = currentPrice / initialPrice;
        
        // Check if we've exceeded the price threshold
        if (priceMultiplier <= config.getPriceMultiplierThreshold()) {
            return 1.0; // No dampening needed
        }
        
        // Check if the proposed impact would exceed max change percent
        if (Math.abs(proposedImpact) <= config.getMaxChangePercent()) {
            return 1.0; // Impact is within acceptable range
        }
        
        // Get recent trading activity
        int tradingVolume = recentTradingActivity.getOrDefault(symbol, 0);
        
        // If there's sufficient trading activity, reduce or eliminate dampening
        if (tradingVolume >= config.getMinVolumeThreshold()) {
            // Calculate volume-based dampening reduction
            double volumeReduction = Math.min(1.0, 
                (tradingVolume - config.getMinVolumeThreshold()) * config.getVolumeSensitivity() / config.getMinVolumeThreshold());
            
            // Interpolate between full dampening and no dampening based on volume
            double baseDampening = config.getDampeningFactor();
            double adjustedDampening = baseDampening + (1.0 - baseDampening) * volumeReduction;
            
            logger.debug(String.format("Stock %s: Volume dampening applied. Volume: %d, Base: %.2f, Adjusted: %.2f", 
                symbol, tradingVolume, baseDampening, adjustedDampening));
            
            return adjustedDampening;
        }
        
        // Apply full dampening due to low trading activity and high price growth
        logger.debug(String.format("Stock %s: Full dampening applied. Price multiplier: %.2f, Trading volume: %d", 
            symbol, priceMultiplier, tradingVolume));
        
        return config.getDampeningFactor();
    }
    
    /**
     * Gets the current price multiplier for a stock (current price / initial price).
     */
    public double getPriceMultiplier(Stock stock) {
        recordInitialPrice(stock);
        double initialPrice = initialPrices.get(stock.getSymbol());
        return stock.getCurrentPrice() / initialPrice;
    }
    
    /**
     * Gets the current trading activity for a stock.
     */
    public int getTradingActivity(String symbol) {
        return recentTradingActivity.getOrDefault(symbol, 0);
    }
    
    /**
     * Clears all tracked data. Useful for testing or reset scenarios.
     */
    public void clear() {
        initialPrices.clear();
        recentTradingActivity.clear();
    }
}