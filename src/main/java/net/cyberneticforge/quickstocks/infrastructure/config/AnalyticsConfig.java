package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration holder for analytics settings.
 * Contains EWMA lambda and default window settings for various metrics.
 */
@Setter
@Getter
@SuppressWarnings("unused")
public class AnalyticsConfig {
    
    private double lambda = 0.94;
    private DefaultWindows defaultWindowsMinutes = new DefaultWindows();

    @Setter
    @Getter
    public static class DefaultWindows {
        private int change = 1440;      // 24 hours
        private int volatility = 1440;  // 24 hours  
        private int correlation = 1440; // 24 hours
    }
}