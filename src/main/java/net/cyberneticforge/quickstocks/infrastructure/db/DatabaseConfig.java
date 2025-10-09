package net.cyberneticforge.quickstocks.infrastructure.db;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.infrastructure.config.AnalyticsConfig;

/**
 * Configuration holder for database settings.
 * Supports SQLite, MySQL, and PostgreSQL configurations.
 */
@Setter
@Getter
public class DatabaseConfig {
    
    private String provider = "sqlite";
    
    // SQLite configuration
    private String sqliteFile = "plugins/QuickStocks/data.db";
    
    // MySQL configuration
    private String mysqlHost = "127.0.0.1";
    private int mysqlPort = 3306;
    private String mysqlDatabase = "QuickStocks";
    private String mysqlUser = "root";
    private String mysqlPassword = "";
    private boolean mysqlUseSSL = false;
    
    // PostgreSQL configuration
    private String postgresHost = "127.0.0.1";
    private int postgresPort = 5432;
    private String postgresDatabase = "QuickStocks";
    private String postgresUser = "postgres";
    private String postgresPassword = "";
    
    // Feature configuration
    private boolean historyEnabled = true;
    private int topListWindowHours = 24;
    
    // Analytics configuration
    private AnalyticsConfig analytics = new AnalyticsConfig();
    
    // Price threshold configuration
    private boolean priceThresholdEnabled = true;
    private double maxChangePercent = 0.15;
    private double priceMultiplierThreshold = 5.0;
    private double dampeningFactor = 0.3;
    private int minVolumeThreshold = 100;
    private double volumeSensitivity = 0.5;
}