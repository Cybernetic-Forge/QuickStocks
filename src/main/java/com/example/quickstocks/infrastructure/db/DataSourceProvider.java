package com.example.quickstocks.infrastructure.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.util.logging.Logger;

/**
 * Provides database connection through HikariCP connection pool.
 * Supports SQLite, MySQL, and PostgreSQL based on configuration.
 */
public class DataSourceProvider {
    
    private static final Logger logger = Logger.getLogger(DataSourceProvider.class.getName());
    
    private final DatabaseConfig config;
    private HikariDataSource dataSource;
    
    public DataSourceProvider(DatabaseConfig config) {
        this.config = config;
    }
    
    /**
     * Initializes the data source with HikariCP connection pool.
     */
    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();
        
        switch (config.getProvider().toLowerCase()) {
            case "sqlite":
                configureSQLite(hikariConfig);
                break;
            case "mysql":
                configureMySQL(hikariConfig);
                break;
            case "postgres":
                configurePostgreSQL(hikariConfig);
                break;
            default:
                throw new IllegalArgumentException("Unsupported database provider: " + config.getProvider());
        }
        
        // Common HikariCP settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(hikariConfig);
        logger.info("Database connection pool initialized for " + config.getProvider());
    }
    
    private void configureSQLite(HikariConfig config) {
        String filePath = this.config.getSqliteFile();
        
        // Create directory if it doesn't exist
        File dbFile = new File(filePath);
        dbFile.getParentFile().mkdirs();
        
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + filePath);
        
        // SQLite specific settings
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "10000");
        config.addDataSourceProperty("foreign_keys", "true");
        
        logger.info("Configured SQLite database at: " + filePath);
    }
    
    private void configureMySQL(HikariConfig config) {
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            this.config.getMysqlHost(),
            this.config.getMysqlPort(),
            this.config.getMysqlDatabase(),
            this.config.isMysqlUseSSL()));
        config.setUsername(this.config.getMysqlUser());
        config.setPassword(this.config.getMysqlPassword());
        
        logger.info("Configured MySQL database at: " + this.config.getMysqlHost() + ":" + this.config.getMysqlPort());
    }
    
    private void configurePostgreSQL(HikariConfig config) {
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
            this.config.getPostgresHost(),
            this.config.getPostgresPort(),
            this.config.getPostgresDatabase()));
        config.setUsername(this.config.getPostgresUser());
        config.setPassword(this.config.getPostgresPassword());
        
        logger.info("Configured PostgreSQL database at: " + this.config.getPostgresHost() + ":" + this.config.getPostgresPort());
    }
    
    /**
     * Gets the configured DataSource.
     */
    public DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not initialized. Call initialize() first.");
        }
        return dataSource;
    }
    
    /**
     * Closes the connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
}