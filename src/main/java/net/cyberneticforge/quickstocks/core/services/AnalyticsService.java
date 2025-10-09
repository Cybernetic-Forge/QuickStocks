package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.db.Db;

import java.util.*;
import java.util.logging.Logger;

/**
 * Service for calculating analytics metrics like EWMA volatility, correlations, and Sharpe ratios.
 */
@SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public double getChangePct(String instrumentId, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            var results = database.query("""
                SELECT price, ts FROM instrument_price_history\s
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
                LIMIT 1
               \s""", instrumentId, windowStart);
            
            if (results.isEmpty()) {
                return 0.0;
            }
            
            double oldPrice = ((Number) results.getFirst().get("price")).doubleValue();
            
            // Get the most recent price
            var currentResults = database.query("""
                SELECT price FROM instrument_price_history\s
                WHERE instrument_id = ?
                ORDER BY ts DESC
                LIMIT 1
               \s""", instrumentId);
            
            if (currentResults.isEmpty()) {
                return 0.0;
            }
            
            double currentPrice = ((Number) currentResults.getFirst().get("price")).doubleValue();
            
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
                SELECT price, ts FROM instrument_price_history\s
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
               \s""", instrumentId, windowStart);
            
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
            double ewmaVariance = getEwmaVariance(lambda, returns);

            return Math.sqrt(ewmaVariance);
            
        } catch (Exception e) {
            logger.warning("Failed to calculate EWMA volatility for " + instrumentId + ": " + e.getMessage());
            return 0.0;
        }
    }

    private static double getEwmaVariance(double lambda, List<Double> returns) {
        double ewmaVariance = 0.0;
        if (returns.size() >= 2) {
            // Initialize with sample variance of first two returns for stability
            double mean = (returns.get(0) + returns.get(1)) / 2.0;
            ewmaVariance = (Math.pow(returns.get(0) - mean, 2) + Math.pow(returns.get(1) - mean, 2)) / 2.0;
        } else {
            ewmaVariance = Math.pow(returns.getFirst(), 2); // Single return case
        }

        // Apply EWMA to remaining returns
        for (int i = Math.max(2, 1); i < returns.size(); i++) {
            double returnSquared = Math.pow(returns.get(i), 2);
            ewmaVariance = lambda * ewmaVariance + (1.0 - lambda) * returnSquared;
        }
        return ewmaVariance;
    }

    /**
     * Overloaded method using default lambda.
     */
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public double getCorrelation(String instrumentA, String instrumentB, int windowMinutes) {
        try {
            long windowStart = System.currentTimeMillis() - (windowMinutes * 60 * 1000L);
            
            // Get price history for both instruments
            var resultsA = database.query("""
                SELECT price, ts FROM instrument_price_history\s
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
               \s""", instrumentA, windowStart);
                
            var resultsB = database.query("""
                SELECT price, ts FROM instrument_price_history\s
                WHERE instrument_id = ? AND ts >= ?
                ORDER BY ts ASC
               \s""", instrumentB, windowStart);
            
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
    @SuppressWarnings("unused")
    public double getSharpe(String playerUuid, int windowDays, double riskFree) {
        try {
            // Query portfolio performance data for the player within the window
            var results = database.query("""
                SELECT\s
                    avg_return,
                    return_std_dev,
                    return_count,
                    total_return
                FROM sharpe_ratio_data\s
                WHERE player_uuid = ?
               \s""", playerUuid);
            
            if (results.isEmpty()) {
                logger.info("No portfolio data found for player " + playerUuid + " - Sharpe ratio cannot be calculated");
                return 0.0;
            }
            
            var result = results.getFirst();
            double avgReturn = ((Number) result.get("avg_return")).doubleValue();
            double stdDev = ((Number) result.get("return_std_dev")).doubleValue();
            int returnCount = ((Number) result.get("return_count")).intValue();
            
            // Need sufficient data points and non-zero standard deviation
            if (returnCount < 5 || stdDev <= 0.0) {
                logger.fine("Insufficient data or zero volatility for Sharpe calculation for player " + playerUuid);
                return 0.0;
            }
            
            // Calculate Sharpe ratio: (Average Return - Risk Free Rate) / Standard Deviation
            double excessReturn = avgReturn - (riskFree / 365.0); // Convert annual risk-free rate to daily
            double sharpeRatio = excessReturn / stdDev;
            
            logger.fine(String.format("Calculated Sharpe ratio for %s: %.4f (avg_return=%.6f, std_dev=%.6f, excess=%.6f)", 
                playerUuid, sharpeRatio, avgReturn, stdDev, excessReturn));
            
            return sharpeRatio;
            
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
    @SuppressWarnings("unused")
    public double getDefaultLambda() { return defaultLambda; }
    @SuppressWarnings("unused")
    public int getDefaultChangeWindow() { return defaultChangeWindow; }
    @SuppressWarnings("unused")
    public int getDefaultVolatilityWindow() { return defaultVolatilityWindow; }
    @SuppressWarnings("unused")
    public int getDefaultCorrelationWindow() { return defaultCorrelationWindow; }
    
    /**
     * Records a portfolio value snapshot for Sharpe ratio calculations.
     * This should be called periodically to build portfolio history.
     * @param playerUuid Player UUID
     * @param totalValue Current total portfolio value
     * @param cashBalance Cash balance
     * @param holdingsValue Value of all holdings
     */
    @SuppressWarnings("unused")
    public void recordPortfolioValue(String playerUuid, double totalValue, double cashBalance, double holdingsValue) {
        try {
            String id = java.util.UUID.randomUUID().toString();
            long currentTime = System.currentTimeMillis();
            
            database.execute("""
                INSERT INTO portfolio_history\s
                (id, player_uuid, ts, total_value, cash_balance, holdings_value, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
               \s""", id, playerUuid, currentTime, totalValue, cashBalance, holdingsValue, currentTime);
                
            logger.fine(String.format("Recorded portfolio value for %s: total=%.2f, cash=%.2f, holdings=%.2f", 
                playerUuid, totalValue, cashBalance, holdingsValue));
                
        } catch (Exception e) {
            logger.warning("Failed to record portfolio value for player " + playerUuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Gets portfolio performance summary for a player.
     * @param playerUuid Player UUID
     * @return Map containing performance metrics, or empty map if no data
     */
    @SuppressWarnings("unused")
    public Map<String, Object> getPortfolioPerformance(String playerUuid) {
        try {
            var results = database.query("""
                SELECT\s
                    avg_return,
                    return_std_dev,
                    return_count,
                    total_return
                FROM sharpe_ratio_data\s
                WHERE player_uuid = ?
               \s""", playerUuid);
            
            if (results.isEmpty()) {
                return new HashMap<>();
            }
            
            return results.getFirst();
            
        } catch (Exception e) {
            logger.warning("Failed to get portfolio performance for player " + playerUuid + ": " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Gets Sharpe ratio leaderboard - players with best risk-adjusted returns.
     * @param limit Maximum number of players to return
     * @param riskFreeRate Risk-free rate for Sharpe calculation
     * @return List of player performance data ordered by Sharpe ratio (descending)
     */
    @SuppressWarnings("unused")
    public List<Map<String, Object>> getSharpeLeaderboard(int limit, double riskFreeRate) {
        try {
            var results = database.query("""
                SELECT\s
                    player_uuid,
                    avg_return,
                    return_std_dev,
                    return_count,
                    total_return,
                    CASE\s
                        WHEN return_std_dev > 0 THEN (avg_return - ?) / return_std_dev
                        ELSE 0.0
                    END as sharpe_ratio
                FROM sharpe_ratio_data\s
                WHERE return_count >= 5 AND return_std_dev > 0
                ORDER BY sharpe_ratio DESC
                LIMIT ?
               \s""", riskFreeRate / 365.0, limit); // Convert annual rate to daily
            
            logger.fine("Retrieved " + results.size() + " players for Sharpe leaderboard");
            return results;
            
        } catch (Exception e) {
            logger.warning("Failed to get Sharpe leaderboard: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}