package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.infrastructure.db.DatabaseConfig;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Service for managing bStats metrics and custom charts.
 * Collects anonymous usage statistics to help improve the plugin.
 */
public class MetricsService {
    
    private static final Logger logger = Logger.getLogger(MetricsService.class.getName());
    private static final int BSTATS_PLUGIN_ID = 24106; // bStats plugin ID for QuickStocks
    
    private final JavaPlugin plugin;
    private final DatabaseConfig databaseConfig;
    private final StockMarketService stockMarketService;
    private final CompanyService companyService;
    private final HoldingsService holdingsService;
    private Metrics metrics;
    
    /**
     * Creates a new MetricsService instance.
     *
     * @param plugin The plugin instance
     * @param databaseConfig The database configuration
     * @param stockMarketService The stock market service for market stats
     * @param companyService The company service for company stats
     * @param holdingsService The holdings service for trader stats
     */
    public MetricsService(JavaPlugin plugin, DatabaseConfig databaseConfig, 
                          StockMarketService stockMarketService,
                          CompanyService companyService,
                          HoldingsService holdingsService) {
        this.plugin = plugin;
        this.databaseConfig = databaseConfig;
        this.stockMarketService = stockMarketService;
        this.companyService = companyService;
        this.holdingsService = holdingsService;
    }
    
    /**
     * Initializes bStats metrics with custom charts.
     */
    public void initialize() {
        try {
            metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
            
            // Register custom charts
            registerDatabaseProviderChart();
            registerActiveInstrumentsChart();
            registerCompaniesChart();
            registerActiveTradersChart();
            registerMarketStatusChart();
            
            logger.info("bStats metrics initialized successfully");
        } catch (Exception e) {
            logger.warning("Failed to initialize bStats metrics: " + e.getMessage());
        }
    }
    
    /**
     * Registers a chart showing the database provider being used.
     */
    private void registerDatabaseProviderChart() {
        metrics.addCustomChart(new SimplePie("database_provider", () -> {
            if (databaseConfig != null && databaseConfig.getProvider() != null) {
                return databaseConfig.getProvider().toUpperCase();
            }
            return "UNKNOWN";
        }));
    }
    
    /**
     * Registers a chart showing the number of active instruments (stocks/cryptos).
     */
    private void registerActiveInstrumentsChart() {
        metrics.addCustomChart(new SingleLineChart("active_instruments", () -> {
            if (stockMarketService != null) {
                return stockMarketService.getAllStocks().size();
            }
            return 0;
        }));
    }
    
    /**
     * Registers a chart showing the number of companies.
     */
    private void registerCompaniesChart() {
        metrics.addCustomChart(new SingleLineChart("total_companies", () -> {
            if (companyService != null) {
                try {
                    return companyService.getAllCompanies().size();
                } catch (Exception e) {
                    logger.warning("Failed to get company count for metrics: " + e.getMessage());
                    return 0;
                }
            }
            return 0;
        }));
    }
    
    /**
     * Registers a chart showing the number of active traders (players with holdings).
     */
    private void registerActiveTradersChart() {
        metrics.addCustomChart(new SingleLineChart("active_traders", () -> {
            if (holdingsService != null) {
                try {
                    return holdingsService.getPlayerCountWithHoldings();
                } catch (Exception e) {
                    logger.warning("Failed to get active traders count for metrics: " + e.getMessage());
                    return 0;
                }
            }
            return 0;
        }));
    }
    
    /**
     * Registers a chart showing whether the market is currently open or closed.
     */
    private void registerMarketStatusChart() {
        metrics.addCustomChart(new SimplePie("market_status", () -> {
            if (stockMarketService != null) {
                return stockMarketService.isMarketOpen() ? "Open" : "Closed";
            }
            return "Unknown";
        }));
    }
    
    /**
     * Shuts down the metrics service.
     */
    public void shutdown() {
        if (metrics != null) {
            logger.info("Shutting down bStats metrics");
            metrics = null;
        }
    }
}
