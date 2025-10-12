package net.cyberneticforge.quickstocks;

import lombok.Getter;
import net.cyberneticforge.quickstocks.commands.*;
import net.cyberneticforge.quickstocks.core.algorithms.PriceThresholdController;
import net.cyberneticforge.quickstocks.core.services.*;
import net.cyberneticforge.quickstocks.hooks.ChestShopAccountProvider;
import net.cyberneticforge.quickstocks.hooks.ChestShopHook;
import net.cyberneticforge.quickstocks.hooks.HookManager;
import net.cyberneticforge.quickstocks.infrastructure.config.*;
import net.cyberneticforge.quickstocks.infrastructure.db.ConfigLoader;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseManager;
import net.cyberneticforge.quickstocks.listeners.CompanySettingsGUIListener;
import net.cyberneticforge.quickstocks.listeners.MarketDeviceListener;
import net.cyberneticforge.quickstocks.listeners.MarketGUIListener;
import net.cyberneticforge.quickstocks.listeners.PortfolioGUIListener;
import net.cyberneticforge.quickstocks.listeners.shops.ChestShopListener;
import net.cyberneticforge.quickstocks.listeners.shops.ChestShopProtectionListener;
import net.cyberneticforge.quickstocks.listeners.shops.ChestShopTransactionListener;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class QuickStocksPlugin extends JavaPlugin {

    @Getter
    private static JavaPlugin instance;
    @Getter
    private static TranslationService translationService;
    @Getter
    private static StockMarketService stockMarketService;
    @Getter
    private static SimulationEngine simulationEngine;
    @Getter
    private static DatabaseManager databaseManager;
    @Getter
    private static QueryService queryService;
    @Getter
    private static CryptoService cryptoService;
    @Getter
    private static GuiConfig guiConfig;
    @Getter
    private static WalletService walletService;
    @Getter
    private static HoldingsService holdingsService;
    @Getter
    private static TradingService tradingService;
    @Getter
    private static WatchlistService watchlistService;
    @Getter
    private static AuditService auditService;
    @Getter
    private static CompanyService companyService;
    @Getter
    private static InvitationService invitationService;
    @Getter
    private static CompanyMarketService companyMarketService;
    @Getter
    private static SalaryService salaryService;
    @Getter
    private static BukkitRunnable marketUpdateTask;
    @Getter
    private static InstrumentPersistenceService instrumentPersistenceService;
    @Getter
    private static AnalyticsService analyticsService;
    @Getter
    private static HookManager hookManager;
    @Getter
    private static MetricsService metricsService;
    @Getter
    private static MarketCfg marketCfg;
    @Getter
    private static TradingCfg tradingCfg;
    @Getter
    private static CompanyCfg companyCfg;

    @Override
    public void onEnable() {
        getLogger().info("QuickStocks enabling (Paper 1.21.8)...");
        instance = this;
        try {
            // Save default config if it doesn't exist
            saveDefaultConfig();
            
            // Initialize hook manager to detect external plugins
            hookManager = new HookManager();

            // Initialize translation service
            translationService = new TranslationService();

            // Initialize database
            initializeDatabase();
            
            // Initialize configuration managers
            guiConfig = new GuiConfig();
            marketCfg = new MarketCfg();
            tradingCfg = new TradingCfg();
            companyCfg = new CompanyCfg();
            
            // Load configuration for threshold controller
            DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
            PriceThresholdController thresholdController = new PriceThresholdController(config);
            
            // Initialize the stock market service with threshold controller
            stockMarketService = new StockMarketService(thresholdController);
            
            // Initialize analytics service using values from market config
            analyticsService = new AnalyticsService(
                databaseManager.getDb(), 
                marketCfg.getAnalyticsLambda(), 
                60, 
                marketCfg.getAnalyticsChangeWindow(), 
                marketCfg.getAnalyticsVolatilityWindow()
            );
            
            // Initialize simulation engine
            simulationEngine = new SimulationEngine(databaseManager.getDb(), analyticsService);
            
            // Initialize query service
            queryService = new QueryService(databaseManager.getDb());
            
            // Initialize crypto service
            cryptoService = new CryptoService(databaseManager.getDb());
            
            // Initialize wallet service
            walletService = new WalletService(databaseManager.getDb());

            // Initialize company services using company config
            companyService = new CompanyService(databaseManager.getDb(), companyCfg.getCompanyConfig());
            invitationService = new InvitationService(databaseManager.getDb());
            companyMarketService = new CompanyMarketService(databaseManager.getDb(), companyCfg.getCompanyConfig());
            salaryService = new SalaryService(databaseManager.getDb(), companyCfg.getCompanyConfig(), companyService);

            // Initialize holdings service
            holdingsService = new HoldingsService(databaseManager.getDb());
            
            // Initialize trading service
            tradingService = new TradingService(databaseManager.getDb());
            
            // Initialize watchlist service
            watchlistService = new WatchlistService(databaseManager);

            // Initialize audit service
            auditService = new AuditService(databaseManager.getDb());

            // Initialize instrument persistence service
            instrumentPersistenceService = new InstrumentPersistenceService(databaseManager.getDb());

            // Connect trading service to market service for threshold tracking
            tradingService.setStockMarketService(stockMarketService);
            
            // Add some default stocks for demonstration
            initializeDefaultStocks();
            
            // Register commands
            registerCommands();

            // Register listeners
            registerListeners();
            
            // Start the simulation engine
            simulationEngine.start();
            
            // Start salary payment scheduler (check every 5 minutes)
            startSalaryPaymentScheduler();
            // Initialize bStats metrics if enabled
            if (getConfig().getBoolean("metrics.enabled", true)) {
                metricsService = new MetricsService();
                metricsService.initialize();
            }
            
            getLogger().info("QuickStocks enabled successfully! Market is now running.");
            
        } catch (Exception ex) {
            getLogger().severe("Failed to enable QuickStocks: " + ex.getMessage());
            getLogger().severe("StackTrace: " + Arrays.toString(ex.getStackTrace()));
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
        
        // Shutdown metrics
        if (metricsService != null) {
            metricsService.shutdown();
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
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
        registerCommand("crypto", new CryptoCommand(cryptoService));
        registerCommand("wallet", new WalletCommand());
        registerCommand("market", new MarketCommand(databaseManager.getDb()));
        registerCommand("marketdevice", new MarketDeviceCommand());
        registerCommand("watch", new WatchCommand());
        registerCommand("company", new CompanyCommand());
        registerCommand("stocks", new StocksCommand());
    }
    
    /**
     * Registers event listeners with the server.
     */
    private void registerListeners() {
        MarketDeviceListener deviceListener = new MarketDeviceListener();

        // Register GUI listeners for the new market interface
        MarketGUIListener marketGUIListener = new MarketGUIListener();
        PortfolioGUIListener portfolioGUIListener = new PortfolioGUIListener();
        CompanySettingsGUIListener companySettingsGUIListener = new CompanySettingsGUIListener();
        
        getServer().getPluginManager().registerEvents(deviceListener, this);
        getServer().getPluginManager().registerEvents(marketGUIListener, this);
        getServer().getPluginManager().registerEvents(portfolioGUIListener, this);
        getServer().getPluginManager().registerEvents(companySettingsGUIListener, this);
        
        // Register ChestShop integration listeners if ChestShop is hooked
        if (hookManager.isHooked(net.cyberneticforge.quickstocks.hooks.HookType.ChestShop)) {
            CompanyConfig companyConfig = new CompanyConfig(); // TODO: Load from config
            ChestShopHook chestShopHook = new ChestShopHook(companyService);
            
            // Register company names as valid ChestShop accounts
            ChestShopAccountProvider accountProvider = new ChestShopAccountProvider(companyService);
            accountProvider.registerWithChestShop();
            
            ChestShopListener chestShopListener = new ChestShopListener(companyService, companyConfig, accountProvider);
            ChestShopTransactionListener chestShopTransactionListener = 
                new ChestShopTransactionListener(chestShopHook, companyConfig, walletService);
            ChestShopProtectionListener chestShopProtectionListener =
                new ChestShopProtectionListener(chestShopHook, companyConfig);
            
            getServer().getPluginManager().registerEvents(chestShopListener, this);
            getServer().getPluginManager().registerEvents(chestShopTransactionListener, this);
            getServer().getPluginManager().registerEvents(chestShopProtectionListener, this);
            getLogger().info("Registered ChestShop integration listeners and account provider");
        }
        
        getLogger().info("Registered Market Device, Crafting, and GUI event listeners");
    }
    
    /**
     * Initializes some default stocks for testing and demonstration.
     * DEPRECATED: Example stocks have been removed. The system uses real Minecraft items and company shares instead.
     */
    private void initializeDefaultStocks() {
        // Example stocks (MINE, CRAFT, BLOCK, PIXEL) have been removed as per issue requirements.
        // The system now relies on:
        // 1. Minecraft items (seeded via ItemSeeder)
        // 2. Company shares (created via /company market enable)
        getLogger().info("Using real market instruments (Minecraft items and company shares)");
    }
    
    /**
     * Starts a scheduler to process salary payments for all companies.
     * Checks every 5 minutes if any company needs to pay salaries.
     */
    private void startSalaryPaymentScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Get all companies
                    List<Map<String, Object>> companies = databaseManager.getDb().query(
                        "SELECT id, name FROM companies"
                    );
                    
                    int totalPayments = 0;
                    for (Map<String, Object> company : companies) {
                        String companyId = (String) company.get("id");
                        String companyName = (String) company.get("name");
                        
                        try {
                            int payments = salaryService.processSalaryPayments(companyId);
                            if (payments > 0) {
                                totalPayments += payments;
                                getLogger().info("Processed " + payments + " salary payments for company " + companyName);
                            }
                        } catch (Exception e) {
                            getLogger().warning("Failed to process salaries for company " + companyName + ": " + e.getMessage());
                        }
                    }
                    
                    if (totalPayments > 0) {
                        getLogger().info("Total salary payments processed: " + totalPayments);
                    }
                } catch (Exception e) {
                    getLogger().warning("Error in salary payment scheduler: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(this, 20L * 60 * 5, 20L * 60 * 5); // Run every 5 minutes
    }

    public static void registerCommand(String command, CommandExecutor executor) {
        Objects.requireNonNull(instance.getCommand(command)).setExecutor(executor);
    }
}

