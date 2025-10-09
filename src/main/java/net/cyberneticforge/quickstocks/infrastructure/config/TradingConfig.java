package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Configuration class for trading economy features including fees, limits, 
 * circuit breakers, order types, and slippage.
 */
@Setter
@Getter
public class TradingConfig {
    
    // Fee configuration
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
    
    private FeeConfig fee = new FeeConfig();
    private LimitsConfig limits = new LimitsConfig();
    private CircuitBreakerConfig circuitBreakers = new CircuitBreakerConfig();
    private OrdersConfig orders = new OrdersConfig();
    private SlippageConfig slippage = new SlippageConfig();

}