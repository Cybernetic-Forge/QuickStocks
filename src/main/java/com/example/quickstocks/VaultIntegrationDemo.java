package com.example.quickstocks;

import com.example.quickstocks.core.services.WalletService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Demonstration of the Vault integration functionality.
 * This class shows how the WalletService automatically detects and uses Vault when available,
 * or falls back to the internal wallet system when Vault is not present.
 */
public class VaultIntegrationDemo {

    public static void main(String[] args) {
        System.out.println("🏦 QuickStocks Vault Integration Demo");
        System.out.println("=====================================");
        
        try {
            // Create a simple mock database for demonstration
            MockDataSource dataSource = new MockDataSource();
            com.example.quickstocks.infrastructure.db.Db database = 
                new com.example.quickstocks.infrastructure.db.Db(dataSource);
            
            // Initialize WalletService - it will automatically detect Vault availability
            System.out.println("\n1. Initializing WalletService...");
            WalletService walletService = new WalletService(database);
            
            // Show which economy system is being used
            System.out.println("✓ Economy System: " + walletService.getEconomyProviderName());
            System.out.println("✓ Using Vault: " + (walletService.isUsingVault() ? "Yes" : "No"));
            
            // Demonstrate wallet operations
            System.out.println("\n2. Demonstrating wallet operations...");
            String playerUuid = UUID.randomUUID().toString();
            System.out.println("Player UUID: " + playerUuid.substring(0, 8) + "...");
            
            // Test wallet functionality
            demonstrateWalletOperations(walletService, playerUuid);
            
            System.out.println("\n🎉 Demo completed successfully!");
            System.out.println("\nIn a real Minecraft server environment:");
            System.out.println("• If Vault plugin is installed → Uses Vault economy");
            System.out.println("• If Vault plugin is missing → Uses internal wallet system");
            System.out.println("• The transition is automatic and transparent to users");
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateWalletOperations(WalletService walletService, String playerUuid) 
            throws SQLException {
        
        // Get initial balance
        double balance = walletService.getBalance(playerUuid);
        System.out.println("Initial balance: $" + String.format("%.2f", balance));
        
        // Give player some starting money
        System.out.println("Adding $1000.00 to wallet...");
        walletService.addBalance(playerUuid, 1000.0);
        balance = walletService.getBalance(playerUuid);
        System.out.println("New balance: $" + String.format("%.2f", balance));
        
        // Simulate a purchase
        System.out.println("Attempting to purchase item for $250.00...");
        boolean purchaseSuccess = walletService.removeBalance(playerUuid, 250.0);
        if (purchaseSuccess) {
            balance = walletService.getBalance(playerUuid);
            System.out.println("✓ Purchase successful! Remaining balance: $" + String.format("%.2f", balance));
        } else {
            System.out.println("❌ Purchase failed - insufficient funds");
        }
        
        // Test insufficient funds
        System.out.println("Attempting to purchase expensive item for $2000.00...");
        boolean expensivePurchase = walletService.removeBalance(playerUuid, 2000.0);
        if (expensivePurchase) {
            System.out.println("✓ Expensive purchase successful!");
        } else {
            System.out.println("❌ Purchase failed - insufficient funds (as expected)");
        }
        
        // Check if player can afford something
        boolean canAfford = walletService.hasBalance(playerUuid, 500.0);
        System.out.println("Can afford $500.00 item: " + (canAfford ? "Yes" : "No"));
    }
    
    /**
     * Mock DataSource that doesn't actually connect to a database
     * (good enough for this demonstration).
     */
    static class MockDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            // Return null - our demo WalletService won't need actual DB operations
            // because it will use the internal memory storage for this demo
            return null;
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