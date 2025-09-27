package com.example.quickstocks.infrastructure.db;

/**
 * Configuration holder for database settings.
 * Supports SQLite, MySQL, and PostgreSQL configurations.
 */
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
    
    // Getters and setters
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getSqliteFile() {
        return sqliteFile;
    }
    
    public void setSqliteFile(String sqliteFile) {
        this.sqliteFile = sqliteFile;
    }
    
    public String getMysqlHost() {
        return mysqlHost;
    }
    
    public void setMysqlHost(String mysqlHost) {
        this.mysqlHost = mysqlHost;
    }
    
    public int getMysqlPort() {
        return mysqlPort;
    }
    
    public void setMysqlPort(int mysqlPort) {
        this.mysqlPort = mysqlPort;
    }
    
    public String getMysqlDatabase() {
        return mysqlDatabase;
    }
    
    public void setMysqlDatabase(String mysqlDatabase) {
        this.mysqlDatabase = mysqlDatabase;
    }
    
    public String getMysqlUser() {
        return mysqlUser;
    }
    
    public void setMysqlUser(String mysqlUser) {
        this.mysqlUser = mysqlUser;
    }
    
    public String getMysqlPassword() {
        return mysqlPassword;
    }
    
    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }
    
    public boolean isMysqlUseSSL() {
        return mysqlUseSSL;
    }
    
    public void setMysqlUseSSL(boolean mysqlUseSSL) {
        this.mysqlUseSSL = mysqlUseSSL;
    }
    
    public String getPostgresHost() {
        return postgresHost;
    }
    
    public void setPostgresHost(String postgresHost) {
        this.postgresHost = postgresHost;
    }
    
    public int getPostgresPort() {
        return postgresPort;
    }
    
    public void setPostgresPort(int postgresPort) {
        this.postgresPort = postgresPort;
    }
    
    public String getPostgresDatabase() {
        return postgresDatabase;
    }
    
    public void setPostgresDatabase(String postgresDatabase) {
        this.postgresDatabase = postgresDatabase;
    }
    
    public String getPostgresUser() {
        return postgresUser;
    }
    
    public void setPostgresUser(String postgresUser) {
        this.postgresUser = postgresUser;
    }
    
    public String getPostgresPassword() {
        return postgresPassword;
    }
    
    public void setPostgresPassword(String postgresPassword) {
        this.postgresPassword = postgresPassword;
    }
}