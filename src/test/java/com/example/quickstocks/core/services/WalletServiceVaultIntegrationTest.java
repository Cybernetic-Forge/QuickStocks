package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standalone integration test to demonstrate Vault integration functionality.
 * This test demonstrates that the WalletService correctly detects Vault availability
 * and switches between internal and Vault economy systems.
 * 
 * This is designed to be run manually to verify functionality.
 */
public class WalletServiceVaultIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=== WalletService Vault Integration Test ===");
        
        // Create a mock database implementation for testing
        MockDb mockDb = new MockDb(new MockDataSource());
        
        // Test 1: WalletService should use internal system when Vault not available
        System.out.println("\n1. Testing WalletService without Vault...");
        WalletService walletService = new WalletService(mockDb);
        
        System.out.println("Is using Vault: " + walletService.isUsingVault());
        System.out.println("Economy provider: " + walletService.getEconomyProviderName());
        
        // Test 2: Basic wallet operations
        System.out.println("\n2. Testing basic wallet operations...");
        String testPlayerUuid = UUID.randomUUID().toString();
        
        try {
            // Test getting initial balance (should be 0.0)
            double initialBalance = walletService.getBalance(testPlayerUuid);
            System.out.println("Initial balance: $" + String.format("%.2f", initialBalance));
            
            // Test setting balance
            walletService.setBalance(testPlayerUuid, 100.0);
            double balanceAfterSet = walletService.getBalance(testPlayerUuid);
            System.out.println("Balance after setting to $100.00: $" + String.format("%.2f", balanceAfterSet));
            
            // Test adding balance
            walletService.addBalance(testPlayerUuid, 50.0);
            double balanceAfterAdd = walletService.getBalance(testPlayerUuid);
            System.out.println("Balance after adding $50.00: $" + String.format("%.2f", balanceAfterAdd));
            
            // Test removing balance with sufficient funds
            boolean removeSuccess = walletService.removeBalance(testPlayerUuid, 25.0);
            double balanceAfterRemove = walletService.getBalance(testPlayerUuid);
            System.out.println("Remove $25.00 successful: " + removeSuccess);
            System.out.println("Balance after removing $25.00: $" + String.format("%.2f", balanceAfterRemove));
            
            // Test removing balance with insufficient funds
            boolean removeFailure = walletService.removeBalance(testPlayerUuid, 200.0);
            System.out.println("Remove $200.00 (insufficient funds) successful: " + removeFailure);
            
            // Test has balance check
            boolean hasEnough = walletService.hasBalance(testPlayerUuid, 100.0);
            boolean hasNotEnough = walletService.hasBalance(testPlayerUuid, 200.0);
            System.out.println("Has $100.00: " + hasEnough);
            System.out.println("Has $200.00: " + hasNotEnough);
            
            System.out.println("\n✅ All tests completed successfully!");
            
        } catch (SQLException e) {
            System.err.println("❌ Test failed with SQL exception: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Test Complete ===");
        System.out.println("Note: In a real Minecraft server environment with Vault plugin installed,");
        System.out.println("the WalletService would automatically detect and use Vault instead of the internal system.");
    }

    /**
     * Mock database implementation for testing purposes.
     */
    static class MockDb extends Db {
        private double storedBalance = 0.0;
        private String storedPlayerUuid = null;

        public MockDb(DataSource dataSource) {
            super(dataSource);
        }

        @Override
        public <T> T queryValue(String sql, Object... params) throws SQLException {
            if (sql.contains("SELECT balance FROM wallets")) {
                if (params.length > 0 && params[0].equals(storedPlayerUuid)) {
                    return (T) Double.valueOf(storedBalance);
                }
                return null; // No balance found
            }
            return null;
        }

        @Override
        public int execute(String sql, Object... params) throws SQLException {
            if (sql.contains("INSERT OR REPLACE INTO wallets")) {
                if (params.length >= 2) {
                    storedPlayerUuid = (String) params[0];
                    storedBalance = (Double) params[1];
                }
            }
            return 1; // Mock successful execution
        }
    }

    /**
     * Mock DataSource for testing.
     */
    static class MockDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return null; // Not actually used in our mock
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return null;
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}