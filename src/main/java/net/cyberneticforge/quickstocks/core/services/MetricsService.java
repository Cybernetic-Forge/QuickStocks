package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

/**
 * Service for managing bStats metrics and custom charts.
 * Collects anonymous usage statistics to help improve the plugin.
 */
public class MetricsService {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    private static final int BSTATS_PLUGIN_ID = 27476; // bStats plugin ID for QuickStocks
    private Metrics metrics;
    
    /**
     * Creates a new MetricsService instance.
     */
    public MetricsService() {
    }
    
    /**
     * Initializes bStats metrics with custom charts.
     */
    public void initialize() {
        try {
            metrics = new Metrics(QuickStocksPlugin.getInstance(), BSTATS_PLUGIN_ID);
            
            // Register custom charts
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
     * Registers a chart showing the number of active instruments (stocks/cryptos).
     */
    private void registerActiveInstrumentsChart() {
        metrics.addCustomChart(new SingleLineChart("active_instruments", () -> {
            if (QuickStocksPlugin.getStockMarketService() != null) {
                return QuickStocksPlugin.getStockMarketService().getAllStocks().size();
            }
            return 0;
        }));
    }
    
    /**
     * Registers a chart showing the number of companies.
     */
    private void registerCompaniesChart() {
        metrics.addCustomChart(new SingleLineChart("total_companies", () -> {
            if (QuickStocksPlugin.getCompanyService() != null) {
                try {
                    return QuickStocksPlugin.getCompanyService().getAllCompanies().size();
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
            if (QuickStocksPlugin.getHoldingsService() != null) {
                try {
                    return QuickStocksPlugin.getHoldingsService().getPlayerCountWithHoldings();
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
            if (QuickStocksPlugin.getStockMarketService() != null) {
                return QuickStocksPlugin.getStockMarketService().isMarketOpen() ? "Open" : "Closed";
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
