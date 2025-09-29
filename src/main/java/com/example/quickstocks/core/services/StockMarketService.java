package com.example.quickstocks.core.services;

import com.example.quickstocks.core.algorithms.PriceThresholdController;
import com.example.quickstocks.core.algorithms.StockPriceCalculator;
import com.example.quickstocks.core.enums.MarketFactor;
import com.example.quickstocks.core.models.MarketInfluence;
import com.example.quickstocks.core.models.Stock;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Core service that manages the stock market simulation.
 * Handles stock registration, price updates, and market factor management.
 */
public class StockMarketService {
    
    private final Map<String, Stock> stocks;
    private final List<MarketInfluence> marketInfluences;
    private final StockPriceCalculator priceCalculator;
    private final PriceThresholdController thresholdController;
    private volatile boolean marketOpen;
    
    public StockMarketService() {
        this.stocks = new ConcurrentHashMap<>();
        this.marketInfluences = initializeMarketInfluences();
        this.thresholdController = null;
        this.priceCalculator = new StockPriceCalculator();
        this.marketOpen = true;
    }
    
    public StockMarketService(PriceThresholdController thresholdController) {
        this.stocks = new ConcurrentHashMap<>();
        this.marketInfluences = initializeMarketInfluences();
        this.thresholdController = thresholdController;
        this.priceCalculator = new StockPriceCalculator(thresholdController);
        this.marketOpen = true;
    }
    
    /**
     * Initialize all market influences with default values.
     */
    private List<MarketInfluence> initializeMarketInfluences() {
        return Arrays.stream(MarketFactor.values())
            .map(factor -> {
                // Start with slight random bias
                double initialValue = (Math.random() - 0.5) * 0.4; // -0.2 to +0.2
                double initialIntensity = 0.3 + Math.random() * 0.4; // 0.3 to 0.7
                return new MarketInfluence(factor, initialValue, initialIntensity);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Registers a new stock in the market.
     */
    public void addStock(String symbol, String name, String sector, double initialPrice) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or empty");
        }
        
        if (stocks.containsKey(symbol.toUpperCase())) {
            throw new IllegalArgumentException("Stock with symbol " + symbol + " already exists");
        }
        
        Stock stock = new Stock(symbol.toUpperCase(), name, sector, initialPrice);
        
        // Set volatility based on sector
        stock.setVolatilityRating(getSectorVolatility(sector));
        
        // Record initial price for threshold calculations
        if (thresholdController != null) {
            thresholdController.recordInitialPrice(stock);
        }
        
        stocks.put(symbol.toUpperCase(), stock);
    }
    
    /**
     * Gets the default volatility rating for a sector.
     */
    private double getSectorVolatility(String sector) {
        switch (sector.toLowerCase()) {
            case "technology": return 0.8;
            case "biotech": return 0.9;
            case "crypto": return 1.0;
            case "energy": return 0.7;
            case "finance": return 0.6;
            case "healthcare": return 0.5;
            case "utilities": return 0.3;
            case "consumer": return 0.4;
            default: return 0.5;
        }
    }
    
    /**
     * Updates all stock prices based on current market conditions.
     */
    public void updateAllStockPrices() {
        if (!marketOpen) return;
        
        // First update market influences
        priceCalculator.updateMarketInfluences(marketInfluences);
        
        // Then update all stock prices
        for (Stock stock : stocks.values()) {
            double newPrice = priceCalculator.calculateNewPrice(stock, marketInfluences);
            stock.updatePrice(newPrice);
            
            // Update volume with some randomness
            double newVolume = Math.max(0, stock.getDailyVolume() + 
                (Math.random() - 0.5) * 100000);
            stock.updateVolume(newVolume);
        }
    }
    
    /**
     * Gets a stock by its symbol.
     */
    public Optional<Stock> getStock(String symbol) {
        return Optional.ofNullable(stocks.get(symbol.toUpperCase()));
    }
    
    /**
     * Gets all registered stocks.
     */
    public Collection<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }
    
