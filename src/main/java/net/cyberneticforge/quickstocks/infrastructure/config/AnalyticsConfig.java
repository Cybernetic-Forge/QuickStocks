package net.cyberneticforge.quickstocks.infrastructure.config;

/**
 * Configuration holder for analytics settings.
 * Contains EWMA lambda and default window settings for various metrics.
 */
public class AnalyticsConfig {
    
    private double lambda = 0.94;
    private DefaultWindows defaultWindowsMinutes = new DefaultWindows();
    
    public double getLambda() {
        return lambda;
    }
    
    public void setLambda(double lambda) {
        this.lambda = lambda;
    }
    
    public DefaultWindows getDefaultWindowsMinutes() {
        return defaultWindowsMinutes;
    }
    
    public void setDefaultWindowsMinutes(DefaultWindows defaultWindowsMinutes) {
        this.defaultWindowsMinutes = defaultWindowsMinutes;
    }
    
    public static class DefaultWindows {
        private int change = 1440;      // 24 hours
        private int volatility = 1440;  // 24 hours  
        private int correlation = 1440; // 24 hours
        
        public int getChange() {
            return change;
        }
        
        public void setChange(int change) {
            this.change = change;
        }
        
        public int getVolatility() {
            return volatility;
        }
        
        public void setVolatility(int volatility) {
            this.volatility = volatility;
        }
        
        public int getCorrelation() {
            return correlation;
        }
        
        public void setCorrelation(int correlation) {
            this.correlation = correlation;
        }
    }
}