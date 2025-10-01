package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.commands.*;
import com.example.quickstocks.core.algorithms.PriceThresholdController;
import com.example.quickstocks.core.services.*;
import com.example.quickstocks.infrastructure.config.CompanyConfig;
import com.example.quickstocks.infrastructure.db.ConfigLoader;
import com.example.quickstocks.infrastructure.db.DatabaseConfig;
import com.example.quickstocks.infrastructure.db.DatabaseManager;
import com.example.quickstocks.listeners.CompanySettingsGUIListener;
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
    private WatchlistService watchlistService;
    private AuditService auditService;
    private CompanyService companyService;
    private InvitationService invitationService;
    private CompanyMarketService companyMarketService;
    private BukkitRunnable marketUpdateTask;

    @Override
    public void onEnable() {
        getLogger().info("QuickStocks enabling (Paper 1.21.8)...");
        
        try {
            // Initialize database
            initializeDatabase();
            
            // Initialize translation manager
            translationManager = new TranslationManager(this);
            
            // Load configuration for threshold controller
            DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
            PriceThresholdController thresholdController = new PriceThresholdController(config);
            
            // Initialize the stock market service with threshold controller
            stockMarketService = new StockMarketService(thresholdController);
            
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
            
            // Initialize watchlist service
            watchlistService = new WatchlistService(databaseManager);

            // Initialize audit service
            auditService = new AuditService(databaseManager.getDb(), holdingsService);

            // Initialize company services
            CompanyConfig companyConfig = new CompanyConfig(); // TODO: Load from config
            companyService = new CompanyService(databaseManager.getDb(), walletService, companyConfig);
            invitationService = new InvitationService(databaseManager.getDb(), companyService);
            companyMarketService = new CompanyMarketService(databaseManager.getDb(), companyService, walletService, companyConfig);

            // Connect trading service to market service for threshold tracking
            tradingService.setStockMarketService(stockMarketService);
            
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
        // Load configuration from config.yml (with fallback to defaults)
        DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
        
        // Override with plugin-specific paths for SQLite
        if ("sqlite".equals(config.getProvider())) {
            config.setSqliteFile(getDataFolder().getAbsolutePath() + "/data.db");
        }
        
        // Create the data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize database with seeding enabled
        databaseManager = new DatabaseManager(config, false);
        databaseManager.initialize();
        
        getLogger().info("Database initialized: " + config.getSqliteFile());
        getLogger().info("Price threshold enabled: " + config.isPriceThresholdEnabled());
    }
    
    /**
     * Registers commands with the server.
     */
    private void registerCommands() {
        StocksCommand stocksCommand = new StocksCommand(queryService, auditService);
        CryptoCommand cryptoCommand = new CryptoCommand(cryptoService);
        WalletCommand walletCommand = new WalletCommand(walletService);
        MarketCommand marketCommand = new MarketCommand(queryService, tradingService, holdingsService, walletService, watchlistService, companyService);
        MarketDeviceCommand marketDeviceCommand = new MarketDeviceCommand(this, translationManager);
        WatchCommand watchCommand = new WatchCommand(watchlistService, queryService);
        CompanyCommand companyCommand = new CompanyCommand(companyService, invitationService, companyMarketService);
        
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
        
        // Register the /watch command
        getCommand("watch").setExecutor(watchCommand);
        getCommand("watch").setTabCompleter(watchCommand);
        
        // Register the /company command
        getCommand("company").setExecutor(companyCommand);
        getCommand("company").setTabCompleter(companyCommand);
        
        getLogger().info("Registered /stocks, /crypto, /wallet, /market, /marketdevice, /watch, and /company commands");
    }
    
    /**
     * Registers event listeners with the server.
     */
    private void registerListeners() {
        MarketDeviceCommand marketDeviceCommand = new MarketDeviceCommand(this, translationManager);
        MarketDeviceListener deviceListener = new MarketDeviceListener(this, translationManager, marketDeviceCommand);
        CraftingListener craftingListener = new CraftingListener(this, recipeManager);
        
        // Register GUI listeners for the new market interface
        MarketGUIListener marketGUIListener = new MarketGUIListener(queryService, tradingService, holdingsService, walletService, companyService);
        PortfolioGUIListener portfolioGUIListener = new PortfolioGUIListener(queryService, tradingService, holdingsService, walletService, companyService);
        CompanySettingsGUIListener companySettingsGUIListener = new CompanySettingsGUIListener();
        
        getServer().getPluginManager().registerEvents(deviceListener, this);
        getServer().getPluginManager().registerEvents(craftingListener, this);
        getServer().getPluginManager().registerEvents(marketGUIListener, this);
        getServer().getPluginManager().registerEvents(portfolioGUIListener, this);
        getServer().getPluginManager().registerEvents(companySettingsGUIListener, this);
        
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

