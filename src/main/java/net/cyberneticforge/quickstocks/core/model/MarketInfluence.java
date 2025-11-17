package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;
import net.cyberneticforge.quickstocks.core.enums.MarketFactor;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents the current influence of a market factor on stock prices.
 * Each influence has a strength and direction that affects price calculations.
 */
@Getter
@SuppressWarnings({"SameParameterValue", "unused"})
public class MarketInfluence {
    // Getters
    private final MarketFactor factor;
    private double currentValue; // -1.0 (very negative) to +1.0 (very positive)
    private double intensity; // 0.0 to 1.0, how strong the influence currently is
    private LocalDateTime lastUpdated;
    private double trendDirection; // -1.0 (declining) to +1.0 (increasing)
    
    public MarketInfluence(MarketFactor factor) {
        this.factor = Objects.requireNonNull(factor, "Factor cannot be null");
        this.currentValue = 0.0; // Neutral by default
        this.intensity = 0.5; // Medium intensity by default
        this.lastUpdated = LocalDateTime.now();
        this.trendDirection = 0.0; // No trend by default
    }
    
    public MarketInfluence(MarketFactor factor, double currentValue, double intensity) {
        this.factor = Objects.requireNonNull(factor, "Factor cannot be null");
        this.currentValue = clamp(currentValue, -1.0, 1.0);
        this.intensity = clamp(intensity, 0.0, 1.0);
        this.lastUpdated = LocalDateTime.now();
        this.trendDirection = 0.0;
    }
    
    /**
     * Updates the influence values and timestamp.
     */
    public void updateInfluence(double newValue, double newIntensity) {
        double oldValue = this.currentValue;
        this.currentValue = clamp(newValue, -1.0, 1.0);
        this.intensity = clamp(newIntensity, 0.0, 1.0);
        this.lastUpdated = LocalDateTime.now();
        
        // Calculate trend direction based on change
        this.trendDirection = clamp(newValue - oldValue, -1.0, 1.0);
    }
    
    /**
     * Calculates the total impact this influence has on stock price.
     * Combines the factor's base weight with current values and volatility.
     * 
     * @return Impact multiplier (-2.0 to +2.0 typically)
     */
    public double calculateImpact() {
        double baseImpact = factor.getBaseWeight() * currentValue * intensity;
        double volatilityMultiplier = 1.0 + (factor.getVolatility() * intensity * 0.5);
        return baseImpact * volatilityMultiplier;
    }
    
    /**
     * @return True if this influence is currently having a positive effect
     */
    public boolean isPositive() {
        return currentValue > 0;
    }
    
    /**
     * @return True if this influence is currently having a negative effect
     */
    public boolean isNegative() {
        return currentValue < 0;
    }
    
    /**
     * @return True if this influence is currently neutral
     */
    public boolean isNeutral() {
        return Math.abs(currentValue) < 0.1;
    }
    
    /**
     * @return True if this influence is currently strong (intensity > 0.7)
     */
    public boolean isStrong() {
        return intensity > 0.7;
    }
    
    /**
     * Simulates a random fluctuation in the influence based on the factor's volatility.
     * Uses smaller, more realistic changes that accumulate over time.
     */
    public void applyRandomFluctuation() {
        // Reduced fluctuation range: max 10% change per update (was 20%)
        double volatilityRange = factor.getVolatility() * 0.1;
        double randomChange = (Math.random() - 0.5) * 2 * volatilityRange;
        
        double newValue = currentValue + randomChange;
        // Smaller intensity changes: max ±2.5% per update (was ±5%)
        double newIntensity = intensity + (Math.random() - 0.5) * 0.05;
        
        updateInfluence(newValue, newIntensity);
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MarketInfluence that = (MarketInfluence) o;
        return factor == that.factor;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(factor);
    }
    
    @Override
    public String toString() {
        return String.format("MarketInfluence{%s: %.2f (intensity: %.2f)}", 
            factor.name(), currentValue, intensity);
    }
}