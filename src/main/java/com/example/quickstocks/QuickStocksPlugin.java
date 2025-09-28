package com.example.quickstocks;

import com.example.quickstocks.application.queries.QueryService;
import com.example.quickstocks.commands.CryptoCommand;
import com.example.quickstocks.commands.MarketCommand;
import com.example.quickstocks.commands.MarketDeviceCommand;
import com.example.quickstocks.commands.StocksCommand;
import com.example.quickstocks.commands.WalletCommand;
import com.example.quickstocks.core.services.AuditService;
import com.example.quickstocks.core.services.BackupService;
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
    private AuditService auditService;
    private BackupService backupService;
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
            
            // Initialize audit service
            auditService = new AuditService(databaseManager.getDb(), holdingsService);
            
            // Initialize backup service
            String dataPath = getDataFolder().getAbsolutePath();
            boolean backupEnabled = getConfig().getBoolean("backup.enabled", true);
            backupService = new BackupService(databaseManager.getDb(), dataPath, backupEnabled);
            
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
        
        // Perform emergency backup before shutdown
        if (backupService != null) {
            getLogger().info("Performing emergency backup...");
            BackupService.BackupResult backupResult = backupService.performEmergencyBackup();
            if (backupResult.success) {
                getLogger().info("Emergency backup completed: " + backupResult.fileCount + " files, " + backupResult.totalSize + " bytes");
            } else {
                getLogger().warning("Emergency backup failed: " + backupResult.message);
            }
        }
        
        // Stop the simulation engine and wait for current operations to complete
        if (simulationEngine != null) {
            getLogger().info("Stopping simulation engine...");
            simulationEngine.stop();
            
            // Give simulation engine time to finish current operations
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Stop the market update task
        if (marketUpdateTask != null && !marketUpdateTask.isCancelled()) {
            getLogger().info("Cancelling market update task...");
            marketUpdateTask.cancel();
        }
        
        // Close the market to prevent new trades
        if (stockMarketService != null) {
            getLogger().info("Closing market...");
            stockMarketService.setMarketOpen(false);
        }
        
        // Remove recipes
        if (recipeManager != null) {
            recipeManager.removeRecipes();
        }
        
        // Log final statistics
        try {
            if (queryService != null) {
                int totalOrders = queryService.getTotalOrderCount();
                int totalPlayers = queryService.getTotalPlayerCount();
                getLogger().info(String.format("Final statistics: %d total orders, %d total players", totalOrders, totalPlayers));
            }
        } catch (Exception e) {
            getLogger().warning("Failed to log final statistics: " + e.getMessage());
        }
        
        // Shutdown database with graceful connection closing
        if (databaseManager != null) {
            getLogger().info("Shutting down database...");
            databaseManager.shutdown();
        }
        
        getLogger().info("QuickStocks disabled gracefully");
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
        StocksCommand stocksCommand = new StocksCommand(queryService, auditService);
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

