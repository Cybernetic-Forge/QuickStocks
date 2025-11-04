package net.cyberneticforge.quickstocks;

import lombok.Getter;
import net.cyberneticforge.quickstocks.api.QuickStocksAPI;
import net.cyberneticforge.quickstocks.commands.*;
import net.cyberneticforge.quickstocks.core.algorithms.PriceThresholdController;
import net.cyberneticforge.quickstocks.core.services.*;
import net.cyberneticforge.quickstocks.hooks.chestshop.ChestShopAccountProvider;
import net.cyberneticforge.quickstocks.hooks.chestshop.ChestShopHook;
import net.cyberneticforge.quickstocks.hooks.HookManager;
import net.cyberneticforge.quickstocks.hooks.HookType;
import net.cyberneticforge.quickstocks.hooks.WorldGuardFlags;
import net.cyberneticforge.quickstocks.hooks.WorldGuardHook;
import net.cyberneticforge.quickstocks.infrastructure.config.CompanyCfg;
import net.cyberneticforge.quickstocks.infrastructure.config.GuiConfig;
import net.cyberneticforge.quickstocks.infrastructure.config.MarketCfg;
import net.cyberneticforge.quickstocks.infrastructure.config.TradingCfg;
import net.cyberneticforge.quickstocks.infrastructure.db.ConfigLoader;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseConfig;
import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseManager;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
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
    private static QuickStocksPlugin instance;

    /* Logging */
    @Getter
    private static PluginLogger pluginLogger;

    /* Configurations */
    @Getter
    private static MarketCfg marketCfg;
    @Getter
    private static TradingCfg tradingCfg;
    @Getter
    private static CompanyCfg companyCfg;

    /* Services */
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
    private static CompanyPlotService companyPlotService;
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
    private static WorldGuardHook worldGuardHook;
    
    // Scheduler task tracking for reload functionality
    private static BukkitRunnable salaryPaymentTask;
    private static BukkitRunnable rentCollectionTask;

    @Override
    public void onLoad() {
        instance = this;
        // Register WG flags BEFORE WG locks its registry
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                WorldGuardFlags.registerFlags();
                getLogger().info("WorldGuard flags registered in onLoad().");
            } catch (IllegalStateException ex) {
                // Safety net in case something still tries to register too late
                getLogger().severe("Could not register WorldGuard flags (registry locked). " +
                        "Ensure load: STARTUP & softdepend: [WorldGuard].");
            }
        } else {
            getLogger().warning("WorldGuard not present during onLoad(); skipping flag registration.");
        }
    }

    @Override
    public void onEnable() {
        getLogger().info("QuickStocks enabling (Paper 1.21.8)...");
        instance = this;
        try {
            // Save default config if it doesn't exist
            saveDefaultConfig();
            
            // Initialize centralized logger
            int debugLevel = getConfig().getInt("logging.debugLevel", 1);
            pluginLogger = new PluginLogger(this, debugLevel);
            pluginLogger.info("PluginLogger initialized with debug level: " + debugLevel);
            
            // Initialize hook manager to detect external plugins
            hookManager = new HookManager();
            
            // Initialize WorldGuard hook if available
            if (hookManager.isHooked(HookType.WorldGuard) && WorldGuardFlags.QUICKSTOCKS_TRADING != null && WorldGuardFlags.QUICKSTOCKS_PLOTS != null) {
                worldGuardHook = new WorldGuardHook();
                getLogger().info("WorldGuard hook initialized successfully.");
            } else {
                getLogger().severe("WorldGuard flags unavailable; disabling QuickStocks to avoid NPEs.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            // Initialize translation service
            translationService = new TranslationService();

            // Initialize database
            initializeDatabase();

            guiConfig = new GuiConfig();
            marketCfg = new MarketCfg();
            tradingCfg = new TradingCfg();
            companyCfg = new CompanyCfg();

            DatabaseConfig config = ConfigLoader.loadDatabaseConfig();
            PriceThresholdController thresholdController = new PriceThresholdController(config);

            stockMarketService = new StockMarketService(thresholdController);
            analyticsService = new AnalyticsService();
            simulationEngine = new SimulationEngine();
            queryService = new QueryService();
            cryptoService = new CryptoService();
            walletService = new WalletService();
            companyService = new CompanyService();
            invitationService = new InvitationService();
            companyMarketService = new CompanyMarketService();
            salaryService = new SalaryService();
            companyPlotService = new CompanyPlotService();
            holdingsService = new HoldingsService();
            tradingService = new TradingService();
            watchlistService = new WatchlistService();
            auditService = new AuditService();
            instrumentPersistenceService = new InstrumentPersistenceService();
            tradingService.setStockMarketService(new StockMarketService());

            initializeDefaultStocks();
            registerCommands();
            registerListeners();
            simulationEngine.start();

            startSalaryPaymentScheduler();
            startRentCollectionScheduler();

            if (getConfig().getBoolean("metrics.enabled", true)) {
                metricsService = new MetricsService();
                metricsService.initialize();
            }

            QuickStocksAPI.initialize(companyService, companyMarketService, tradingService, holdingsService, stockMarketService, instrumentPersistenceService, walletService, watchlistService, cryptoService);
            
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
        // Register admin commands (always available)
        registerCommand("quickstocks", new QuickStocksCommand());
        registerCommand("wallet", new WalletCommand());
        
        // Only register market-related commands if market system is enabled
        if (marketCfg.isEnabled()) {
            registerCommand("market", new MarketCommand(databaseManager.getDb()));
            if (marketCfg.isMarketDeviceEnabled()) {
                registerCommand("marketdevice", new MarketDeviceCommand());
            }
            if (marketCfg.isWatchlistEnabled()) {
                registerCommand("watch", new WatchCommand());
            }
            if (marketCfg.isStocksCommandEnabled()) {
                registerCommand("stocks", new StocksCommand());
            }
            if (marketCfg.isCryptoCommandEnabled()) {
                registerCommand("crypto", new CryptoCommand(cryptoService));
            }
        }
        
        // Only register company command if companies system is enabled
        if (companyCfg.isEnabled()) {
            registerCommand("company", new CompanyCommand());
        }
    }
    
    /**
     * Registers event listeners with the server.
     */
    private void registerListeners() {
        // Only register market-related listeners if market system is enabled
        if (marketCfg.isEnabled()) {
            if (marketCfg.isMarketDeviceEnabled()) {
                getServer().getPluginManager().registerEvents(new MarketDeviceListener(), this);
            }
            if (marketCfg.isPortfolioEnabled() || marketCfg.isTradingEnabled()) {
                getServer().getPluginManager().registerEvents(new MarketGUIListener(), this);
                getServer().getPluginManager().registerEvents(new PortfolioGUIListener(), this);
            }
            getLogger().info("Registered market-related event listeners");
        }
        
        // Only register company-related listeners if companies system is enabled
        if (companyCfg.isEnabled()) {
            getServer().getPluginManager().registerEvents(new CompanySettingsGUIListener(), this);
            
            // Register plot listener if plots are enabled
            if (companyCfg.isPlotsEnabled()) {
                getServer().getPluginManager().registerEvents(new net.cyberneticforge.quickstocks.listeners.CompanyPlotListener(), this);
                getServer().getPluginManager().registerEvents(new net.cyberneticforge.quickstocks.listeners.PlotEditGUIListener(), this);
                getServer().getPluginManager().registerEvents(new net.cyberneticforge.quickstocks.listeners.PlotPermissionEditGUIListener(), this);
                getLogger().info("Registered company plot listeners");
            }
            
            // Register ChestShop integration listeners if ChestShop is hooked and chestshop is enabled
            if (companyCfg.isChestShopEnabled() && hookManager.isHooked(net.cyberneticforge.quickstocks.hooks.HookType.ChestShop)) {
                ChestShopHook chestShopHook = new ChestShopHook(companyService);
                // Register company names as valid ChestShop accounts
                ChestShopAccountProvider accountProvider = new ChestShopAccountProvider(companyService);
                accountProvider.registerWithChestShop();
                getServer().getPluginManager().registerEvents(new ChestShopListener(accountProvider), this);
                getServer().getPluginManager().registerEvents(new ChestShopTransactionListener(chestShopHook), this);
                getServer().getPluginManager().registerEvents(new ChestShopProtectionListener(chestShopHook), this);
                getLogger().info("Registered ChestShop integration listeners and account provider");
            }
            
            getLogger().info("Registered company-related event listeners");
        }
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
     * Package-private for reload functionality.
     */
    void startSalaryPaymentScheduler() {
        // Cancel existing task if running
        if (salaryPaymentTask != null && !salaryPaymentTask.isCancelled()) {
            salaryPaymentTask.cancel();
        }
        
        salaryPaymentTask = new BukkitRunnable() {
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
        };
        salaryPaymentTask.runTaskTimerAsynchronously(this, 20L * 60 * 5, 20L * 60 * 5); // Run every 5 minutes
    }
    
    /**
     * Starts the scheduled task for rent collection.
     * Package-private for reload functionality.
     */
    void startRentCollectionScheduler() {
        // Cancel existing task if running
        if (rentCollectionTask != null && !rentCollectionTask.isCancelled()) {
            rentCollectionTask.cancel();
        }
        
        rentCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (companyCfg.isPlotsEnabled()) {
                        companyPlotService.processRentCollection();
                    }
                } catch (Exception e) {
                    getLogger().warning("Error in rent collection scheduler: " + e.getMessage());
                }
            }
        };
        rentCollectionTask.runTaskTimerAsynchronously(this, 20L * 60 * 10, 20L * 60 * 10); // Run every 10 minutes
    }
    
    /**
     * Reinitializes the plugin logger with a new debug level.
     * Package-private for reload functionality.
     */
    void reinitializeLogger() {
        int debugLevel = getConfig().getInt("logging.debugLevel", 1);
        pluginLogger = new PluginLogger(this, debugLevel);
        pluginLogger.info("PluginLogger reinitialized with debug level: " + debugLevel);
    }
    
    /**
     * Recreates the simulation engine with a fresh executor service.
     * This is necessary because the executor service is terminated on stop and cannot be restarted.
     * Package-private for reload functionality.
     */
    void recreateSimulationEngine() {
        simulationEngine = new SimulationEngine();
        simulationEngine.start();
    }

    public static void registerCommand(String command, CommandExecutor executor) {
        Objects.requireNonNull(instance.getCommand(command)).setExecutor(executor);
    }
}

