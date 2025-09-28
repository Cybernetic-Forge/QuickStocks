package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.commands.CryptoCommand;
import com.example.quickstocks.commands.StocksCommand;
import com.example.quickstocks.core.services.CryptoService;
import com.example.quickstocks.core.services.SimulationEngine;
import com.example.quickstocks.core.services.StockMarketService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Map;

public final class QuickStocksPlugin extends JavaPlugin {
    
    private StockMarketService stockMarketService;
    private SimulationEngine simulationEngine;
    private DatabaseManager databaseManager;
    private QueryService queryService;
    private CryptoService cryptoService;
    private BukkitRunnable marketUpdateTask;

    @Override
    public void onEnable() {
        getLogger().info(I18n.tr("plugin.enabling"));
        
        try {
            // Initialize I18n system first
            I18n.initialize(this);
            
            // Initialize database
            initializeDatabase();
            
            // Initialize the stock market service
            stockMarketService = new StockMarketService();
            
            // Initialize simulation engine
            simulationEngine = new SimulationEngine(stockMarketService, databaseManager.getDb());
            
            // Initialize query service
            queryService = new QueryService(databaseManager.getDb());
            
            // Initialize crypto service
            cryptoService = new CryptoService(databaseManager.getDb());
            
            // Add some default stocks for demonstration
            initializeDefaultStocks();
            
            // Register commands
            registerCommands();
            
            // Start the simulation engine
            simulationEngine.start();
            
            getLogger().info(I18n.tr("plugin.enabled"));
            
        } catch (Exception e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            getLogger().severe(I18n.tr("plugin.enable_failed", placeholders));
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(I18n.tr("plugin.disabling"));
        
        // Stop the simulation engine
        if (simulationEngine != null) {
            simulationEngine.stop();
        }
        
        // Stop the market update task
        if (marketUpdateTask != null && !marketUpdateTask.isCancelled()) {
            marketUpdateTask.cancel();
        }
        
        // Close the market
        if (stockMarketService != null) {
            stockMarketService.setMarketOpen(false);
        }
        
        // Shutdown database
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        getLogger().info("QuickStocks disabled");
    }
    
    /**
     * Initializes the database system.
     */
    private void initializeDatabase() throws SQLException {
        // Create database configuration for the plugin data folder
        DatabaseConfig config = new DatabaseConfig();
        config.setProvider("sqlite");
        config.setSqliteFile(getDataFolder().getAbsolutePath() + "/data.db");
        
        // Create the data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize database with seeding enabled
        databaseManager = new DatabaseManager(config, true);
        databaseManager.initialize();
        
        getLogger().info("Database initialized: " + config.getSqliteFile());
    }
    
    /**
     * Registers commands with the server.
     */
    private void registerCommands() {
        StocksCommand stocksCommand = new StocksCommand(queryService);
        CryptoCommand cryptoCommand = new CryptoCommand(cryptoService);
        
        // Register the /stocks command
        getCommand("stocks").setExecutor(stocksCommand);
        getCommand("stocks").setTabCompleter(stocksCommand);
        
        // Register the /crypto command
        getCommand("crypto").setExecutor(cryptoCommand);
        getCommand("crypto").setTabCompleter(cryptoCommand);
        
        getLogger().info("Registered /stocks and /crypto commands");
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
            
            int stockCount = stockMarketService.getAllStocks().size();
            Map<String, Object> placeholders = Map.of("count", stockCount);
            getLogger().info(I18n.tr("plugin.initialized_stocks", placeholders));
        } catch (Exception e) {
            Map<String, Object> placeholders = Map.of("error", e.getMessage());
            getLogger().severe(I18n.tr("plugin.failed_initialize_stocks", placeholders));
        }
    }
    
    /**
     * Gets the stock market service instance.
     */
    public StockMarketService getStockMarketService() {
        return stockMarketService;
    }
    
    /**
     * Gets the query service instance.
     */
    public QueryService getQueryService() {
        return queryService;
    }
    
    /**
     * Gets the crypto service instance.
     */
    public CryptoService getCryptoService() {
        return cryptoService;
    }
}

