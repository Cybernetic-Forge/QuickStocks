package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;

import java.util.List;

/**
 * Configuration manager for trading economy settings.
 * Loads configuration from trading.yml using YamlParser.
 */
@Getter
public class TradingCfg {

    private final YamlParser config;
    private TradingConfig tradingConfig;
    
    public TradingCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "trading.yml");
        loadValues();
    }
    
    /**
     * Loads all configuration values from the YAML file and populates TradingConfig
     */
    private void loadValues() {
        tradingConfig = new TradingConfig();
        
        // Fee configuration
        TradingConfig.FeeConfig feeConfig = new TradingConfig.FeeConfig();
        feeConfig.setMode(config.getString("trading.fee.mode", "percent"));
        feeConfig.setPercent(config.getDouble("trading.fee.percent", 0.25));
        feeConfig.setFlat(config.getDouble("trading.fee.flat", 0.0));
        tradingConfig.setFee(feeConfig);
        
        // Limits configuration
        TradingConfig.LimitsConfig limitsConfig = new TradingConfig.LimitsConfig();
        limitsConfig.setMaxOrderQty(config.getDouble("trading.limits.maxOrderQty", 10000));
        limitsConfig.setMaxNotionalPerMinute(config.getDouble("trading.limits.maxNotionalPerMinute", 250000));
        limitsConfig.setPerPlayerCooldownMs(config.getLong("trading.limits.perPlayerCooldownMs", 750));
        tradingConfig.setLimits(limitsConfig);
        
        // Circuit breakers configuration
        TradingConfig.CircuitBreakerConfig cbConfig = new TradingConfig.CircuitBreakerConfig();
        cbConfig.setEnable(config.getBoolean("trading.circuitBreakers.enable", true));
        
        List<Double> levels = config.getDoubleList("trading.circuitBreakers.levels");
        if (levels.isEmpty()) {
            levels = List.of(7.0, 13.0, 20.0);
        }
        cbConfig.setLevels(levels);
        
        List<Integer> haltMinutes = config.getIntegerList("trading.circuitBreakers.haltMinutes");
        if (haltMinutes.isEmpty()) {
            haltMinutes = List.of(15, 15, -1);
        }
        cbConfig.setHaltMinutes(haltMinutes);
        tradingConfig.setCircuitBreakers(cbConfig);
        
        // Orders configuration
        TradingConfig.OrdersConfig ordersConfig = new TradingConfig.OrdersConfig();
        ordersConfig.setAllowMarket(config.getBoolean("trading.orders.allowMarket", true));
        ordersConfig.setAllowLimit(config.getBoolean("trading.orders.allowLimit", true));
        ordersConfig.setAllowStop(config.getBoolean("trading.orders.allowStop", true));
        tradingConfig.setOrders(ordersConfig);
        
        // Slippage configuration
        TradingConfig.SlippageConfig slippageConfig = new TradingConfig.SlippageConfig();
        slippageConfig.setMode(config.getString("trading.slippage.mode", "linear"));
        slippageConfig.setK(config.getDouble("trading.slippage.k", 0.0005));
        tradingConfig.setSlippage(slippageConfig);
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }
}
