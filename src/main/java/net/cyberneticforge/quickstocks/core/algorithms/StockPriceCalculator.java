package net.cyberneticforge.quickstocks.core.algorithms;

import net.cyberneticforge.quickstocks.core.enums.MarketFactor;
import net.cyberneticforge.quickstocks.core.model.MarketInfluence;
import net.cyberneticforge.quickstocks.core.model.Stock;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Advanced stock price calculation algorithm that simulates realistic market behavior
 * by considering multiple market factors, technical indicators, and random events.
 */
public class StockPriceCalculator {
    
    private static final Logger logger = Logger.getLogger(StockPriceCalculator.class.getName());
    
    private final Random random;
    private final PriceThresholdController thresholdController;
    private static final double BASE_VOLATILITY = 0.02; // 2% base daily volatility
    private static final double MAX_PRICE_CHANGE = 0.20; // 20% maximum change per update
    private static final double MOMENTUM_DECAY = 0.95; // How quickly momentum fades
    
    public StockPriceCalculator() {
        this.random = new Random();
        this.thresholdController = null; // Will be set later via setter
    }
    
    public StockPriceCalculator(long seed) {
        this.random = new Random(seed); // Deterministic for testing
        this.thresholdController = null; // Will be set later via setter
    }
    
    public StockPriceCalculator(PriceThresholdController thresholdController) {
        this.random = new Random();
        this.thresholdController = thresholdController;
    }
    
    public StockPriceCalculator(long seed, PriceThresholdController thresholdController) {
        this.random = new Random(seed);
        this.thresholdController = thresholdController;
    }
    
    /**
     * Calculates the new stock price based on current market influences and stock characteristics.
     * 
     * @param stock The stock to update
     * @param marketInfluences Current market factor influences
     * @return The new calculated price
     */
    public double calculateNewPrice(Stock stock, List<MarketInfluence> marketInfluences) {
        double currentPrice = stock.getCurrentPrice();
        
        // Calculate base change from market influences
        double influenceImpact = calculateInfluenceImpact(marketInfluences, stock);
        
        // Apply technical analysis factors
        double technicalImpact = calculateTechnicalImpact(stock);
        
        // Add random market noise
        double randomNoise = calculateRandomNoise(stock);
        
        // Apply momentum and trend following
        double momentumImpact = calculateMomentumImpact(stock);
        
        // Combine all impacts with weights
        double totalImpact = (influenceImpact * 0.4) + 
                           (technicalImpact * 0.2) + 
                           (randomNoise * 0.2) + 
                           (momentumImpact * 0.2);
        
        // Apply stock-specific volatility multiplier
        totalImpact *= (1.0 + stock.getVolatilityRating());
        
        // Apply price threshold dampening if controller is available
        if (thresholdController != null) {
            double dampeningFactor = thresholdController.calculateDampeningFactor(stock, totalImpact);
            if (dampeningFactor < 1.0) {
                totalImpact *= dampeningFactor;
                logger.fine(String.format("Applied threshold dampening to %s: factor=%.2f, original impact=%.4f, dampened impact=%.4f", 
                    stock.getSymbol(), dampeningFactor, totalImpact / dampeningFactor, totalImpact));
            }
        }
        
        // Ensure changes don't exceed maximum limits
        totalImpact = Math.max(-MAX_PRICE_CHANGE, Math.min(MAX_PRICE_CHANGE, totalImpact));
        
        // Calculate new price
        double newPrice = currentPrice * (1.0 + totalImpact);
        
        // Apply price boundaries (stocks can't go below $0.01)
        newPrice = Math.max(0.01, newPrice);
        
        // Apply mean reversion for extreme prices
        newPrice = applyMeanReversion(stock, newPrice);
        
        return newPrice;
    }
    
