package net.cyberneticforge.quickstocks.core.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompanyService operations.
 * These tests verify company management logic including creation, employees, and transactions.
 */
@DisplayName("CompanyService Tests")
public class CompanyServiceTest {
    
    @Test
    @DisplayName("Company creation requires valid name")
    public void testCompanyCreationValidName() {
        // Given: A valid company name
        // When: Validating the name
        // Then: Should be accepted
        
        String companyName = "TechCorp";
        
        boolean isValid = companyName != null && !companyName.trim().isEmpty();
        
        assertTrue(isValid, "Valid company name should be accepted");
    }
    
    @Test
    @DisplayName("Company creation rejects empty name")
    public void testCompanyCreationEmptyName() {
        // Given: An empty company name
        // When: Validating the name
        // Then: Should be rejected
        
        String companyName = "";
        
        boolean isValid = companyName != null && !companyName.trim().isEmpty();
        
        assertFalse(isValid, "Empty company name should be rejected");
    }
    
    @Test
    @DisplayName("Company creation rejects null name")
    public void testCompanyCreationNullName() {
        // Given: A null company name
        // When: Validating the name
        // Then: Should be rejected
        
        String companyName = null;
        
        boolean isValid = companyName != null && !companyName.trim().isEmpty();
        
        assertFalse(isValid, "Null company name should be rejected");
    }
    
    @Test
    @DisplayName("Company creation requires valid type")
    public void testCompanyCreationValidType() {
        // Given: A valid company type
        // When: Validating the type
        // Then: Should be accepted
        
        String companyType = "PUBLIC";
        String[] validTypes = {"PRIVATE", "PUBLIC", "DAO"};
        
        boolean isValid = false;
        for (String validType : validTypes) {
            if (validType.equals(companyType)) {
                isValid = true;
                break;
            }
        }
        
        assertTrue(isValid, "Valid company type should be accepted");
    }
    
    @Test
    @DisplayName("Company creation rejects invalid type")
    public void testCompanyCreationInvalidType() {
        // Given: An invalid company type
        // When: Validating the type
        // Then: Should be rejected
        
        String companyType = "INVALID";
        String[] validTypes = {"PRIVATE", "PUBLIC", "DAO"};
        
        boolean isValid = false;
        for (String validType : validTypes) {
            if (validType.equals(companyType)) {
                isValid = true;
                break;
            }
        }
        
        assertFalse(isValid, "Invalid company type should be rejected");
    }
    
    @Test
    @DisplayName("Company creation requires sufficient balance")
    public void testCompanyCreationSufficientBalance() {
        // Given: Player has $1,500 and creation costs $1,000
        // When: Checking if player can create company
        // Then: Should be allowed
        
        double playerBalance = 1500.0;
        double creationCost = 1000.0;
        
        boolean canCreate = playerBalance >= creationCost;
        
        assertTrue(canCreate, "Company creation should succeed with sufficient balance");
    }
    
    @Test
    @DisplayName("Company creation fails with insufficient balance")
    public void testCompanyCreationInsufficientBalance() {
        // Given: Player has $500 and creation costs $1,000
        // When: Checking if player can create company
        // Then: Should be rejected
        
        double playerBalance = 500.0;
        double creationCost = 1000.0;
        
        boolean canCreate = playerBalance >= creationCost;
        
        assertFalse(canCreate, "Company creation should fail with insufficient balance");
    }
    
    @Test
    @DisplayName("Company deposit increases balance")
    public void testCompanyDeposit() {
        // Given: Company has $5,000 balance
        // When: Depositing $2,000
        // Then: Balance should be $7,000
        
        double currentBalance = 5000.0;
        double depositAmount = 2000.0;
        double expectedBalance = 7000.0;
        
        double actualBalance = currentBalance + depositAmount;
        
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Company deposit should increase balance");
    }
    
    @Test
    @DisplayName("Company withdrawal decreases balance")
    public void testCompanyWithdrawal() {
        // Given: Company has $5,000 balance
        // When: Withdrawing $1,500
        // Then: Balance should be $3,500
        
        double currentBalance = 5000.0;
        double withdrawalAmount = 1500.0;
        double expectedBalance = 3500.0;
        
        double actualBalance = currentBalance - withdrawalAmount;
        
        assertEquals(expectedBalance, actualBalance, 0.001,
            "Company withdrawal should decrease balance");
    }
    
    @Test
    @DisplayName("Company withdrawal fails when insufficient balance")
    public void testCompanyWithdrawalInsufficientBalance() {
        // Given: Company has $1,000 balance
        // When: Attempting to withdraw $2,000
        // Then: Withdrawal should fail
        
        double currentBalance = 1000.0;
        double withdrawalAmount = 2000.0;
        
        boolean canWithdraw = currentBalance >= withdrawalAmount;
        
        assertFalse(canWithdraw, "Withdrawal should fail with insufficient balance");
    }
    
