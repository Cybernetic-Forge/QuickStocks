package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for cryptocurrency settings.
 * Loads configuration from market.yml using YamlParser.
 */
@Getter
public class CryptoCfg {

    private final YamlParser config;

    private final PersonalCryptoConfig personalConfig = new PersonalCryptoConfig();
    private final CompanyCryptoConfig companyConfig = new CompanyCryptoConfig();
    private final DefaultsConfig defaultsConfig = new DefaultsConfig();
    private final TradingConfig tradingConfig = new TradingConfig();
    
    private boolean enabled = true;
    
    public CryptoCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "market.yml");
        addMissingDefaults();
        loadValues();
    }
    
    /**
     * Adds missing configuration entries with default values
     */
    private void addMissingDefaults() {
        config.addMissing("crypto.enabled", true);
        
        // Personal crypto settings
        config.addMissing("crypto.personal.enabled", true);
        config.addMissing("crypto.personal.creationCost", 500000.0);
        config.addMissing("crypto.personal.maxPerPlayer", -1);
        
        // Company crypto settings
        config.addMissing("crypto.company.enabled", true);
        config.addMissing("crypto.company.balanceThreshold", 100000.0);
        config.addMissing("crypto.company.balanceThresholds.PRIVATE", 100000.0);
        config.addMissing("crypto.company.balanceThresholds.PUBLIC", 250000.0);
        config.addMissing("crypto.company.balanceThresholds.DAO", 150000.0);
        config.addMissing("crypto.company.maxPerCompany", -1);
        
        // Default settings
        config.addMissing("crypto.defaults.startingPrice", 1.0);
        config.addMissing("crypto.defaults.decimals", 8);
        config.addMissing("crypto.defaults.initialVolume", 0.0);
        
        // Trading settings
        config.addMissing("crypto.trading.minPrice", 0.00000001);
        config.addMissing("crypto.trading.maxPrice", 1000000.0);
        
        config.saveChanges();
    }
    
    /**
     * Loads all configuration values from the YAML file
     */
    private void loadValues() {
        enabled = config.getBoolean("crypto.enabled", true);
        
        // Personal crypto
        personalConfig.setEnabled(config.getBoolean("crypto.personal.enabled", true));
        personalConfig.setCreationCost(config.getDouble("crypto.personal.creationCost", 500000.0));
        personalConfig.setMaxPerPlayer(config.getInt("crypto.personal.maxPerPlayer", -1));
        
        // Company crypto
        companyConfig.setEnabled(config.getBoolean("crypto.company.enabled", true));
        companyConfig.setBalanceThreshold(config.getDouble("crypto.company.balanceThreshold", 100000.0));
        
        // Load balance thresholds by company type
        Map<String, Double> balanceThresholds = new HashMap<>();
        balanceThresholds.put("PRIVATE", config.getDouble("crypto.company.balanceThresholds.PRIVATE", 100000.0));
        balanceThresholds.put("PUBLIC", config.getDouble("crypto.company.balanceThresholds.PUBLIC", 250000.0));
        balanceThresholds.put("DAO", config.getDouble("crypto.company.balanceThresholds.DAO", 150000.0));
        companyConfig.setBalanceThresholds(balanceThresholds);
        
        companyConfig.setMaxPerCompany(config.getInt("crypto.company.maxPerCompany", -1));
        
        // Defaults
        defaultsConfig.setStartingPrice(config.getDouble("crypto.defaults.startingPrice", 1.0));
        defaultsConfig.setDecimals(config.getInt("crypto.defaults.decimals", 8));
        defaultsConfig.setInitialVolume(config.getDouble("crypto.defaults.initialVolume", 0.0));
        
        // Trading
        tradingConfig.setMinPrice(config.getDouble("crypto.trading.minPrice", 0.00000001));
        tradingConfig.setMaxPrice(config.getDouble("crypto.trading.maxPrice", 1000000.0));
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }

    @Setter
    @Getter
    public static class PersonalCryptoConfig {
        private boolean enabled = true;
        private double creationCost = 500000.0;
        private int maxPerPlayer = -1; // -1 = unlimited
    }

    @Setter
    @Getter
    public static class CompanyCryptoConfig {
        private boolean enabled = true;
        private double balanceThreshold = 100000.0;
        private Map<String, Double> balanceThresholds = new HashMap<>();
        private int maxPerCompany = -1; // -1 = unlimited
    }

    @Setter
    @Getter
    public static class DefaultsConfig {
        private double startingPrice = 1.0;
        private int decimals = 8;
        private double initialVolume = 0.0;
    }

    @Setter
    @Getter
    public static class TradingConfig {
        private double minPrice = 0.00000001;
        private double maxPrice = 1000000.0;
    }
}
