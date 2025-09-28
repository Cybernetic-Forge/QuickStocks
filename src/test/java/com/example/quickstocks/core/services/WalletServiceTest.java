package com.example.quickstocks.core.services;

import com.example.quickstocks.infrastructure.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for WalletService to verify Vault integration and internal wallet functionality.
 * 
 * Note: These tests focus on the internal wallet system since we can't easily mock
 * the full Bukkit/Vault environment in unit tests. Integration tests would be needed
 * for full Vault testing in a Minecraft server environment.
 */
@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private Db mockDatabase;

    private WalletService walletService;
    private static final String TEST_PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @BeforeEach
    void setUp() {
        // WalletService will use internal system since Vault isn't available in unit tests
        walletService = new WalletService(mockDatabase);
    }

    @Test
    void testGetBalanceWithNoExistingBalance() throws SQLException {
        // Mock database to return null (no existing balance)
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(null);

        double balance = walletService.getBalance(TEST_PLAYER_UUID);

        assertEquals(0.0, balance, "Balance should be 0.0 for new player");
    }

    @Test
    void testGetBalanceWithExistingBalance() throws SQLException {
        // Mock database to return existing balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(150.75);

        double balance = walletService.getBalance(TEST_PLAYER_UUID);

        assertEquals(150.75, balance, "Balance should match database value");
    }

    @Test
    void testSetBalance() throws SQLException {
        double newBalance = 250.50;

        walletService.setBalance(TEST_PLAYER_UUID, newBalance);

        // Verify the execute method was called (we can't verify the exact SQL without more complex mocking)
        // In real integration tests, we would verify the database state
    }

    @Test
    void testAddBalance() throws SQLException {
        // Mock current balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(100.0);

        walletService.addBalance(TEST_PLAYER_UUID, 50.0);

        // In a real test, we would verify the final balance is 150.0
        // This test verifies the method doesn't throw exceptions
    }

    @Test
    void testRemoveBalanceWithSufficientFunds() throws SQLException {
        // Mock current balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(100.0);

        boolean result = walletService.removeBalance(TEST_PLAYER_UUID, 50.0);

        assertTrue(result, "Should successfully remove balance when sufficient funds available");
    }

    @Test
    void testRemoveBalanceWithInsufficientFunds() throws SQLException {
        // Mock current balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(25.0);

        boolean result = walletService.removeBalance(TEST_PLAYER_UUID, 50.0);

        assertFalse(result, "Should fail to remove balance when insufficient funds");
    }

    @Test
    void testHasBalanceWithSufficientFunds() throws SQLException {
        // Mock current balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(100.0);

        boolean hasBalance = walletService.hasBalance(TEST_PLAYER_UUID, 50.0);

        assertTrue(hasBalance, "Should return true when player has sufficient balance");
    }

    @Test
    void testHasBalanceWithInsufficientFunds() throws SQLException {
        // Mock current balance
        when(mockDatabase.queryValue(anyString(), any())).thenReturn(25.0);

        boolean hasBalance = walletService.hasBalance(TEST_PLAYER_UUID, 50.0);

        assertFalse(hasBalance, "Should return false when player has insufficient balance");
    }

    @Test
    void testIsUsingVault() {
        // In unit test environment, Vault won't be available
        assertFalse(walletService.isUsingVault(), "Should use internal system in unit test environment");
    }

    @Test
    void testGetEconomyProviderName() {
        String providerName = walletService.getEconomyProviderName();
        assertEquals("Internal Wallet System", providerName, "Should return internal system name when Vault not available");
    }
}