package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles automated data backups and snapshots for QuickStocks.
 * Creates daily CSV/JSON exports of critical data tables.
 */
public class BackupService {
    
    private static final Logger logger = Logger.getLogger(BackupService.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final Db database;
    private final String backupBasePath;
    private final boolean backupEnabled;
    
    public BackupService(Db database, String dataPath, boolean backupEnabled) {
        this.database = database;
        this.backupEnabled = backupEnabled;
        this.backupBasePath = Paths.get(dataPath, "backups").toString();
        
        // Create backup directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(backupBasePath));
            logger.info("Backup service initialized. Path: " + backupBasePath + ", Enabled: " + backupEnabled);
        } catch (IOException e) {
            logger.warning("Failed to create backup directory: " + e.getMessage());
        }
    }
    
    /**
     * Performs a daily backup of all critical data.
     * @return backup result with statistics
     */
    public BackupResult performDailyBackup() {
        if (!backupEnabled) {
            logger.fine("Backup skipped - backups are disabled");
            return new BackupResult(false, "Backups are disabled", 0, 0);
        }
        
        String today = LocalDate.now().format(DATE_FORMAT);
        return performBackup(today, "DAILY");
    }
    
    /**
     * Performs a manual backup with the current date.
     * @return backup result with statistics
     */
    public BackupResult performManualBackup() {
        String today = LocalDate.now().format(DATE_FORMAT);
        return performBackup(today, "MANUAL");
    }
    
    /**
     * Performs an emergency backup (typically called during shutdown).
     * @return backup result with statistics
     */
    public BackupResult performEmergencyBackup() {
        String today = LocalDate.now().format(DATE_FORMAT);
        return performBackup(today + "-emergency", "EMERGENCY");
    }
    
