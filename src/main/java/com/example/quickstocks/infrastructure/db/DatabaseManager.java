package com.example.quickstocks.infrastructure.db;

import com.example.quickstocks.application.boot.ItemSeeder;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Central database manager that coordinates database initialization,
 * connection pooling, and migration management.
 */
public class DatabaseManager {
    
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());
    
    private final DatabaseConfig config;
    private DataSourceProvider dataSourceProvider;
    private Db db;
    private MigrationRunner migrationRunner;
    
    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }
    
    /**
     * Initializes the database system.
     */
    public void initialize() throws SQLException {
        logger.info("Initializing database system...");
        
        // Initialize connection pool
        dataSourceProvider = new DataSourceProvider(config);
        dataSourceProvider.initialize();
        
        // Create database utility
        db = new Db(dataSourceProvider.getDataSource());
        
        // Run migrations
        migrationRunner = new MigrationRunner(db);
        migrationRunner.runMigrations();
        
        // Create tables if absent (defensive programming)
        createTablesIfAbsent();
        
        // Run item seeder after migrations
        runItemSeeder();
        
        logger.info("Database system initialized successfully");
        logDatabaseInfo();
    }
    
    /**
     * Creates core tables if they don't exist (defensive programming).
     */
    private void createTablesIfAbsent() throws SQLException {
        // These should already be created by migrations, but this is defensive
        db.createTableIfAbsent("instruments", """
            CREATE TABLE instruments (
              id            TEXT PRIMARY KEY,
              type          TEXT NOT NULL,
              symbol        TEXT NOT NULL UNIQUE,
              display_name  TEXT NOT NULL,
              mc_material   TEXT,
              decimals      INTEGER NOT NULL DEFAULT 0,
              created_by    TEXT,
              created_at    INTEGER NOT NULL
            )
            """);
        
        db.createTableIfAbsent("instrument_state", """
            CREATE TABLE instrument_state (
              instrument_id TEXT PRIMARY KEY,
              last_price    REAL NOT NULL,
              last_volume   REAL NOT NULL DEFAULT 0,
              change_1h     REAL NOT NULL DEFAULT 0,
              change_24h    REAL NOT NULL DEFAULT 0,
              volatility_24h REAL NOT NULL DEFAULT 0,
              market_cap    REAL NOT NULL DEFAULT 0,
              updated_at    INTEGER NOT NULL,
              FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
            )
            """);
        
        db.createTableIfAbsent("instrument_price_history", """
            CREATE TABLE instrument_price_history (
              id            TEXT PRIMARY KEY,
              instrument_id TEXT NOT NULL,
              ts            INTEGER NOT NULL,
              price         REAL NOT NULL,
              volume        REAL NOT NULL DEFAULT 0,
              reason        TEXT,
              FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
            )
            """);
    }
    
    /**
     * Runs the item seeder to populate Minecraft items as instruments.
     */
    private void runItemSeeder() throws SQLException {
        logger.info("Running item seeder...");
        ItemSeeder itemSeeder = new ItemSeeder(db);
        itemSeeder.seedItems();
    }
    
    /**
     * Logs information about the database setup.
     */
    private void logDatabaseInfo() throws SQLException {
        int currentVersion = migrationRunner.getCurrentVersion();
        logger.info("Database schema version: " + currentVersion);
        logger.info("Database provider: " + config.getProvider());
        
        if ("sqlite".equals(config.getProvider())) {
            logger.info("SQLite database file: " + config.getSqliteFile());
        }
        
        // Log table counts
        try {
            Integer instrumentCount = db.queryValue("SELECT COUNT(*) FROM instruments");
            Integer stateCount = db.queryValue("SELECT COUNT(*) FROM instrument_state");
            Integer historyCount = db.queryValue("SELECT COUNT(*) FROM instrument_price_history");
            
            logger.info("Database contents: " + 
                       instrumentCount + " instruments, " + 
                       stateCount + " state records, " + 
                       historyCount + " history records");
        } catch (SQLException e) {
            logger.warning("Could not query table counts: " + e.getMessage());
        }
    }
    
    /**
     * Gets the database utility instance.
     */
    public Db getDb() {
        if (db == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return db;
    }
    
    /**
     * Gets the migration runner instance.
     */
    public MigrationRunner getMigrationRunner() {
        if (migrationRunner == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return migrationRunner;
    }
    
    /**
     * Shuts down the database system.
     */
    public void shutdown() {
        logger.info("Shutting down database system...");
        
        if (dataSourceProvider != null) {
            dataSourceProvider.close();
        }
        
        logger.info("Database system shut down");
    }
}