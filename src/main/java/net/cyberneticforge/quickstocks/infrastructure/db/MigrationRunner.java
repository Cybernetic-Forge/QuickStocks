package net.cyberneticforge.quickstocks.infrastructure.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.*;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages database schema migrations with versioning.
 * Migrations are stored in resources/migrations/ and executed in order.
 */
public class MigrationRunner {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    private static final String MIGRATIONS_PATH = "/migrations/";
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.sql");
    
    private final Db db;
    
    public MigrationRunner(Db db) {
        this.db = db;
    }
    
    /**
     * Runs all pending migrations.
     */
    public void runMigrations() throws SQLException {
        logger.info("Starting database migrations...");
        
        // Ensure schema_version table exists
        createSchemaVersionTable();
        
        // Get applied migrations
        Set<Integer> appliedVersions = getAppliedVersions();
        
        // Find and execute pending migrations
        List<Migration> pendingMigrations = findPendingMigrations(appliedVersions);
        
        if (pendingMigrations.isEmpty()) {
            logger.info("No pending migrations found");
            return;
        }
        
        logger.info("Found " + pendingMigrations.size() + " pending migrations");
        
        for (Migration migration : pendingMigrations) {
            executeMigration(migration);
            appliedVersions.add(migration.version());
        }
        
        logger.info("Database migrations completed successfully");
    }
    
    private void createSchemaVersionTable() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS schema_version (
                version INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                executed_at INTEGER NOT NULL,
                success BOOLEAN NOT NULL DEFAULT TRUE
            )
            """;
        
        db.execute(createTableSQL);
        logger.debug("Schema version table ensured");
    }
    
    private Set<Integer> getAppliedVersions() throws SQLException {
        List<Map<String, Object>> results = db.query(
            "SELECT version FROM schema_version WHERE success = true ORDER BY version"
        );
        
        Set<Integer> versions = new HashSet<>();
        for (Map<String, Object> row : results) {
            Object versionObj = row.get("version");
            if (versionObj instanceof Number) {
                versions.add(((Number) versionObj).intValue());
            }
        }
        
        logger.debug("Found " + versions.size() + " applied migrations");
        return versions;
    }
    
    private List<Migration> findPendingMigrations(Set<Integer> appliedVersions) {
        List<Migration> migrations = new ArrayList<>();
        
        // In a real implementation, we'd scan the classpath for migration files
        // For now, we'll explicitly list the migrations we expect
        String[] expectedMigrations = {
            "V1__init.sql",
            "V2__holdings_orders.sql",
            "V3__trading_economy.sql",
            "V4__watchlists.sql",
            "V5__analytics_views.sql",
            "V6__portfolio_tracking.sql",
            "V7__fix_strftime_indexes.sql",
            "V8__companies.sql",
            "V9__company_market.sql",
            "V10__chestshop_permission.sql",
            "V11__employee_salaries.sql",
        };
        
        for (String filename : expectedMigrations) {
            Matcher matcher = MIGRATION_PATTERN.matcher(filename);
            if (matcher.matches()) {
                int version = Integer.parseInt(matcher.group(1));
                String name = matcher.group(2);
                
                if (!appliedVersions.contains(version)) {
                    try {
                        String sql = loadMigrationSQL(filename);
                        migrations.add(new Migration(version, name, filename, sql));
                    } catch (IOException e) {
                        logger.warning("Failed to load migration " + filename + ": " + e.getMessage());
                    }
                }
            }
        }
        
        // Sort by version
        migrations.sort(Comparator.comparingInt(Migration::version));
        
        return migrations;
    }
    
    private String loadMigrationSQL(String filename) throws IOException {
        InputStream stream = getClass().getResourceAsStream(MIGRATIONS_PATH + filename);
        if (stream == null) {
            throw new IOException("Migration file not found: " + filename);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /*
     * Cleans SQL by removing all types of comments (both line comments -- and block comments )
     * while preserving string literals and other important content.
     */
    private String cleanSqlComments(String sql) {
        StringBuilder result = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBlockComment = false;
        
        for (int i = 0; i < sql.length(); i++) {
            char currentChar = sql.charAt(i);
            char nextChar = (i + 1 < sql.length()) ? sql.charAt(i + 1) : '\0';
            
            // Handle string literals to avoid removing comments inside strings
            if (currentChar == '\'' && !inDoubleQuote && !inBlockComment) {
                inSingleQuote = !inSingleQuote;
                result.append(currentChar);
            } else if (currentChar == '"' && !inSingleQuote && !inBlockComment) {
                inDoubleQuote = !inDoubleQuote;
                result.append(currentChar);
            }
            // Handle block comments /* ... */
            else if (currentChar == '/' && nextChar == '*' && !inSingleQuote && !inDoubleQuote) {
                inBlockComment = true;
                i++; // Skip the '*'
            } else if (currentChar == '*' && nextChar == '/' && inBlockComment) {
                inBlockComment = false;
                i++; // Skip the '/'
            }
            // Handle line comments -- ...
            else if (currentChar == '-' && nextChar == '-' && !inSingleQuote && !inDoubleQuote && !inBlockComment) {
                // Skip everything until end of line
                while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\r') {
                    i++;
                }
                // Don't increment i again in the main loop
                i--;
            }
            // Add character if not in a comment
            else if (!inBlockComment) {
                result.append(currentChar);
            }
        }
        
        return result.toString();
    }
    
    private void executeMigration(Migration migration) throws SQLException {
        logger.info("Executing migration V" + migration.version() + "__" + migration.name());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Parse SQL more carefully with improved comment handling
            String sql = migration.sql();
            
            // Clean SQL by removing all types of comments
            String cleanedSql = cleanSqlComments(sql);
            
            // Split by semicolon and execute each statement
            String[] statements = cleanedSql.split(";");

            for (String s : statements) {
                String statement = s.trim();
                if (!statement.isEmpty()) {
                    logger.debug("Executing SQL statement: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    db.execute(statement);
                }
            }
            
            // Only record success after ALL statements have executed successfully
            db.execute(
                "INSERT OR REPLACE INTO schema_version (version, name, executed_at, success) VALUES (?, ?, ?, ?)",
                migration.version(),
                migration.name(),
                System.currentTimeMillis(),
                true
            );
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Migration V" + migration.version() + "__" + migration.name() +
                       " completed successfully in " + duration + "ms");
            
        } catch (SQLException e) {
            // Record failed migration - use INSERT OR REPLACE to handle retries
            try {
                db.execute(
                    "INSERT OR REPLACE INTO schema_version (version, name, executed_at, success) VALUES (?, ?, ?, ?)",
                    migration.version(),
                    migration.name(),
                    System.currentTimeMillis(),
                    false
                );
            } catch (SQLException recordError) {
                logger.warning("Failed to record migration failure: " + recordError.getMessage());
            }
            
            throw new SQLException("Migration V" + migration.version() + "__" + migration.name() + " failed", e);
        }
    }
    
    /**
     * Gets the current schema version.
     */
    public int getCurrentVersion() throws SQLException {
        Integer version = db.queryValue("SELECT MAX(version) FROM schema_version WHERE success = true");
        return version != null ? version : 0;
    }

    /**
         * Represents a database migration.
         */
        private record Migration(int version, String name, String filename, String sql) {

    }
}