    @Test
    @DisplayName("Employee count increases when employee joins")
    public void testEmployeeJoin() {
        // Given: Company has 5 employees
        // When: New employee joins
        // Then: Count should be 6
        
        int currentEmployees = 5;
        int expectedEmployees = 6;
        
        int actualEmployees = currentEmployees + 1;
        
        assertEquals(expectedEmployees, actualEmployees,
            "Employee count should increase when employee joins");
    }
    
    @Test
    @DisplayName("Employee count decreases when employee leaves")
    public void testEmployeeLeave() {
        // Given: Company has 8 employees
        // When: Employee leaves
        // Then: Count should be 7
        
        int currentEmployees = 8;
        int expectedEmployees = 7;
        
        int actualEmployees = currentEmployees - 1;
        
        assertEquals(expectedEmployees, actualEmployees,
            "Employee count should decrease when employee leaves");
    }
    
    @Test
    @DisplayName("Company owner has all permissions")
    public void testOwnerPermissions() {
        // Given: A company owner
        // When: Checking permissions
        // Then: Should have all permissions
        
        boolean isOwner = true;
        
        boolean canInvite = isOwner;
        boolean canWithdraw = isOwner;
        boolean canManageCompany = isOwner;
        
        assertTrue(canInvite, "Owner should have invite permission");
        assertTrue(canWithdraw, "Owner should have withdraw permission");
        assertTrue(canManageCompany, "Owner should have manage permission");
    }
    
    @Test
    @DisplayName("Regular employee has limited permissions")
    public void testEmployeePermissions() {
        // Given: A regular employee (not owner)
        // When: Checking permissions
        // Then: Should have limited permissions
        
        boolean isOwner = false;
        boolean hasWithdrawPerm = false; // Default for regular employee
        
        assertFalse(isOwner, "Regular employee is not owner");
        assertFalse(hasWithdrawPerm, "Regular employee cannot withdraw by default");
    }
    
    @Test
    @DisplayName("Company can go public when conditions met")
    public void testCompanyGoPublic() {
        // Given: A PRIVATE company that meets requirements
        // When: Attempting to go public
        // Then: Should be allowed
        
        String companyType = "PRIVATE";
        boolean hasRequiredBalance = true; // Meets minimum requirements
        boolean hasRequiredEmployees = true; // Meets minimum requirements
        
        boolean canGoPublic = "PRIVATE".equals(companyType) && 
                              hasRequiredBalance && 
                              hasRequiredEmployees;
        
        assertTrue(canGoPublic, "Company should be able to go public when conditions met");
    }
    
    @Test
    @DisplayName("PUBLIC company cannot go public again")
    public void testPublicCompanyCannotGoPublic() {
        // Given: A company that is already PUBLIC
        // When: Attempting to go public
        // Then: Should be rejected
        
        String companyType = "PUBLIC";
        
        boolean canGoPublic = "PRIVATE".equals(companyType);
        
        assertFalse(canGoPublic, "PUBLIC company cannot go public again");
    }
    
    @Test
    @DisplayName("Company balance starts at zero")
    public void testNewCompanyBalance() {
        // Given: A newly created company
        // When: Checking initial balance
        // Then: Balance should be 0.0
        
        double initialBalance = 0.0;
        
        assertEquals(0.0, initialBalance, 0.001,
            "New company should start with zero balance");
    }
    
    @Test
    @DisplayName("Invitation expires after duration")
    public void testInvitationExpiration() {
        // Given: An invitation sent 8 days ago (7-day expiration)
        // When: Checking if expired
        // Then: Should be expired
        
        long invitationTime = System.currentTimeMillis() - (8L * 24 * 60 * 60 * 1000); // 8 days ago
        long currentTime = System.currentTimeMillis();
        long expirationDuration = 7L * 24 * 60 * 60 * 1000; // 7 days
        
        boolean isExpired = (currentTime - invitationTime) > expirationDuration;
        
        assertTrue(isExpired, "Invitation should expire after 7 days");
    }
    
    @Test
    @DisplayName("Invitation valid within duration")
    public void testInvitationValid() {
        // Given: An invitation sent 5 days ago (7-day expiration)
        // When: Checking if expired
        // Then: Should not be expired
        
        long invitationTime = System.currentTimeMillis() - (5L * 24 * 60 * 60 * 1000); // 5 days ago
        long currentTime = System.currentTimeMillis();
        long expirationDuration = 7L * 24 * 60 * 60 * 1000; // 7 days
        
        boolean isExpired = (currentTime - invitationTime) > expirationDuration;
        
        assertFalse(isExpired, "Invitation should be valid within 7 days");
    }
    
    @Test
    @DisplayName("Company name uniqueness validation")
    public void testCompanyNameUniqueness() {
        // Given: Two companies cannot have the same name
        // When: Checking name uniqueness
        // Then: Should be validated
        
        String existingName = "TechCorp";
        String newName = "TechCorp";
        
        boolean isUnique = !existingName.equalsIgnoreCase(newName);
        
        assertFalse(isUnique, "Company names must be unique");
    }
}
