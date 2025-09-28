package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.commands.CryptoCommand;
import com.example.quickstocks.commands.MarketCommand;
import com.example.quickstocks.commands.MarketDeviceCommand;
import com.example.quickstocks.commands.StocksCommand;
import com.example.quickstocks.commands.WalletCommand;
import com.example.quickstocks.core.services.CryptoService;
import com.example.quickstocks.core.services.HoldingsService;
import com.example.quickstocks.core.services.SimulationEngine;
import com.example.quickstocks.core.services.StockMarketService;
import com.example.quickstocks.core.services.TradingService;
import com.example.quickstocks.core.services.WalletService;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.listeners.CraftingListener;
import com.example.quickstocks.listeners.MarketDeviceListener;
import com.example.quickstocks.listeners.MarketGUIListener;
import com.example.quickstocks.listeners.PortfolioGUIListener;
import com.example.quickstocks.utils.RecipeManager;
import com.example.quickstocks.utils.TranslationManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;

public final class QuickStocksPlugin extends JavaPlugin {
    
    private StockMarketService stockMarketService;
    private SimulationEngine simulationEngine;
    private DatabaseManager databaseManager;
    private QueryService queryService;
    private CryptoService cryptoService;
    private TranslationManager translationManager;
    private RecipeManager recipeManager;
    private WalletService walletService;
    private HoldingsService holdingsService;
    private TradingService tradingService;
    private BukkitRunnable marketUpdateTask;

    @Override
    public void onEnable() {
        getLogger().info("QuickStocks enabling (Paper 1.21.8)...");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Initialize translation manager
            translationManager = new TranslationManager(this);
            
            // Initialize the stock market service
            stockMarketService = new StockMarketService();
            
            // Initialize simulation engine
            simulationEngine = new SimulationEngine(stockMarketService, databaseManager.getDb());
            
            // Initialize query service
            queryService = new QueryService(databaseManager.getDb());
            
            // Initialize crypto service
            cryptoService = new CryptoService(databaseManager.getDb());
            
            // Initialize wallet service
            walletService = new WalletService(databaseManager.getDb());
            
            // Initialize holdings service
            holdingsService = new HoldingsService(databaseManager.getDb());
            
            // Initialize trading service
            tradingService = new TradingService(databaseManager.getDb(), walletService, holdingsService);
            
            // Add some default stocks for demonstration
            initializeDefaultStocks();
            
            // Register commands
            registerCommands();
            
            // Initialize recipe manager
            MarketDeviceCommand marketDeviceCommand = new MarketDeviceCommand(this, translationManager);
            recipeManager = new RecipeManager(this, marketDeviceCommand, translationManager);
            
            // Register listeners
            registerListeners();
            
            // Register recipes
            registerRecipes();
            
            // Start the simulation engine
            simulationEngine.start();
            
            getLogger().info("QuickStocks enabled successfully! Market is now running.");
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable QuickStocks: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("QuickStocks disabling...");
        
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
        
        // Remove recipes
        if (recipeManager != null) {
            recipeManager.removeRecipes();
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
        WalletCommand walletCommand = new WalletCommand(walletService);
        MarketCommand marketCommand = new MarketCommand(queryService, tradingService, holdingsService, walletService);
        MarketDeviceCommand marketDeviceCommand = new MarketDeviceCommand(this, translationManager);
        
        // Register the /stocks command
        getCommand("stocks").setExecutor(stocksCommand);
        getCommand("stocks").setTabCompleter(stocksCommand);
        
        // Register the /crypto command
        getCommand("crypto").setExecutor(cryptoCommand);
        getCommand("crypto").setTabCompleter(cryptoCommand);
        
        // Register the /wallet command
        getCommand("wallet").setExecutor(walletCommand);
        getCommand("wallet").setTabCompleter(walletCommand);
        
        // Register the /market command
        getCommand("market").setExecutor(marketCommand);
        getCommand("market").setTabCompleter(marketCommand);
        
        // Register the /marketdevice command
        getCommand("marketdevice").setExecutor(marketDeviceCommand);
        getCommand("marketdevice").setTabCompleter(marketDeviceCommand);
        
        getLogger().info("Registered /stocks, /crypto, /wallet, /market, and /marketdevice commands");
    }
    
    /**
     * Registers event listeners with the server.
     */
    private void registerListeners() {
        MarketDeviceCommand marketDeviceCommand = new MarketDeviceCommand(this, translationManager);
        MarketDeviceListener deviceListener = new MarketDeviceListener(this, translationManager, marketDeviceCommand);
        CraftingListener craftingListener = new CraftingListener(this, recipeManager);
        
        // Register GUI listeners for the new market interface
        MarketGUIListener marketGUIListener = new MarketGUIListener(queryService, tradingService, holdingsService, walletService);
        PortfolioGUIListener portfolioGUIListener = new PortfolioGUIListener(queryService, tradingService, holdingsService, walletService);
        
        getServer().getPluginManager().registerEvents(deviceListener, this);
        getServer().getPluginManager().registerEvents(craftingListener, this);
        getServer().getPluginManager().registerEvents(marketGUIListener, this);
        getServer().getPluginManager().registerEvents(portfolioGUIListener, this);
        
        getLogger().info("Registered Market Device, Crafting, and GUI event listeners");
    }
    
    /**
     * Registers crafting recipes if enabled in config.
     */
    private void registerRecipes() {
        if (recipeManager != null) {
            recipeManager.registerRecipes();
        }
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

