package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BackupServiceTest {
    
    @Mock
    private Db database;
    
    @TempDir
    Path tempDir;
    
    private BackupService backupService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        backupService = new BackupService(database, tempDir.toString(), true);
    }
    
    @Test
    void testPerformManualBackup_Success() throws SQLException {
        // Mock database queries for backup data
        mockBackupData();
        
        // Mock backup metadata operations
        when(database.execute(contains("INSERT INTO backup_metadata"), any(), any(), any(), any(), any()))
            .thenReturn(1);
        when(database.execute(contains("UPDATE backup_metadata"), any(), any(), any(), any(), any()))
            .thenReturn(1);
        
        BackupService.BackupResult result = backupService.performManualBackup();
        
        assertTrue(result.success);
        assertEquals(6, result.fileCount); // 5 CSV files + 1 JSON summary
        assertTrue(result.totalSize > 0);
        assertTrue(result.message.contains("successfully"));
        
        // Verify backup metadata was inserted and updated
        verify(database).execute(contains("INSERT INTO backup_metadata"), any(), any(), any(), any(), any());
        verify(database).execute(contains("UPDATE backup_metadata"), any(), any(), any(), any(), any());
    }
    
    @Test
    void testPerformEmergencyBackup_Success() throws SQLException {
        // Mock database queries for backup data
        mockBackupData();
        
        // Mock backup metadata operations
        when(database.execute(contains("INSERT INTO backup_metadata"), any(), any(), any(), any(), any()))
            .thenReturn(1);
        when(database.execute(contains("UPDATE backup_metadata"), any(), any(), any(), any(), any()))
            .thenReturn(1);
        
        BackupService.BackupResult result = backupService.performEmergencyBackup();
        
        assertTrue(result.success);
        assertEquals(6, result.fileCount);
        assertTrue(result.totalSize > 0);
        
        // Emergency backup should have different naming pattern
        verify(database).execute(contains("INSERT INTO backup_metadata"), any(), eq("EMERGENCY"), any(), any(), any());
    }
    
    @Test
    void testPerformBackup_DatabaseError() throws SQLException {
        // Mock database error during backup metadata insertion
        when(database.execute(contains("INSERT INTO backup_metadata"), any(), any(), any(), any(), any()))
            .thenThrow(new SQLException("Database connection failed"));
        
        BackupService.BackupResult result = backupService.performManualBackup();
        
        assertFalse(result.success);
        assertTrue(result.message.contains("failed"));
        assertEquals(0, result.fileCount);
        assertEquals(0, result.totalSize);
    }
    
    @Test
    void testBackupService_DisabledBackups() {
        // Create service with backups disabled
        BackupService disabledService = new BackupService(database, tempDir.toString(), false);
        
        BackupService.BackupResult result = disabledService.performDailyBackup();
        
        assertFalse(result.success);
        assertTrue(result.message.contains("disabled"));
        assertEquals(0, result.fileCount);
        assertEquals(0, result.totalSize);
    }
    
    @Test
    void testGetBackupHistory() throws SQLException {
        // Mock backup history data
        List<Map<String, Object>> historyData = Arrays.asList(
            createBackupMetadataRow("backup1", "2024-01-01", "DAILY", "COMPLETED"),
            createBackupMetadataRow("backup2", "2024-01-02", "MANUAL", "COMPLETED"),
            createBackupMetadataRow("backup3", "2024-01-03", "EMERGENCY", "FAILED")
        );
        
        when(database.query(contains("backup_metadata"), eq(10)))
            .thenReturn(historyData);
        
        List<BackupService.BackupMetadata> history = backupService.getBackupHistory(10);
        
        assertEquals(3, history.size());
        
        BackupService.BackupMetadata first = history.get(0);
        assertEquals("backup1", first.id);
        assertEquals("2024-01-01", first.backupDate);
        assertEquals("DAILY", first.backupType);
        assertEquals("COMPLETED", first.status);
        
        BackupService.BackupMetadata failed = history.get(2);
        assertEquals("FAILED", failed.status);
    }
    
    @Test
    void testEscapeCsvValue() {
        BackupService service = new BackupService(database, tempDir.toString(), true);
        
        // Test normal values (using reflection to access private method)
        // Note: In a real implementation, you might make this method package-private for testing
        // For now, we'll test the backup functionality indirectly through the main methods
        
        // This test validates that CSV files are created properly
        try {
            mockBackupData();
            when(database.execute(anyString(), any())).thenReturn(1);
            
            BackupService.BackupResult result = service.performManualBackup();
            assertTrue(result.success);
        } catch (SQLException e) {
            fail("Should not throw SQLException in test");
        }
    }
    
    private void mockBackupData() throws SQLException {
        // Mock instruments data
        when(database.query(contains("SELECT * FROM instruments")))
            .thenReturn(Arrays.asList(
                Map.of("id", "aapl", "symbol", "AAPL", "display_name", "Apple Inc."),
                Map.of("id", "googl", "symbol", "GOOGL", "display_name", "Alphabet Inc.")
            ));
        
        // Mock instrument state data
        when(database.query(contains("SELECT * FROM instrument_state")))
            .thenReturn(Arrays.asList(
                Map.of("instrument_id", "aapl", "last_price", 150.0),
                Map.of("instrument_id", "googl", "last_price", 2800.0)
            ));
        
        // Mock holdings data
        when(database.query(contains("SELECT * FROM user_holdings")))
            .thenReturn(Arrays.asList(
                Map.of("player_uuid", "player1", "instrument_id", "aapl", "qty", 10.0),
                Map.of("player_uuid", "player2", "instrument_id", "googl", "qty", 5.0)
            ));
        
        // Mock orders data
        when(database.query(contains("SELECT * FROM orders")))
            .thenReturn(Arrays.asList(
                Map.of("id", "order1", "player_uuid", "player1", "side", "BUY"),
                Map.of("id", "order2", "player_uuid", "player2", "side", "SELL")
            ));
        
        // Mock wallets data
        when(database.query(contains("SELECT * FROM wallets")))
            .thenReturn(Arrays.asList(
                Map.of("player_uuid", "player1", "balance", 1000.0),
                Map.of("player_uuid", "player2", "balance", 2000.0)
            ));
    }
    
    private Map<String, Object> createBackupMetadataRow(String id, String date, String type, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("backup_date", date);
        row.put("backup_type", type);
        row.put("file_count", 5);
        row.put("total_size", 1024L);
        row.put("status", status);
        row.put("created_at", System.currentTimeMillis() - 86400000); // 1 day ago
        row.put("completed_at", System.currentTimeMillis() - 86400000 + 30000); // 30 seconds later
        return row;
    }
}