    /**
     * Calculates the impact of market influences on the stock price.
     */
    private double calculateInfluenceImpact(List<MarketInfluence> influences, Stock stock) {
        if (influences.isEmpty()) return 0.0;
        
        double totalImpact = 0.0;
        double totalWeight = 0.0;
        
        for (MarketInfluence influence : influences) {
            double impact = influence.calculateImpact();
            double weight = getFactorWeightForStock(influence.getFactor(), stock);
            
            totalImpact += impact * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? totalImpact / totalWeight : 0.0;
    }
    
    /**
     * Gets the weight of a specific factor for a particular stock based on sector and characteristics.
     */
    private double getFactorWeightForStock(MarketFactor factor, Stock stock) {
        double baseWeight = factor.getBaseWeight();
        
        // Adjust weights based on stock sector
        switch (stock.getSector().toLowerCase()) {
            case "technology":
                if (factor == MarketFactor.SOCIAL_MEDIA_BUZZ || 
                    factor == MarketFactor.PRODUCT_LAUNCHES) {
                    baseWeight *= 1.5;
                }
                break;
            case "energy":
                if (factor == MarketFactor.COMMODITY_PRICES || 
                    factor == MarketFactor.GEOPOLITICAL_EVENTS) {
                    baseWeight *= 1.3;
                }
                break;
            case "finance":
                if (factor == MarketFactor.INTEREST_RATES || 
                    factor == MarketFactor.REGULATORY_CHANGES) {
                    baseWeight *= 1.4;
                }
                break;
            case "healthcare":
                if (factor == MarketFactor.PANDEMIC_IMPACT || 
                    factor == MarketFactor.REGULATORY_CHANGES) {
                    baseWeight *= 1.3;
                }
                break;
        }
        
        // Adjust for stock volatility rating
        baseWeight *= (1.0 + stock.getVolatilityRating() * 0.2);
        
        return baseWeight;
    }
    
    /**
     * Calculates technical analysis impact based on price history and patterns.
     */
    private double calculateTechnicalImpact(Stock stock) {
        var priceHistory = stock.getPriceHistory();
        if (priceHistory.size() < 5) return 0.0;
        
        double impact = 0.0;
        
        // Support/Resistance levels
        impact += calculateSupportResistanceImpact(stock, priceHistory);
        
        // Moving average trends
        impact += calculateMovingAverageTrend(priceHistory);
        
        // Volume analysis
        impact += calculateVolumeImpact(stock);
        
        return impact / 3.0; // Average the impacts
    }
    
    private double calculateSupportResistanceImpact(Stock stock, List<Stock.PriceHistory> history) {
        double currentPrice = stock.getCurrentPrice();
        
        // Find recent high and low
        double recentHigh = history.stream()
            .skip(Math.max(0, history.size() - 20))
            .mapToDouble(Stock.PriceHistory::price)
            .max().orElse(currentPrice);
            
        double recentLow = history.stream()
            .skip(Math.max(0, history.size() - 20))
            .mapToDouble(Stock.PriceHistory::price)
            .min().orElse(currentPrice);
        
        // If near resistance, slight downward pressure
        if (currentPrice > recentHigh * 0.95) {
            return -0.02;
        }
        // If near support, slight upward pressure
        else if (currentPrice < recentLow * 1.05) {
            return 0.02;
        }
        
        return 0.0;
    }
    
    private double calculateMovingAverageTrend(List<Stock.PriceHistory> history) {
        if (history.size() < 10) return 0.0;
        
        // Calculate simple moving averages
        double shortMA = history.stream()
            .skip(history.size() - 5)
            .mapToDouble(Stock.PriceHistory::price)
            .average().orElse(0.0);
            
        double longMA = history.stream()
            .skip(history.size() - 10)
            .mapToDouble(Stock.PriceHistory::price)
            .average().orElse(0.0);
        
        // Trending up if short MA > long MA
        return longMA > 0 ? (shortMA - longMA) / longMA * 0.5 : 0.0;
    }
    
    private double calculateVolumeImpact(Stock stock) {
        double volume = stock.getDailyVolume();
        
        // High volume can amplify price movements
        if (volume > 1000000) { // High volume threshold
            return stock.isGaining() ? 0.01 : -0.01;
        }
        
        return 0.0;
    }
    
    /**
     * Adds random market noise to simulate unpredictable events.
     */
    private double calculateRandomNoise(Stock stock) {
        double baseNoise = BASE_VOLATILITY * (random.nextGaussian() * 0.5);
        
        // Occasional larger random events (1% chance)
        if (random.nextDouble() < 0.01) {
            baseNoise *= (2.0 + random.nextDouble() * 3.0); // 2x to 5x multiplier
        }
        
        return baseNoise;
    }
    
    /**
     * Calculates momentum impact based on recent price movements.
     */
    private double calculateMomentumImpact(Stock stock) {
        double priceChange = stock.getPriceChangePercent();
        
        // Momentum continues in the same direction but decays
        return priceChange * MOMENTUM_DECAY * 0.3;
    }
    
    /**
     * Applies mean reversion to prevent prices from becoming unrealistic.
     */
    private double applyMeanReversion(Stock stock, double newPrice) {
        var history = stock.getPriceHistory();
        if (history.size() < 50) return newPrice;
        
        // Calculate long-term average price
        double longTermAverage = history.stream()
            .skip(Math.max(0, history.size() - 50))
            .mapToDouble(Stock.PriceHistory::price)
            .average().orElse(newPrice);
        
        // If price is very far from average, apply gentle pull back
        double deviation = (newPrice - longTermAverage) / longTermAverage;
        if (Math.abs(deviation) > 0.5) { // More than 50% deviation
            double pullback = deviation * 0.05; // 5% pullback toward mean
            newPrice -= newPrice * pullback;
        }
        
        return newPrice;
    }
    
    /**
     * Updates market influences with realistic fluctuations.
     */
    public void updateMarketInfluences(List<MarketInfluence> influences) {
        for (MarketInfluence influence : influences) {
            influence.applyRandomFluctuation();
            
            // Occasionally apply major events
            if (random.nextDouble() < 0.001) { // 0.1% chance per update
                applyMajorEvent(influence);
            }
        }
    }
    
    /**
     * Applies a major market event to an influence.
     */
    private void applyMajorEvent(MarketInfluence influence) {
        double eventStrength = random.nextGaussian() * 0.3; // Can be positive or negative
        double newIntensity = Math.min(1.0, influence.getIntensity() + Math.abs(eventStrength));
        
        influence.updateInfluence(
            Math.max(-1.0, Math.min(1.0, influence.getCurrentValue() + eventStrength)),
            newIntensity
        );
    }
    
    /**
     * Records trading activity for a stock, which affects threshold dampening.
     * Should be called when buy/sell orders are executed.
     */
    public void recordTradingActivity(String symbol, int volume) {
        if (thresholdController != null) {
            thresholdController.recordTradingActivity(symbol, volume);
        }
    }
    
    /**
     * Gets the threshold controller for direct access.
     */
    public PriceThresholdController getThresholdController() {
        return thresholdController;
    }
}