package com.example.quickstocks;

import com.example.quickstocks.core.enums.MarketFactor;
import com.example.quickstocks.core.models.MarketInfluence;
import com.example.quickstocks.core.models.Stock;
import com.example.quickstocks.core.services.StockMarketService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Test simulation that runs the stock market with updates every 5 seconds.
 * This demonstrates the realistic stock price algorithm in action.
 */
public class StockMarketSimulationTest {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final StockMarketService marketService;
    private final ScheduledExecutorService scheduler;
    private int updateCount = 0;
    
    public StockMarketSimulationTest() {
        this.marketService = new StockMarketService();
        this.scheduler = Executors.newScheduledThreadPool(1);
        setupTestStocks();
    }
    
    /**
     * Sets up a diverse portfolio of test stocks across different sectors.
     */
    private void setupTestStocks() {
        // Technology stocks (high volatility)
        marketService.addStock("TECH", "TechCorp Inc.", "Technology", 150.00);
        marketService.addStock("INNOV", "InnovateTech Ltd.", "Technology", 85.50);
        
        // Finance stocks (medium volatility)
        marketService.addStock("BANK", "MegaBank Corp", "Finance", 45.25);
        marketService.addStock("INVEST", "Investment Solutions", "Finance", 78.90);
        
        // Energy stocks (medium-high volatility)
        marketService.addStock("OIL", "Energy Corp", "Energy", 120.75);
        marketService.addStock("SOLAR", "Solar Power Inc.", "Energy", 35.20);
        
        // Healthcare stocks (low-medium volatility)
        marketService.addStock("HEALTH", "HealthCare Systems", "Healthcare", 95.40);
        marketService.addStock("PHARMA", "PharmaCorp", "Healthcare", 180.25);
        
        // Utilities (low volatility)
        marketService.addStock("UTIL", "Utility Services", "Utilities", 62.80);
        
        // Consumer goods (low-medium volatility)
        marketService.addStock("GOODS", "Consumer Products", "Consumer", 42.15);
        
        System.out.println("📈 Market initialized with " + marketService.getAllStocks().size() + " stocks");
        printMarketSummary();
    }
    
