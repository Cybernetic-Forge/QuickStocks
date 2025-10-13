package net.cyberneticforge.quickstocks.infrastructure.db;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database utility class providing simplified access to database operations.
 * Handles connections, queries, and updates with proper resource management.
 */
@SuppressWarnings({"ALL", "UnusedReturnValue"})
public class Db {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    private final DataSource dataSource;
    
    public Db(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     * Gets a database connection from the pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Executes a SQL statement (INSERT, UPDATE, DELETE) and returns the number of affected rows.
     */
    public int execute(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            int result = stmt.executeUpdate();
            
            logger.debug("Executed SQL: " + sql + " (affected rows: " + result + ")");
            return result;
        }
    }
    
    /**
     * Executes a batch of SQL statements and returns the total affected rows.
     */
    public int executeBatch(String sql, List<Object[]> paramsList) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (Object[] params : paramsList) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            int totalAffected = 0;
            for (int result : results) {
                if (result >= 0) {
                    totalAffected += result;
                }
            }
            
            logger.debug("Executed batch SQL: " + sql + " (total affected rows: " + totalAffected + ")");
            return totalAffected;
        }
    }
    
    /**
     * Executes a SELECT query and returns the results as a list of maps.
     */
    public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            setParameters(stmt, params);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> results = new ArrayList<>();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    results.add(row);
                }
                
                logger.debug("Executed query: " + sql + " (returned " + results.size() + " rows)");
                return results;
            }
        }
    }
    
    /**
     * Executes a SELECT query and returns the first result as a map, or null if no results.
     */
    public Map<String, Object> queryOne(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = query(sql, params);
        return results.isEmpty() ? null : results.getFirst();
    }
    
    /**
     * Executes a SELECT query that returns a single value.
     */
    @SuppressWarnings("unchecked")
    public <T> T queryValue(String sql, Object... params) throws SQLException {
        Map<String, Object> result = queryOne(sql, params);
        if (result == null || result.isEmpty()) {
            return null;
        }
        return (T) result.values().iterator().next();
    }
    
    /**
     * Checks if a table exists in the database.
     */
    public boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }
    
    /**
     * Creates a table if it doesn't already exist.
     */
    public void createTableIfAbsent(String tableName, String createTableSQL) throws SQLException {
        if (!tableExists(tableName)) {
            execute(createTableSQL);
            logger.info("Created table: " + tableName);
        } else {
            logger.debug("Table already exists: " + tableName);
        }
    }
    
    /**
     * Executes multiple SQL statements in a transaction.
     */
    public void executeTransaction(TransactionBlock block) throws SQLException {
        try (Connection conn = getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                block.execute(new TransactionDb(conn));
                conn.commit();
                logger.debug("Transaction committed successfully");
            } catch (Exception e) {
                conn.rollback();
                logger.warning("Transaction rolled back due to error: " + e.getMessage());
                if (e instanceof SQLException) {
                    throw (SQLException) e;
                } else {
                    throw new SQLException("Transaction failed", e);
                }
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }
    
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    public Map<String, Object> queryRow(String sql, Object... params) throws SQLException {
        return queryOne(sql, params);
    }

    /**
     * Functional interface for transaction blocks.
     */
    @FunctionalInterface
    public interface TransactionBlock {
        void execute(TransactionDb db);
    }
    
    /**
     * Database interface for use within transactions.
     */
    public static class TransactionDb {
        private final Connection connection;
        
        TransactionDb(Connection connection) {
            this.connection = connection;
        }
        
        public int execute(String sql, Object... params) throws SQLException {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                return stmt.executeUpdate();
            }
        }
        
        public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        results.add(row);
                    }
                    
                    return results;
                }
            }
        }
    }
}