    /**
     * Gets stocks in a specific sector.
     */
    public List<Stock> getStocksBySector(String sector) {
        return stocks.values().stream()
            .filter(stock -> stock.getSector().equalsIgnoreCase(sector))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the top performing stocks.
     */
    public List<Stock> getTopPerformers(int count) {
        return stocks.values().stream()
            .sorted((a, b) -> Double.compare(b.getPriceChangePercent(), a.getPriceChangePercent()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the worst performing stocks.
     */
    public List<Stock> getWorstPerformers(int count) {
        return stocks.values().stream()
            .sorted((a, b) -> Double.compare(a.getPriceChangePercent(), b.getPriceChangePercent()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Applies a major market event that affects specific influences.
     */
    public void applyMarketEvent(MarketFactor factor, double impact, double intensity) {
        marketInfluences.stream()
            .filter(influence -> influence.getFactor() == factor)
            .findFirst()
            .ifPresent(influence -> influence.updateInfluence(impact, intensity));
    }
    
    /**
     * Gets current market influences for transparency.
     */
    public List<MarketInfluence> getMarketInfluences() {
        return new ArrayList<>(marketInfluences);
    }
    
    /**
     * Gets the most impactful current market influences.
     */
    public List<MarketInfluence> getTopInfluences(int count) {
        return marketInfluences.stream()
            .sorted((a, b) -> Double.compare(
                Math.abs(b.calculateImpact()), 
                Math.abs(a.calculateImpact())
            ))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Calculates overall market sentiment (-1.0 to +1.0).
     */
    public double getMarketSentiment() {
        double totalImpact = marketInfluences.stream()
            .mapToDouble(MarketInfluence::calculateImpact)
            .sum();
        
        return Math.max(-1.0, Math.min(1.0, totalImpact / marketInfluences.size()));
    }
    
    /**
     * Gets market statistics.
     */
    public MarketStats getMarketStats() {
        if (stocks.isEmpty()) {
            return new MarketStats(0, 0.0, 0.0, 0.0, 0.0, 0, 0.0);
        }
        
        double avgPrice = stocks.values().stream()
            .mapToDouble(Stock::getCurrentPrice)
            .average().orElse(0.0);
            
        double avgChange = stocks.values().stream()
            .mapToDouble(Stock::getPriceChangePercent)
            .average().orElse(0.0);
            
        double totalVolume = stocks.values().stream()
            .mapToDouble(Stock::getDailyVolume)
            .sum();
            
        double totalMarketCap = stocks.values().stream()
            .mapToDouble(Stock::getMarketCap)
            .sum();
            
        long gainers = stocks.values().stream()
            .mapToLong(stock -> stock.isGaining() ? 1 : 0)
            .sum();
        
        return new MarketStats(stocks.size(), avgPrice, avgChange, totalVolume, 
                              totalMarketCap, gainers, getMarketSentiment());
    }
    
    /**
     * Opens or closes the market.
     */
    public void setMarketOpen(boolean open) {
        this.marketOpen = open;
    }
    
    public boolean isMarketOpen() {
        return marketOpen;
    }
    
    /**
     * Market statistics data class.
     */
    public static class MarketStats {
        private final int totalStocks;
        private final double averagePrice;
        private final double averageChange;
        private final double totalVolume;
        private final double totalMarketCap;
        private final long gainers;
        private final double marketSentiment;
        
        public MarketStats(int totalStocks, double averagePrice, double averageChange, 
                          double totalVolume, double totalMarketCap, long gainers, 
                          double marketSentiment) {
            this.totalStocks = totalStocks;
            this.averagePrice = averagePrice;
            this.averageChange = averageChange;
            this.totalVolume = totalVolume;
            this.totalMarketCap = totalMarketCap;
            this.gainers = gainers;
            this.marketSentiment = marketSentiment;
        }
        
        // Getters
        public int getTotalStocks() { return totalStocks; }
        public double getAveragePrice() { return averagePrice; }
        public double getAverageChange() { return averageChange; }
        public double getTotalVolume() { return totalVolume; }
        public double getTotalMarketCap() { return totalMarketCap; }
        public long getGainers() { return gainers; }
        public long getLosers() { return totalStocks - gainers; }
        public double getMarketSentiment() { return marketSentiment; }
        
        @Override
        public String toString() {
            return String.format("MarketStats{stocks: %d, avgChange: %.2f%%, sentiment: %.2f}", 
                totalStocks, averageChange * 100, marketSentiment);
        }
    }
    
    /**
     * Gets the price threshold controller.
     */
    public PriceThresholdController getThresholdController() {
        return thresholdController;
    }
}