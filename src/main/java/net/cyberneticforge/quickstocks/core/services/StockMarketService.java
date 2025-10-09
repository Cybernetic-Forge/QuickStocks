package net.cyberneticforge.quickstocks.core.services;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.core.algorithms.PriceThresholdController;
import net.cyberneticforge.quickstocks.core.algorithms.StockPriceCalculator;
import net.cyberneticforge.quickstocks.core.enums.MarketFactor;
import net.cyberneticforge.quickstocks.core.model.MarketInfluence;
import net.cyberneticforge.quickstocks.core.model.Stock;

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
    /**
     * -- GETTER --
     *  Gets the price threshold controller.
     */
    @Getter
    private final PriceThresholdController thresholdController;
    /**
     * -- SETTER --
     *  Opens or closes the market.
     */
    @Setter
    @Getter
    private volatile boolean marketOpen;
    
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
        return switch (sector.toLowerCase()) {
            case "technology" -> 0.8;
            case "biotech" -> 0.9;
            case "crypto" -> 1.0;
            case "energy" -> 0.7;
            case "finance" -> 0.6;
            case "utilities" -> 0.3;
            case "consumer" -> 0.4;
            default -> 0.5;
        };
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
    @SuppressWarnings("unused")
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
            .sorted(Comparator.comparingDouble(Stock::getPriceChangePercent))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    /**
     * Applies a major market event that affects specific influences.
     */
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
         * Market statistics data class.
         */
        @SuppressWarnings("unused")
        public record MarketStats(int totalStocks, double averagePrice, double averageChange, double totalVolume,
                                  double totalMarketCap, long gainers, double marketSentiment) {
        @SuppressWarnings("unused")
        public long getLosers() {
            return totalStocks - gainers;
        }

        @Override
            public String toString() {
                return String.format("MarketStats{stocks: %d, avgChange: %.2f%%, sentiment: %.2f}",
                        totalStocks, averageChange * 100, marketSentiment);
            }
        }

}