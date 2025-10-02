package net.cyberneticforge.quickstocks.api;

import net.cyberneticforge.quickstocks.api.managers.*;
import net.cyberneticforge.quickstocks.core.services.*;
import org.bukkit.plugin.Plugin;

/**
 * Main API class for QuickStocks.
 * Provides centralized access to all manager classes for external plugins.
 * 
 * Usage example:
 * <pre>
 * {@code
 * Plugin quickStocksPlugin = Bukkit.getPluginManager().getPlugin("QuickStocks");
 * QuickStocksAPI api = QuickStocksAPI.getInstance(quickStocksPlugin);
 * 
 * // Use managers
 * api.getCompanyManager().createCompany(playerUuid, "TechCorp", "PUBLIC");
 * api.getTradingManager().buy(playerUuid, instrumentId, 100);
 * api.getWalletManager().getBalance(playerUuid);
 * }
 * </pre>
 */
public class QuickStocksAPI {
    
    private static QuickStocksAPI instance;
    
    private final CompanyManager companyManager;
    private final TradingManager tradingManager;
    private final MarketManager marketManager;
    private final WalletManager walletManager;
    private final WatchlistManager watchlistManager;
    private final CryptoManager cryptoManager;
    private final PortfolioManager portfolioManager;
    
    /**
     * Private constructor - use getInstance() to get the API instance.
     */
    private QuickStocksAPI(CompanyService companyService,
                          CompanyMarketService companyMarketService,
                          TradingService tradingService,
                          HoldingsService holdingsService,
                          StockMarketService stockMarketService,
                          InstrumentPersistenceService instrumentService,
                          WalletService walletService,
                          WatchlistService watchlistService,
                          CryptoService cryptoService) {
        
        this.companyManager = new CompanyManager(companyService, companyMarketService);
        this.tradingManager = new TradingManager(tradingService, holdingsService);
        this.marketManager = new MarketManager(stockMarketService, instrumentService);
        this.walletManager = new WalletManager(walletService);
        this.watchlistManager = new WatchlistManager(watchlistService);
        this.cryptoManager = new CryptoManager(cryptoService);
        this.portfolioManager = new PortfolioManager(holdingsService);
    }
    
    /**
     * Initializes the API instance. Should be called by QuickStocks plugin during initialization.
     * This is an internal method and should not be called by external plugins.
     * 
     * @param companyService Company service instance
     * @param companyMarketService Company market service instance
     * @param tradingService Trading service instance
     * @param holdingsService Holdings service instance
     * @param stockMarketService Stock market service instance
     * @param instrumentService Instrument service instance
     * @param walletService Wallet service instance
     * @param watchlistService Watchlist service instance
     * @param cryptoService Crypto service instance
     */
    public static void initialize(CompanyService companyService,
                                 CompanyMarketService companyMarketService,
                                 TradingService tradingService,
                                 HoldingsService holdingsService,
                                 StockMarketService stockMarketService,
                                 InstrumentPersistenceService instrumentService,
                                 WalletService walletService,
                                 WatchlistService watchlistService,
                                 CryptoService cryptoService) {
        
        if (instance != null) {
            throw new IllegalStateException("QuickStocksAPI has already been initialized");
        }
        
        instance = new QuickStocksAPI(
            companyService,
            companyMarketService,
            tradingService,
            holdingsService,
            stockMarketService,
            instrumentService,
            walletService,
            watchlistService,
            cryptoService
        );
    }
    
    /**
     * Gets the QuickStocksAPI instance.
     * External plugins should use this method to access the API.
     * 
     * @return The API instance
     * @throws IllegalStateException if the API has not been initialized
     */
    public static QuickStocksAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("QuickStocksAPI has not been initialized yet. " +
                                          "Ensure QuickStocks plugin is loaded before accessing the API.");
        }
        return instance;
    }
    
    /**
     * Gets the QuickStocksAPI instance using the plugin reference.
     * This is a convenience method for external plugins.
     * 
     * @param plugin The QuickStocks plugin instance
     * @return The API instance
     * @throws IllegalStateException if the API has not been initialized
     */
    public static QuickStocksAPI getInstance(Plugin plugin) {
        if (plugin == null || !plugin.getName().equals("QuickStocks")) {
            throw new IllegalArgumentException("Invalid plugin - must be QuickStocks plugin instance");
        }
        return getInstance();
    }
    
    /**
     * Resets the API instance. Should only be called by QuickStocks plugin during shutdown.
     * This is an internal method and should not be called by external plugins.
     */
    public static void reset() {
        instance = null;
    }
    
    // Manager getters
    
    /**
     * Gets the Company Manager for company operations.
     * 
     * @return CompanyManager instance
     */
    public CompanyManager getCompanyManager() {
        return companyManager;
    }
    
    /**
     * Gets the Trading Manager for trading operations.
     * 
     * @return TradingManager instance
     */
    public TradingManager getTradingManager() {
        return tradingManager;
    }
    
    /**
     * Gets the Market Manager for market and instrument operations.
     * 
     * @return MarketManager instance
     */
    public MarketManager getMarketManager() {
        return marketManager;
    }
    
    /**
     * Gets the Wallet Manager for wallet and balance operations.
     * 
     * @return WalletManager instance
     */
    public WalletManager getWalletManager() {
        return walletManager;
    }
    
    /**
     * Gets the Watchlist Manager for watchlist operations.
     * 
     * @return WatchlistManager instance
     */
    public WatchlistManager getWatchlistManager() {
        return watchlistManager;
    }
    
    /**
     * Gets the Crypto Manager for cryptocurrency operations.
     * 
     * @return CryptoManager instance
     */
    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }
    
    /**
     * Gets the Portfolio Manager for portfolio and holdings operations.
     * 
     * @return PortfolioManager instance
     */
    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }
}
