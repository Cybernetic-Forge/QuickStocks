package net.cyberneticforge.quickstocks.infrastructure.config;

import java.util.List;

/**
 * Configuration class for trading economy features including fees, limits, 
 * circuit breakers, order types, and slippage.
 */
public class TradingConfig {
    
    // Fee configuration
    public static class FeeConfig {
        private String mode = "percent"; // percent | flat | mixed
        private double percent = 0.25;   // % of notional
        private double flat = 0.0;
        
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        
        public double getPercent() { return percent; }
        public void setPercent(double percent) { this.percent = percent; }
        
        public double getFlat() { return flat; }
        public void setFlat(double flat) { this.flat = flat; }
    }
    
    // Limits configuration
    public static class LimitsConfig {
        private double maxOrderQty = 10000;
        private double maxNotionalPerMinute = 250000;
        private long perPlayerCooldownMs = 750;
        
        public double getMaxOrderQty() { return maxOrderQty; }
        public void setMaxOrderQty(double maxOrderQty) { this.maxOrderQty = maxOrderQty; }
        
        public double getMaxNotionalPerMinute() { return maxNotionalPerMinute; }
        public void setMaxNotionalPerMinute(double maxNotionalPerMinute) { this.maxNotionalPerMinute = maxNotionalPerMinute; }
        
        public long getPerPlayerCooldownMs() { return perPlayerCooldownMs; }
        public void setPerPlayerCooldownMs(long perPlayerCooldownMs) { this.perPlayerCooldownMs = perPlayerCooldownMs; }
    }
    
    // Circuit breaker configuration
    public static class CircuitBreakerConfig {
        private boolean enable = true;
        private List<Double> levels = List.of(7.0, 13.0, 20.0);     // halt thresholds in % move from daily open
        private List<Integer> haltMinutes = List.of(15, 15, -1);    // -1 = rest of session
        
        public boolean isEnable() { return enable; }
        public void setEnable(boolean enable) { this.enable = enable; }
        
        public List<Double> getLevels() { return levels; }
        public void setLevels(List<Double> levels) { this.levels = levels; }
        
        public List<Integer> getHaltMinutes() { return haltMinutes; }
        public void setHaltMinutes(List<Integer> haltMinutes) { this.haltMinutes = haltMinutes; }
    }
    
    // Order types configuration
    public static class OrdersConfig {
        private boolean allowMarket = true;
        private boolean allowLimit = true;
        private boolean allowStop = true;
        
        public boolean isAllowMarket() { return allowMarket; }
        public void setAllowMarket(boolean allowMarket) { this.allowMarket = allowMarket; }
        
        public boolean isAllowLimit() { return allowLimit; }
        public void setAllowLimit(boolean allowLimit) { this.allowLimit = allowLimit; }
        
        public boolean isAllowStop() { return allowStop; }
        public void setAllowStop(boolean allowStop) { this.allowStop = allowStop; }
    }
    
    // Slippage configuration
    public static class SlippageConfig {
        private String mode = "linear";     // none | linear | sqrtImpact
        private double k = 0.0005;          // impact coefficient
        
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        
        public double getK() { return k; }
        public void setK(double k) { this.k = k; }
    }
    
    private FeeConfig fee = new FeeConfig();
    private LimitsConfig limits = new LimitsConfig();
    private CircuitBreakerConfig circuitBreakers = new CircuitBreakerConfig();
    private OrdersConfig orders = new OrdersConfig();
    private SlippageConfig slippage = new SlippageConfig();
    
    public FeeConfig getFee() { return fee; }
    public void setFee(FeeConfig fee) { this.fee = fee; }
    
    public LimitsConfig getLimits() { return limits; }
    public void setLimits(LimitsConfig limits) { this.limits = limits; }
    
    public CircuitBreakerConfig getCircuitBreakers() { return circuitBreakers; }
    public void setCircuitBreakers(CircuitBreakerConfig circuitBreakers) { this.circuitBreakers = circuitBreakers; }
    
    public OrdersConfig getOrders() { return orders; }
    public void setOrders(OrdersConfig orders) { this.orders = orders; }
    
    public SlippageConfig getSlippage() { return slippage; }
    public void setSlippage(SlippageConfig slippage) { this.slippage = slippage; }
}