    /**
     * Starts the simulation with 5-second updates.
     */
    public void startSimulation(int maxUpdates) {
        System.out.println("\n🚀 Starting stock market simulation...");
        System.out.println("⏰ Updates every 5 seconds for " + maxUpdates + " iterations\n");
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateCount++;
                updateMarket();
                
                if (updateCount >= maxUpdates) {
                    stopSimulation();
                }
                
            } catch (Exception e) {
                System.err.println("Error during market update: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    /**
     * Performs a single market update and displays results.
     */
    private void updateMarket() {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        
        // Occasionally apply major market events for testing
        if (updateCount % 10 == 0 && updateCount > 0) {
            applyRandomMarketEvent();
        }
        
        // Update all stock prices
        marketService.updateAllStockPrices();
        
        // Display update information
        System.out.println("🕐 [" + timestamp + "] Update #" + updateCount);
        
        // Show market statistics
        var stats = marketService.getMarketStats();
        System.out.printf("📊 Market: %.2f%% avg change | Sentiment: %.2f | %d↗ %d↘%n",
            stats.getAverageChange() * 100,
            stats.getMarketSentiment(),
            stats.getGainers(),
            stats.getLosers()
        );
        
        // Show top 3 performers
        System.out.println("🏆 Top performers:");
        marketService.getTopPerformers(3).forEach(stock -> 
            System.out.printf("   %s: $%.2f (%+.2f%%)%n", 
                stock.getSymbol(), 
                stock.getCurrentPrice(), 
                stock.getPriceChangePercent() * 100)
        );
        
        // Show worst 3 performers
        System.out.println("📉 Worst performers:");
        marketService.getWorstPerformers(3).forEach(stock -> 
            System.out.printf("   %s: $%.2f (%+.2f%%)%n", 
                stock.getSymbol(), 
                stock.getCurrentPrice(), 
                stock.getPriceChangePercent() * 100)
        );
        
        // Show top market influences
        if (updateCount % 5 == 0) {
            System.out.println("🌍 Current market influences:");
            marketService.getTopInfluences(3).forEach(influence -> 
                System.out.printf("   %s: %.2f (intensity: %.2f)%n",
                    influence.getFactor().name(),
                    influence.getCurrentValue(),
                    influence.getIntensity())
            );
        }
        
        System.out.println();
    }
    
    /**
     * Applies a random market event to test the system's response.
     */
    private void applyRandomMarketEvent() {
        MarketFactor[] factors = {
            MarketFactor.GEOPOLITICAL_EVENTS,
            MarketFactor.EARNINGS_REPORTS,
            MarketFactor.INTEREST_RATES,
            MarketFactor.INVESTOR_CONFIDENCE,
            MarketFactor.FLASH_CRASHES,
            MarketFactor.SOCIAL_MEDIA_BUZZ
        };
        
        MarketFactor randomFactor = factors[(int) (Math.random() * factors.length)];
        double impact = (Math.random() - 0.5) * 1.6; // -0.8 to +0.8
        double intensity = 0.7 + Math.random() * 0.3; // 0.7 to 1.0
        
        marketService.applyMarketEvent(randomFactor, impact, intensity);
        
        System.out.printf("⚡ Market Event: %s (impact: %+.2f, intensity: %.2f)%n", 
            randomFactor.name(), impact, intensity);
    }
    
    /**
     * Prints a summary of all stocks and their current status.
     */
    private void printMarketSummary() {
        System.out.println("\n📋 Initial Stock Listing:");
        System.out.println("Symbol | Name                    | Sector     | Price    | Volatility");
        System.out.println("-------|-------------------------|------------|----------|----------");
        
        marketService.getAllStocks().forEach(stock -> 
            System.out.printf("%-6s | %-23s | %-10s | $%-7.2f | %.1f%n",
                stock.getSymbol(),
                stock.getName(),
                stock.getSector(),
                stock.getCurrentPrice(),
                stock.getVolatilityRating())
        );
    }
    
    /**
     * Stops the simulation and shows final results.
     */
    private void stopSimulation() {
        scheduler.shutdown();
        
        System.out.println("\n🏁 Simulation completed after " + updateCount + " updates");
        System.out.println("📈 Final Market Summary:");
        
        var finalStats = marketService.getMarketStats();
        System.out.printf("Total Market Cap: $%.2f million%n", finalStats.getTotalMarketCap() / 1000000);
        System.out.printf("Average Price Change: %.2f%%%n", finalStats.getAverageChange() * 100);
        System.out.printf("Final Market Sentiment: %.2f%n", finalStats.getMarketSentiment());
        
        // Show final stock prices
        System.out.println("\n📊 Final Stock Prices:");
        marketService.getAllStocks().stream()
            .sorted((a, b) -> a.getSymbol().compareTo(b.getSymbol()))
            .forEach(stock -> {
                double totalChange = (stock.getCurrentPrice() / stock.getPriceHistory().get(0).getPrice() - 1) * 100;
                System.out.printf("%s: $%.2f (total change: %+.2f%%)%n",
                    stock.getSymbol(),
                    stock.getCurrentPrice(),
                    totalChange);
            });
    }
    
    /**
     * Main method to run the simulation.
     */
    public static void main(String[] args) {
        System.out.println("🎯 QuickStocks Market Simulation Test");
        System.out.println("=====================================");
        
        StockMarketSimulationTest simulation = new StockMarketSimulationTest();
        
        // Run for 20 updates (about 100 seconds)
        int maxUpdates = args.length > 0 ? Integer.parseInt(args[0]) : 20;
        simulation.startSimulation(maxUpdates);
        
        // Keep the main thread alive
        try {
            Thread.sleep((maxUpdates + 2) * 5000); // Wait for completion
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}