    /**
     * Performs a backup for the specified date and type.
     */
    private BackupResult performBackup(String backupDate, String backupType) {
        logger.info("Starting " + backupType.toLowerCase() + " backup for " + backupDate);
        
        String backupId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            // Create backup metadata entry
            database.execute(
                "INSERT INTO backup_metadata (id, backup_date, backup_type, status, created_at) VALUES (?, ?, ?, ?, ?)",
                backupId, backupDate, backupType, "PENDING", startTime
            );
            
            // Create backup directory for this date
            Path backupDir = Paths.get(backupBasePath, backupDate);
            Files.createDirectories(backupDir);
            
            int fileCount = 0;
            long totalSize = 0;
            
            // Backup instruments table
            BackupFileResult instrumentsResult = backupTableToCsv(backupDir, "instruments", 
                "SELECT * FROM instruments ORDER BY created_at");
            fileCount += instrumentsResult.fileCount;
            totalSize += instrumentsResult.fileSize;
            
            // Backup instrument state
            BackupFileResult stateResult = backupTableToCsv(backupDir, "instrument_state",
                "SELECT * FROM instrument_state ORDER BY instrument_id");
            fileCount += stateResult.fileCount;
            totalSize += stateResult.fileSize;
            
            // Backup user holdings
            BackupFileResult holdingsResult = backupTableToCsv(backupDir, "user_holdings",
                "SELECT * FROM user_holdings ORDER BY player_uuid, instrument_id");
            fileCount += holdingsResult.fileCount;
            totalSize += holdingsResult.fileSize;
            
            // Backup orders
            BackupFileResult ordersResult = backupTableToCsv(backupDir, "orders",
                "SELECT * FROM orders ORDER BY ts DESC");
            fileCount += ordersResult.fileCount;
            totalSize += ordersResult.fileSize;
            
            // Backup wallets
            BackupFileResult walletsResult = backupTableToCsv(backupDir, "wallets",
                "SELECT * FROM wallets ORDER BY player_uuid");
            fileCount += walletsResult.fileCount;
            totalSize += walletsResult.fileSize;
            
            // Create summary JSON file
            BackupFileResult summaryResult = createBackupSummary(backupDir, backupDate, backupType, fileCount, totalSize);
            fileCount += summaryResult.fileCount;
            totalSize += summaryResult.fileSize;
            
            // Update backup metadata
            long completedTime = System.currentTimeMillis();
            database.execute(
                "UPDATE backup_metadata SET file_count = ?, total_size = ?, status = ?, completed_at = ? WHERE id = ?",
                fileCount, totalSize, "COMPLETED", completedTime, backupId
            );
            
            logger.info(String.format("Backup completed successfully: %d files, %d bytes, %d ms",
                fileCount, totalSize, completedTime - startTime));
            
            return new BackupResult(true, "Backup completed successfully", fileCount, totalSize);
            
        } catch (Exception e) {
            logger.warning("Backup failed: " + e.getMessage());
            
            // Update backup metadata to mark as failed
            try {
                database.execute(
                    "UPDATE backup_metadata SET status = ?, completed_at = ? WHERE id = ?",
                    "FAILED", System.currentTimeMillis(), backupId
                );
            } catch (SQLException sqlE) {
                logger.warning("Failed to update backup metadata: " + sqlE.getMessage());
            }
            
            return new BackupResult(false, "Backup failed: " + e.getMessage(), 0, 0);
        }
    }
    
    /**
     * Backs up a database table to a CSV file.
     */
    private BackupFileResult backupTableToCsv(Path backupDir, String tableName, String query) throws SQLException, IOException {
        Path csvFile = backupDir.resolve(tableName + ".csv");
        
        List<Map<String, Object>> rows = database.query(query);
        
        if (rows.isEmpty()) {
            // Create empty file
            Files.createFile(csvFile);
            return new BackupFileResult(1, 0);
        }
        
        try (FileWriter writer = new FileWriter(csvFile.toFile())) {
            // Write header
            Map<String, Object> firstRow = rows.get(0);
            String header = String.join(",", firstRow.keySet());
            writer.write(header + "\n");
            
            // Write data rows
            for (Map<String, Object> row : rows) {
                StringBuilder line = new StringBuilder();
                boolean first = true;
                for (Object value : row.values()) {
                    if (!first) {
                        line.append(",");
                    }
                    
                    // Escape CSV values
                    String csvValue = escapeCsvValue(value);
                    line.append(csvValue);
                    first = false;
                }
                writer.write(line.toString() + "\n");
            }
        }
        
        long fileSize = Files.size(csvFile);
        logger.fine(String.format("Backed up table %s: %d rows, %d bytes", tableName, rows.size(), fileSize));
        
        return new BackupFileResult(1, fileSize);
    }
    
    /**
     * Creates a backup summary JSON file.
     */
    private BackupFileResult createBackupSummary(Path backupDir, String backupDate, String backupType, 
                                                int fileCount, long totalSize) throws IOException {
        Path summaryFile = backupDir.resolve("backup_summary.json");
        
        String json = String.format("""
            {
              "backup_date": "%s",
              "backup_type": "%s",
              "timestamp": %d,
              "file_count": %d,
              "total_size_bytes": %d,
              "files": [
                "instruments.csv",
                "instrument_state.csv",
                "user_holdings.csv",
                "orders.csv",
                "wallets.csv"
              ]
            }
            """, backupDate, backupType, System.currentTimeMillis(), fileCount, totalSize);
        
        Files.writeString(summaryFile, json);
        long fileSize = Files.size(summaryFile);
        
        return new BackupFileResult(1, fileSize);
    }
    
    /**
     * Escapes a value for CSV format.
     */
    private String escapeCsvValue(Object value) {
        if (value == null) {
            return "";
        }
        
        String str = value.toString();
        
        // If the value contains commas, quotes, or newlines, wrap in quotes and escape quotes
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = str.replace("\"", "\"\"");  // Escape quotes by doubling them
            return "\"" + str + "\"";
        }
        
        return str;
    }
    
    /**
     * Gets backup history from the database.
     */
    public List<BackupMetadata> getBackupHistory(int limit) throws SQLException {
        List<Map<String, Object>> rows = database.query(
            "SELECT * FROM backup_metadata ORDER BY created_at DESC LIMIT ?",
            limit
        );
        
        return rows.stream()
            .map(row -> new BackupMetadata(
                (String) row.get("id"),
                (String) row.get("backup_date"),
                (String) row.get("backup_type"),
                ((Number) row.get("file_count")).intValue(),
                ((Number) row.get("total_size")).longValue(),
                (String) row.get("status"),
                ((Number) row.get("created_at")).longValue(),
                row.get("completed_at") != null ? ((Number) row.get("completed_at")).longValue() : null
            ))
            .toList();
    }
    
    /**
     * Result of a backup operation.
     */
    public static class BackupResult {
        public final boolean success;
        public final String message;
        public final int fileCount;
        public final long totalSize;
        
        public BackupResult(boolean success, String message, int fileCount, long totalSize) {
            this.success = success;
            this.message = message;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
        }
    }
    
    /**
     * Result of backing up a single file.
     */
    private static class BackupFileResult {
        public final int fileCount;
        public final long fileSize;
        
        public BackupFileResult(int fileCount, long fileSize) {
            this.fileCount = fileCount;
            this.fileSize = fileSize;
        }
    }
    
    /**
     * Backup metadata information.
     */
    public static class BackupMetadata {
        public final String id;
        public final String backupDate;
        public final String backupType;
        public final int fileCount;
        public final long totalSize;
        public final String status;
        public final long createdAt;
        public final Long completedAt;
        
        public BackupMetadata(String id, String backupDate, String backupType, int fileCount, 
                            long totalSize, String status, long createdAt, Long completedAt) {
            this.id = id;
            this.backupDate = backupDate;
            this.backupType = backupType;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.status = status;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }
    }
}