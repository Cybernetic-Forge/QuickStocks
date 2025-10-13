package net.cyberneticforge.quickstocks.infrastructure.db;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple configuration loader that reads from config.yml or properties.
 * This is a minimal implementation that doesn't depend on external YAML libraries.
 */
public class ConfigLoader {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    /**
     * Loads database configuration from config.yml and market.yml or falls back to defaults.
     */
    public static DatabaseConfig loadDatabaseConfig() {
        DatabaseConfig config = new DatabaseConfig();
        
        try {
            // Load from config.yml (database and features settings)
            File configFile = new File("plugins/QuickStocks/config.yml");
            if (configFile.exists()) {
                loadFromFile(config, configFile);
            } else {
                // Try to load from classpath
                InputStream stream = ConfigLoader.class.getResourceAsStream("/config.yml");
                if (stream != null) {
                    loadFromStream(config, stream);
                } else {
                    logger.warning("No config.yml found, using defaults");
                }
            }
            
            // Load from market.yml (price threshold settings)
            File marketFile = new File("plugins/QuickStocks/market.yml");
            if (marketFile.exists()) {
                loadFromFile(config, marketFile);
            } else {
                // Try to load from classpath
                InputStream stream = ConfigLoader.class.getResourceAsStream("/market.yml");
                if (stream != null) {
                    loadFromStream(config, stream);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load config, using defaults: " + e.getMessage());
        }
        
        return config;
    }
    
    private static void loadFromFile(DatabaseConfig config, File file) throws Exception {
        try (FileInputStream stream = new FileInputStream(file)) {
            loadFromStream(config, stream);
        }
    }
    
    private static void loadFromStream(DatabaseConfig config, InputStream stream) {
        // Simple YAML-like parsing for basic configuration
        // This is a minimal implementation - in a real project you'd use a proper YAML library
        Properties props = new Properties();
        
        // Convert simple YAML to properties format
        java.util.Scanner scanner = new java.util.Scanner(stream);
        String currentSection = "";
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            if (line.endsWith(":") && !line.contains(" ")) {
                // Section header
                currentSection = line.substring(0, line.length() - 1) + ".";
            } else if (line.contains(":")) {
                // Key-value pair
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = getValue(parts);

                    props.setProperty(currentSection + key, value);
                }
            }
        }
        
        // Apply configuration
        applyConfig(config, props);
        
        logger.info("Configuration loaded successfully");
    }

    private static @NotNull String getValue(String[] parts) {
        String value = parts[1].trim();

        // Remove inline comments
        int commentIndex = value.indexOf('#');
        if (commentIndex != -1) {
            value = value.substring(0, commentIndex).trim();
        }

        // Handle quoted strings
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void applyConfig(DatabaseConfig config, Properties props) {
        // Database configuration
        String provider = props.getProperty("database.provider", "sqlite");
        config.setProvider(provider);
        
        // SQLite configuration
        String sqliteFile = props.getProperty("database.sqlite.file", "plugins/QuickStocks/data.db");
        config.setSqliteFile(sqliteFile);
        
        // MySQL configuration
        String mysqlHost = props.getProperty("database.mysql.host", "127.0.0.1");
        config.setMysqlHost(mysqlHost);
        
        String mysqlPort = props.getProperty("database.mysql.port", "3306");
        config.setMysqlPort(Integer.parseInt(mysqlPort));
        
        String mysqlDatabase = props.getProperty("database.mysql.database", "QuickStocks");
        config.setMysqlDatabase(mysqlDatabase);
        
        String mysqlUser = props.getProperty("database.mysql.user", "root");
        config.setMysqlUser(mysqlUser);
        
        String mysqlPassword = props.getProperty("database.mysql.password", "");
        config.setMysqlPassword(mysqlPassword);
        
        String mysqlUseSSL = props.getProperty("database.mysql.useSSL", "false");
        config.setMysqlUseSSL(Boolean.parseBoolean(mysqlUseSSL));
        
        // PostgreSQL configuration
        String postgresHost = props.getProperty("database.postgres.host", "127.0.0.1");
        config.setPostgresHost(postgresHost);
        
        String postgresPort = props.getProperty("database.postgres.port", "5432");
        config.setPostgresPort(Integer.parseInt(postgresPort));
        
        String postgresDatabase = props.getProperty("database.postgres.database", "QuickStocks");
        config.setPostgresDatabase(postgresDatabase);
        
        String postgresUser = props.getProperty("database.postgres.user", "postgres");
        config.setPostgresUser(postgresUser);
        
        String postgresPassword = props.getProperty("database.postgres.password", "");
        config.setPostgresPassword(postgresPassword);
        
        // Features configuration
        String historyEnabled = props.getProperty("features.historyEnabled", "true");
        config.setHistoryEnabled(Boolean.parseBoolean(historyEnabled));
        
        String topListWindowHours = props.getProperty("features.topListWindowHours", "24");
        config.setTopListWindowHours(Integer.parseInt(topListWindowHours));
        
        // Price threshold configuration
        String priceThresholdEnabled = props.getProperty("market.priceThreshold.enabled", "true");
        config.setPriceThresholdEnabled(Boolean.parseBoolean(priceThresholdEnabled));
        
        String maxChangePercent = props.getProperty("market.priceThreshold.maxChangePercent", "0.15");
        config.setMaxChangePercent(Double.parseDouble(maxChangePercent));
        
        String priceMultiplierThreshold = props.getProperty("market.priceThreshold.priceMultiplierThreshold", "5.0");
        config.setPriceMultiplierThreshold(Double.parseDouble(priceMultiplierThreshold));
        
        String dampeningFactor = props.getProperty("market.priceThreshold.dampeningFactor", "0.3");
        config.setDampeningFactor(Double.parseDouble(dampeningFactor));
        
        String minVolumeThreshold = props.getProperty("market.priceThreshold.minVolumeThreshold", "100");
        config.setMinVolumeThreshold(Integer.parseInt(minVolumeThreshold));
        
        String volumeSensitivity = props.getProperty("market.priceThreshold.volumeSensitivity", "0.5");
        config.setVolumeSensitivity(Double.parseDouble(volumeSensitivity));
    }
}