package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;

import java.util.List;

/**
 * Configuration manager for trading economy settings.
 * Loads configuration from market.yml using YamlParser.
 */
@Getter
public class TradingCfg {

    private final YamlParser config;

    private final FeeConfig feeConfig = new FeeConfig();
    private final LimitsConfig limitsConfig = new LimitsConfig();
    private final CircuitBreakerConfig circuitBreakersConfig = new CircuitBreakerConfig();
    private final OrdersConfig ordersConfig = new OrdersConfig();
    private final SlippageConfig slippageConfig = new SlippageConfig();
    
    public TradingCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "market.yml");
        addMissingDefaults();
        loadValues();
    }
    
    /**
     * Adds missing configuration entries with default values
     */
    private void addMissingDefaults() {
        // First, add any missing values from the internal resource
        config.addMissingFromResource("/trading.yml");
        
        // Then add specific defaults that might not be in the resource
        // Fee settings
        config.addMissing("trading.fee.mode", "percent");
        config.addMissing("trading.fee.percent", 0.25);
        config.addMissing("trading.fee.flat", 0.0);
        
        // Limits settings
        config.addMissing("trading.limits.maxOrderQty", 10000);
        config.addMissing("trading.limits.maxNotionalPerMinute", 250000);
        config.addMissing("trading.limits.perPlayerCooldownMs", 750);
        
        // Circuit breakers settings
        config.addMissing("trading.circuitBreakers.enable", true);
        config.addMissing("trading.circuitBreakers.levels", List.of(7.0, 13.0, 20.0));
        config.addMissing("trading.circuitBreakers.haltMinutes", List.of(15, 15, -1));
        
        // Orders settings
        config.addMissing("trading.orders.allowMarket", true);
        config.addMissing("trading.orders.allowLimit", true);
        config.addMissing("trading.orders.allowStop", true);
        
        // Slippage settings
        config.addMissing("trading.slippage.mode", "linear");
        config.addMissing("trading.slippage.k", 0.0005);
        
        config.saveChanges();
    }
    
    /**
     * Loads all configuration values from the YAML file and populates TradingConfig
     */
    private void loadValues() {
        feeConfig.setMode(config.getString("trading.fee.mode", "percent"));
        feeConfig.setPercent(config.getDouble("trading.fee.percent", 0.25));
        feeConfig.setFlat(config.getDouble("trading.fee.flat", 0.0));

        limitsConfig.setMaxOrderQty(config.getDouble("trading.limits.maxOrderQty", 10000));
        limitsConfig.setMaxNotionalPerMinute(config.getDouble("trading.limits.maxNotionalPerMinute", 250000));
        limitsConfig.setPerPlayerCooldownMs(config.getLong("trading.limits.perPlayerCooldownMs", 750));

        circuitBreakersConfig.setEnable(config.getBoolean("trading.circuitBreakers.enable", true));
        List<Double> levels = config.getDoubleList("trading.circuitBreakers.levels");
        if (levels.isEmpty()) {
            levels = List.of(7.0, 13.0, 20.0);
        }
        circuitBreakersConfig.setLevels(levels);
        List<Integer> haltMinutes = config.getIntegerList("trading.circuitBreakers.haltMinutes");
        if (haltMinutes.isEmpty()) {
            haltMinutes = List.of(15, 15, -1);
        }
        circuitBreakersConfig.setHaltMinutes(haltMinutes);

        ordersConfig.setAllowMarket(config.getBoolean("trading.orders.allowMarket", true));
        ordersConfig.setAllowLimit(config.getBoolean("trading.orders.allowLimit", true));
        ordersConfig.setAllowStop(config.getBoolean("trading.orders.allowStop", true));

        slippageConfig.setMode(config.getString("trading.slippage.mode", "linear"));
        slippageConfig.setK(config.getDouble("trading.slippage.k", 0.0005));
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
    public static class FeeConfig {
        private String mode = "percent"; // percent | flat | mixed
        private double percent = 0.25;   // % of notional
        private double flat = 0.0;

    }

    // Limits configuration
    @Setter
    @Getter
    public static class LimitsConfig {
        private double maxOrderQty = 10000;
        private double maxNotionalPerMinute = 250000;
        private long perPlayerCooldownMs = 750;
    }

    // Circuit breaker configuration
    @Getter
    @Setter
    public static class CircuitBreakerConfig {
        private boolean enable = true;
        private List<Double> levels = List.of(7.0, 13.0, 20.0);     // halt thresholds in % move from daily open
        private List<Integer> haltMinutes = List.of(15, 15, -1);    // -1 = rest of session
    }

    // Order types configuration
    @Setter
    @Getter
    public static class OrdersConfig {
        private boolean allowMarket = true;
        private boolean allowLimit = true;
        private boolean allowStop = true;
    }

    // Slippage configuration
    @Setter
    @Getter
    public static class SlippageConfig {
        private String mode = "linear";     // none | linear | sqrtImpact
        private double k = 0.0005;          // impact coefficient
    }
}
