package net.cyberneticforge.quickstocks.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a stock in the market with price history and characteristics.
 */
public class Stock {
    private final String symbol;
    private final String name;
    private final String sector;
    private double currentPrice;
    private double previousPrice;
    private final List<PriceHistory> priceHistory;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private double dailyVolume;
    private double marketCap;
    private double volatilityRating; // 0.0 to 1.0, higher = more volatile
    
    public Stock(String symbol, String name, String sector, double initialPrice) {
        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null"); 
        this.sector = Objects.requireNonNull(sector, "Sector cannot be null");
        this.currentPrice = initialPrice;
        this.previousPrice = initialPrice;
        this.priceHistory = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.dailyVolume = 0.0;
        this.marketCap = initialPrice * 1000000; // Default 1M shares
        this.volatilityRating = 0.5; // Medium volatility by default
        
        // Add initial price to history
        addPriceToHistory(initialPrice);
    }
    
    /**
     * Updates the stock price and maintains history.
     */
    public void updatePrice(double newPrice) {
        if (newPrice <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        this.previousPrice = this.currentPrice;
        this.currentPrice = newPrice;
        this.lastUpdated = LocalDateTime.now();
        addPriceToHistory(newPrice);
        
        // Update market cap based on new price
        this.marketCap = newPrice * getSharesOutstanding();
    }
    
    private void addPriceToHistory(double price) {
        priceHistory.add(new PriceHistory(price, LocalDateTime.now()));
        
        // Keep only last 1000 entries to prevent memory issues
        if (priceHistory.size() > 1000) {
            priceHistory.remove(0);
        }
    }
    
    /**
     * @return Price change as a percentage (-1.0 to 1.0+)
     */
    public double getPriceChangePercent() {
        if (previousPrice == 0) return 0.0;
        return (currentPrice - previousPrice) / previousPrice;
    }
    
    /**
     * @return Absolute price change
     */
    public double getPriceChange() {
        return currentPrice - previousPrice;
    }
    
    /**
     * @return True if stock price increased
     */
    public boolean isGaining() {
        return currentPrice > previousPrice;
    }
    
    /**
     * @return Number of shares outstanding (calculated from market cap)
     */
    public long getSharesOutstanding() {
        return Math.round(marketCap / currentPrice);
    }
    
    /**
     * Updates the daily trading volume
     */
    public void updateVolume(double volume) {
        this.dailyVolume = Math.max(0, volume);
    }
    
    // Getters
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public double getCurrentPrice() { return currentPrice; }
    public double getPreviousPrice() { return previousPrice; }
    public List<PriceHistory> getPriceHistory() { return new ArrayList<>(priceHistory); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public double getDailyVolume() { return dailyVolume; }
    public double getMarketCap() { return marketCap; }
    public double getVolatilityRating() { return volatilityRating; }
    
    public void setVolatilityRating(double rating) {
        this.volatilityRating = Math.max(0.0, Math.min(1.0, rating));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return Objects.equals(symbol, stock.symbol);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }
    
    @Override
    public String toString() {
        return String.format("Stock{%s: $%.2f (%.2f%%)}", 
            symbol, currentPrice, getPriceChangePercent() * 100);
    }
    
    /**
     * Inner class to track price history with timestamps
     */
    public static class PriceHistory {
        private final double price;
        private final LocalDateTime timestamp;
        
        public PriceHistory(double price, LocalDateTime timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
        
        public double getPrice() { return price; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}