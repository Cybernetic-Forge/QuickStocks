package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.util.*;
import java.util.logging.Logger;

/**
 * Service for calculating analytics metrics like EWMA volatility, correlations, and Sharpe ratios.
 */
public class AnalyticsService {
    
    private static final Logger logger = Logger.getLogger(AnalyticsService.class.getName());
    
    private final Db database;
    private final double defaultLambda;
    private final int defaultChangeWindow;
    private final int defaultVolatilityWindow;
    private final int defaultCorrelationWindow;
    
    public AnalyticsService(Db database, double lambda, int changeWindow, int volatilityWindow, int correlationWindow) {
        this.database = Objects.requireNonNull(database, "Database cannot be null");
        this.defaultLambda = lambda;
        this.defaultChangeWindow = changeWindow;
        this.defaultVolatilityWindow = volatilityWindow;
        this.defaultCorrelationWindow = correlationWindow;
    }
    
    /**
     * Gets price change percentage over a given time window.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @return Change percentage (-1.0 to +1.0)
     */
    public double getChangePct(String instrumentId, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            var results = database.query("""
                SELECT price, ts FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                LIMIT 1
                """, instrumentId, windowStart);
            
            if (results.isEmpty()) {
                return 0.0;
            }
            
            double oldPrice = ((Number) results.get(0).get("price")).doubleValue();
            
            // Get the most recent price
            var currentResults = database.query("""
                SELECT price FROM instrument_price_history 
                WHERE instrument_id = ?
                ORDER BY ts DESC
                LIMIT 1
                """, instrumentId);
            
            if (currentResults.isEmpty()) {
                return 0.0;
            }
            
            double currentPrice = ((Number) currentResults.get(0).get("price")).doubleValue();
            
            return oldPrice > 0 ? (currentPrice - oldPrice) / oldPrice : 0.0;
            
        } catch (Exception e) {
            logger.warning("Failed to calculate change percent for " + instrumentId + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculates EWMA (Exponentially Weighted Moving Average) volatility.
     * Uses λ (lambda) to give more weight to recent observations.
     * @param instrumentId The instrument identifier
     * @param windowMinutes Time window in minutes
     * @param lambda Decay factor (0 < λ < 1), default 0.94
     * @return EWMA volatility
     */
    public double getVolatilityEWMA(String instrumentId, int windowMinutes, double lambda) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            var results = database.query("""
                SELECT price, ts FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                """, instrumentId, windowStart);
            
            if (results.size() < 2) {
                return 0.0;
            }
            
            // Calculate returns (percentage changes)
            List<Double> returns = new ArrayList<>();
            for (int i = 1; i < results.size(); i++) {
                double prevPrice = ((Number) results.get(i-1).get("price")).doubleValue();
                double currPrice = ((Number) results.get(i).get("price")).doubleValue();
                
                if (prevPrice > 0) {
                    returns.add((currPrice - prevPrice) / prevPrice);
                }
            }
            
            if (returns.isEmpty()) {
                return 0.0;
            }
            
            // Calculate EWMA variance
            // σ²(t) = λ × σ²(t-1) + (1-λ) × r²(t)
            double ewmaVariance = 0.0;
            if (returns.size() >= 2) {
                // Initialize with sample variance of first two returns for stability
                double mean = (returns.get(0) + returns.get(1)) / 2.0;
                ewmaVariance = (Math.pow(returns.get(0) - mean, 2) + Math.pow(returns.get(1) - mean, 2)) / 2.0;
            } else {
                ewmaVariance = Math.pow(returns.get(0), 2); // Single return case
            }
            
            // Apply EWMA to remaining returns
            for (int i = Math.max(2, 1); i < returns.size(); i++) {
                double returnSquared = Math.pow(returns.get(i), 2);
                ewmaVariance = lambda * ewmaVariance + (1.0 - lambda) * returnSquared;
            }
            
            return Math.sqrt(ewmaVariance);
            
        } catch (Exception e) {
            logger.warning("Failed to calculate EWMA volatility for " + instrumentId + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Overloaded method using default lambda.
     */
    public double getVolatilityEWMA(String instrumentId, int windowMinutes) {
        return getVolatilityEWMA(instrumentId, windowMinutes, defaultLambda);
    }
    
    /**
     * Calculates correlation between two instruments over a time window.
     * @param instrumentA First instrument ID
     * @param instrumentB Second instrument ID  
     * @param windowMinutes Time window in minutes
     * @return Correlation coefficient (-1.0 to +1.0)
     */
    public double getCorrelation(String instrumentA, String instrumentB, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            // Get price history for both instruments
            var resultsA = database.query("""
                SELECT price, ts FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                """, instrumentA, windowStart);
                
            var resultsB = database.query("""
                SELECT price, ts FROM instrument_price_history 
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                """, instrumentB, windowStart);
            
            if (resultsA.size() < 2 || resultsB.size() < 2) {
                return 0.0;
            }
            
            // Calculate returns for both instruments
            List<Double> returnsA = calculateReturns(resultsA);
            List<Double> returnsB = calculateReturns(resultsB);
            
            // Align time series (use minimum length)
            int minLength = Math.min(returnsA.size(), returnsB.size());
            if (minLength < 2) {
                return 0.0;
            }
            
            returnsA = returnsA.subList(0, minLength);
            returnsB = returnsB.subList(0, minLength);
            
            // Calculate correlation coefficient
            return calculatePearsonCorrelation(returnsA, returnsB);
            
        } catch (Exception e) {
            logger.warning("Failed to calculate correlation between " + instrumentA + " and " + instrumentB + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Calculates Sharpe ratio for a player's portfolio.
     * Sharpe = (Portfolio Return - Risk Free Rate) / Portfolio Standard Deviation
     * @param playerUuid Player UUID
     * @param windowDays Time window in days
     * @param riskFree Risk-free rate (default 0)
     * @return Sharpe ratio
     */
    public double getSharpe(String playerUuid, int windowDays, double riskFree) {
        try {
            long windowStart = System.currentTimeMillis() - (windowDays * 24 * 60 * 60 * 1000L);
            
            // Get player's portfolio performance over time
            // This would require portfolio value tracking - for now return 0.0
            // TODO: Implement portfolio value tracking
            logger.info("Sharpe ratio calculation requires portfolio tracking - returning 0.0 for now");
            
            return 0.0;
            
        } catch (Exception e) {
            logger.warning("Failed to calculate Sharpe ratio for player " + playerUuid + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Helper method to calculate returns from price history results.
     */
    private List<Double> calculateReturns(List<Map<String, Object>> priceHistory) {
        List<Double> returns = new ArrayList<>();
        
        for (int i = 1; i < priceHistory.size(); i++) {
            double prevPrice = ((Number) priceHistory.get(i-1).get("price")).doubleValue();
            double currPrice = ((Number) priceHistory.get(i).get("price")).doubleValue();
            
            if (prevPrice > 0) {
                returns.add((currPrice - prevPrice) / prevPrice);
            }
        }
        
        return returns;
    }
    
    /**
     * Helper method to calculate Pearson correlation coefficient.
     */
    private double calculatePearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.size() < 2) {
            return 0.0;
        }
        
        int n = x.size();
        double sumX = x.stream().mapToDouble(Double::doubleValue).sum();
        double sumY = y.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumXSquared = 0.0;
        double sumYSquared = 0.0;
        
        for (int i = 0; i < n; i++) {
            sumXY += x.get(i) * y.get(i);
            sumXSquared += x.get(i) * x.get(i);
            sumYSquared += y.get(i) * y.get(i);
        }
        
        double numerator = n * sumXY - sumX * sumY;
        double denominator = Math.sqrt((n * sumXSquared - sumX * sumX) * (n * sumYSquared - sumY * sumY));
        
        return denominator != 0 ? numerator / denominator : 0.0;
    }
    
    // Getters for default values
    public double getDefaultLambda() { return defaultLambda; }
    public int getDefaultChangeWindow() { return defaultChangeWindow; }
    public int getDefaultVolatilityWindow() { return defaultVolatilityWindow; }
    public int getDefaultCorrelationWindow() { return defaultCorrelationWindow; }
}