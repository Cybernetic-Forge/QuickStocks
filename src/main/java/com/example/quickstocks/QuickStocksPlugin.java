package com.example.quickstocks;

import com.example.quickstocks.core.services.StockMarketService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class QuickStocksPlugin extends JavaPlugin {
    
    private StockMarketService stockMarketService;
    private BukkitRunnable marketUpdateTask;

    @Override
    public void onEnable() {
        getLogger().info("QuickStocks enabling (Paper 1.21.8)...");
        
        // Initialize the stock market service
        stockMarketService = new StockMarketService();
        
        // Add some default stocks for demonstration
        initializeDefaultStocks();
        
        // Start the market update task (every 5 seconds = 100 ticks)
        startMarketUpdateTask();
        
        getLogger().info("QuickStocks enabled successfully! Market is now running.");
    }

    @Override
    public void onDisable() {
        getLogger().info("QuickStocks disabling...");
        
        // Stop the market update task
        if (marketUpdateTask != null && !marketUpdateTask.isCancelled()) {
            marketUpdateTask.cancel();
        }
        
        // Close the market
        if (stockMarketService != null) {
            stockMarketService.setMarketOpen(false);
        }
        
        getLogger().info("QuickStocks disabled");
    }
    
    /**
     * Initializes some default stocks for testing and demonstration.
     */
    private void initializeDefaultStocks() {
        try {
            stockMarketService.addStock("MINE", "MineCorp Industries", "Technology", 100.00);
            stockMarketService.addStock("CRAFT", "CraftBank Ltd.", "Finance", 75.50);
            stockMarketService.addStock("BLOCK", "BlockChain Energy", "Energy", 120.25);
            stockMarketService.addStock("PIXEL", "Pixel Healthcare", "Healthcare", 85.75);
            
            getLogger().info("Initialized " + stockMarketService.getAllStocks().size() + " default stocks");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize default stocks: " + e.getMessage());
        }
    }
    
    /**
     * Starts the periodic market update task.
     */
    private void startMarketUpdateTask() {
        marketUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    stockMarketService.updateAllStockPrices();
                    
                    // Log market statistics periodically (every minute)
                    if (getServer().getCurrentTick() % 1200 == 0) {
                        var stats = stockMarketService.getMarketStats();
                        getLogger().info(String.format("Market Update - Avg Change: %.2f%%, Sentiment: %.2f", 
                            stats.getAverageChange() * 100, stats.getMarketSentiment()));
                    }
                } catch (Exception e) {
                    getLogger().warning("Error during market update: " + e.getMessage());
                }
            }
        };
        
        // Start immediately and repeat every 5 seconds (100 ticks)
        marketUpdateTask.runTaskTimer(this, 0L, 100L);
        getLogger().info("Market update task started (updates every 5 seconds)");
    }
    
    /**
     * Gets the stock market service instance.
     */
    public StockMarketService getStockMarketService() {
        return stockMarketService;
    }
}

