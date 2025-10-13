package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;

/**
 * Configuration manager for market and market device settings.
 * Loads configuration from market.yml using YamlParser.
 */
@Getter
public class MarketCfg {

    private final YamlParser config;
    
    // Market settings
    private boolean enabled;
    private int updateInterval;
    private boolean startOpen;
    private boolean defaultStocks;
    
    // Sub-feature toggles
    private boolean watchlistEnabled;
    private boolean portfolioEnabled;
    private boolean tradingEnabled;
    private boolean marketDeviceEnabled;
    private boolean stocksCommandEnabled;
    private boolean cryptoCommandEnabled;
    
    // Price threshold settings
    private boolean priceThresholdEnabled;
    private double maxChangePercent;
    private double priceMultiplierThreshold;
    private double dampeningFactor;
    private int minVolumeThreshold;
    private double volumeSensitivity;
    
    // Analytics settings
    private double analyticsLambda;
    private int analyticsChangeWindow;
    private int analyticsVolatilityWindow;
    private int analyticsCorrelationWindow;
    
    public MarketCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "market.yml");
        addMissingDefaults();
        loadValues();
    }
    
    /**
     * Adds missing configuration entries with default values
     */
    private void addMissingDefaults() {
        // Market settings
        config.addMissing("market.enabled", true);
        config.addMissing("market.updateInterval", 5);
        config.addMissing("market.startOpen", true);
        config.addMissing("market.defaultStocks", true);

        // Sub-feature toggles
        config.addMissing("market.features.watchlist", true);
        config.addMissing("market.features.portfolio", true);
        config.addMissing("market.features.trading", true);
        config.addMissing("market.features.marketDevice", true);
        config.addMissing("market.features.stocksCommand", true);
        config.addMissing("market.features.cryptoCommand", true);

        // Price threshold settings
        config.addMissing("market.priceThreshold.enabled", true);
        config.addMissing("market.priceThreshold.maxChangePercent", 0.15);
        config.addMissing("market.priceThreshold.priceMultiplierThreshold", 5.0);
        config.addMissing("market.priceThreshold.dampeningFactor", 0.3);
        config.addMissing("market.priceThreshold.minVolumeThreshold", 100);
        config.addMissing("market.priceThreshold.volumeSensitivity", 0.5);
        
        // Analytics settings
        config.addMissing("analytics.lambda", 0.94);
        config.addMissing("analytics.defaultWindowsMinutes.change", 1440);
        config.addMissing("analytics.defaultWindowsMinutes.volatility", 1440);
        config.addMissing("analytics.defaultWindowsMinutes.correlation", 1440);
        
        config.saveChanges();
    }
    
    /**
     * Loads all configuration values from the YAML file
     */
    private void loadValues() {
        // Market settings
        enabled = config.getBoolean("market.enabled", true);
        updateInterval = config.getInt("market.updateInterval", 5);
        startOpen = config.getBoolean("market.startOpen", true);
        defaultStocks = config.getBoolean("market.defaultStocks", true);
        
        // Sub-feature toggles
        watchlistEnabled = config.getBoolean("market.features.watchlist", true);
        portfolioEnabled = config.getBoolean("market.features.portfolio", true);
        tradingEnabled = config.getBoolean("market.features.trading", true);
        marketDeviceEnabled = config.getBoolean("market.features.marketDevice", true);
        stocksCommandEnabled = config.getBoolean("market.features.stocksCommand", true);
        cryptoCommandEnabled = config.getBoolean("market.features.cryptoCommand", true);
        
        // Price threshold settings
        priceThresholdEnabled = config.getBoolean("market.priceThreshold.enabled", true);
        maxChangePercent = config.getDouble("market.priceThreshold.maxChangePercent", 0.15);
        priceMultiplierThreshold = config.getDouble("market.priceThreshold.priceMultiplierThreshold", 5.0);
        dampeningFactor = config.getDouble("market.priceThreshold.dampeningFactor", 0.3);
        minVolumeThreshold = config.getInt("market.priceThreshold.minVolumeThreshold", 100);
        volumeSensitivity = config.getDouble("market.priceThreshold.volumeSensitivity", 0.5);
        
        // Analytics settings
        analyticsLambda = config.getDouble("analytics.lambda", 0.94);
        analyticsChangeWindow = config.getInt("analytics.defaultWindowsMinutes.change", 1440);
        analyticsVolatilityWindow = config.getInt("analytics.defaultWindowsMinutes.volatility", 1440);
        analyticsCorrelationWindow = config.getInt("analytics.defaultWindowsMinutes.correlation", 1440);
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }
}
