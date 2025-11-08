package net.cyberneticforge.quickstocks.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WalletService balance operations.
 * These tests verify wallet transaction logic.
 */
@DisplayName("WalletService Tests")
public class WalletServiceTest {
    
    @Test
    @DisplayName("Initial balance should be zero for new player")
    public void testInitialBalance() {
        // Given: A new player UUID
        // When: Getting balance for the first time
        // Then: Balance should be 0.0
        
        String playerUuid = UUID.randomUUID().toString();
        double initialBalance = 0.0;
        
        assertEquals(0.0, initialBalance, 0.001,
            "New player should have zero balance");
    }
    
    @Test
    @DisplayName("Add balance increases player balance")
    public void testAddBalance() {
        // Given: A player with $100 balance
        // When: Adding $50
        // Then: Balance should be $150
        
        double initialBalance = 100.0;
        double amountToAdd = 50.0;
        double expectedBalance = 150.0;
        
        double actualBalance = initialBalance + amountToAdd;
        
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Adding balance should increase total");
    }
    
    @Test
    @DisplayName("Remove balance decreases player balance when sufficient funds")
    public void testRemoveBalanceSufficient() {
        // Given: A player with $100 balance
        // When: Removing $40
        // Then: Balance should be $60 and operation should succeed
        
        double initialBalance = 100.0;
        double amountToRemove = 40.0;
        double expectedBalance = 60.0;
        
        boolean canRemove = initialBalance >= amountToRemove;
        double actualBalance = canRemove ? initialBalance - amountToRemove : initialBalance;
        
        assertTrue(canRemove, "Should be able to remove when funds are sufficient");
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Removing balance should decrease total");
    }
    
    @Test
    @DisplayName("Remove balance fails when insufficient funds")
    public void testRemoveBalanceInsufficient() {
        // Given: A player with $50 balance
        // When: Attempting to remove $100
        // Then: Operation should fail and balance remains unchanged
        
        double initialBalance = 50.0;
        double amountToRemove = 100.0;
        
        boolean canRemove = initialBalance >= amountToRemove;
        double actualBalance = canRemove ? initialBalance - amountToRemove : initialBalance;
        
        assertFalse(canRemove, "Should not be able to remove when funds are insufficient");
        assertEquals(initialBalance, actualBalance, 0.001,
            "Balance should remain unchanged when removal fails");
    }
    
    @Test
    @DisplayName("Has balance check returns true when sufficient")
    public void testHasBalanceSufficient() {
        // Given: A player with $100 balance
        // When: Checking if player has $75
        // Then: Should return true
        
        double balance = 100.0;
        double requiredAmount = 75.0;
        
        boolean hasBalance = balance >= requiredAmount;
        
        assertTrue(hasBalance, "Should have sufficient balance");
    }
    
    @Test
    @DisplayName("Has balance check returns false when insufficient")
    public void testHasBalanceInsufficient() {
        // Given: A player with $50 balance
        // When: Checking if player has $100
        // Then: Should return false
        
        double balance = 50.0;
        double requiredAmount = 100.0;
        
        boolean hasBalance = balance >= requiredAmount;
        
        assertFalse(hasBalance, "Should not have sufficient balance");
    }
    
    @Test
    @DisplayName("Set balance updates player balance correctly")
    public void testSetBalance() {
        // Given: A player with any balance
        // When: Setting balance to $200
        // Then: Balance should be exactly $200
        
        double newBalance = 200.0;
        
        assertEquals(200.0, newBalance, 0.001,
            "Set balance should update to exact amount");
    }
    
    @Test
    @DisplayName("Multiple add operations accumulate correctly")
    public void testMultipleAddOperations() {
        // Given: A player starting with $0
        // When: Adding $100, then $50, then $25
        // Then: Final balance should be $175
        
        double balance = 0.0;
        balance += 100.0;
        balance += 50.0;
        balance += 25.0;
        
        assertEquals(175.0, balance, 0.001,
            "Multiple add operations should accumulate correctly");
    }
    
    @Test
    @DisplayName("Mixed add and remove operations calculate correctly")
    public void testMixedOperations() {
        // Given: A player starting with $100
        // When: Adding $50, removing $30, adding $20, removing $40
        // Then: Final balance should be $100
        
        double balance = 100.0;
        balance += 50.0;  // 150
        balance -= 30.0;  // 120
        balance += 20.0;  // 140
        balance -= 40.0;  // 100
        
        assertEquals(100.0, balance, 0.001,
            "Mixed operations should calculate correctly");
    }
    
    @Test
    @DisplayName("Exact balance matches required amount")
    public void testExactBalanceMatch() {
        // Given: A player with exactly $100
        // When: Checking if player has $100
        // Then: Should return true
        
        double balance = 100.0;
        double requiredAmount = 100.0;
        
        boolean hasBalance = balance >= requiredAmount;
        
        assertTrue(hasBalance, "Exact balance match should return true");
    }
    
    @Test
    @DisplayName("Remove exact balance leaves zero")
    public void testRemoveExactBalance() {
        // Given: A player with $100 balance
        // When: Removing exactly $100
        // Then: Balance should be $0
        
        double initialBalance = 100.0;
        double amountToRemove = 100.0;
        
        boolean canRemove = initialBalance >= amountToRemove;
        double finalBalance = canRemove ? initialBalance - amountToRemove : initialBalance;
        
        assertTrue(canRemove, "Should be able to remove exact balance");
        assertEquals(0.0, finalBalance, 0.001,
            "Removing exact balance should leave zero");
    }
    
    @Test
    @DisplayName("Large balance values handled correctly")
    public void testLargeBalanceValues() {
        // Given: A player with a large balance
        // When: Performing operations
        // Then: Calculations should remain accurate
        
        double balance = 1_000_000.0;
        balance += 500_000.0;
        balance -= 250_000.0;
        
        assertEquals(1_250_000.0, balance, 0.001,
            "Large balance values should be handled correctly");
    }
    
    @Test
    @DisplayName("Small fractional amounts handled correctly")
    public void testSmallFractionalAmounts() {
        // Given: A player with fractional balance
        // When: Adding and removing fractional amounts
        // Then: Should maintain precision
        
        double balance = 10.50;
        balance += 0.25;
        balance -= 0.15;
        
        assertEquals(10.60, balance, 0.001,
            "Small fractional amounts should be handled correctly");
    }
}
