# Contributing to the Test Suite

## Overview

This guide explains how to add new tests to the QuickStocks test suite. The project uses MockBukkit and JUnit 5 for testing.

## Prerequisites

- Java 21+
- Maven 3.9+
- Understanding of JUnit 5 testing framework
- Familiarity with the Given-When-Then test pattern

## Project Structure

```
src/test/java/
└── net/cyberneticforge/quickstocks/
    ├── TestBase.java                     # Extend this for MockBukkit setup
    └── core/services/
        ├── FeeServiceTest.java
        ├── WalletServiceTest.java
        ├── TradingServiceTest.java
        ├── CompanyServiceTest.java
        └── HoldingsServiceTest.java
```

## Adding a New Test Class

### Step 1: Create the Test Class

Create a new test class in the appropriate package:

```java
package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YourService operations.
 * Describe what this test class validates.
 */
@DisplayName("YourService Tests")
public class YourServiceTest extends TestBase {
    
    // Your tests go here
}
```

### Step 2: Follow the Given-When-Then Pattern

Each test should follow this structure:

```java
@Test
@DisplayName("Clear description of what is being tested")
public void testMethodName() {
    // Given: Set up the initial state and conditions
    double initialValue = 100.0;
    double operationValue = 50.0;
    
    // When: Perform the action being tested
    double result = initialValue + operationValue;
    
    // Then: Assert the expected outcome
    assertEquals(150.0, result, 0.001, 
        "Descriptive message explaining what should happen");
}
```

### Step 3: Use Descriptive Names

- **Test class name**: `[ServiceName]Test.java` (e.g., `WalletServiceTest.java`)
- **Test method name**: `test[WhatIsBeingTested]` (e.g., `testAddBalance`)
- **Display name**: Clear sentence describing the test (e.g., "Add balance increases player balance")

## Test Examples

### Example 1: Testing Calculations

```java
@Test
@DisplayName("Calculate total with percentage fee")
public void testCalculateTotalWithFee() {
    // Given: A base amount and fee percentage
    double baseAmount = 1000.0;
    double feePercent = 2.5;
    
    // When: Calculating total with fee
    double fee = baseAmount * (feePercent / 100.0);
    double total = baseAmount + fee;
    
    // Then: Total should include the fee
    assertEquals(1025.0, total, 0.001,
        "Total should be base amount plus fee");
}
```

### Example 2: Testing Validations

```java
@Test
@DisplayName("Insufficient balance prevents withdrawal")
public void testWithdrawalInsufficientBalance() {
    // Given: Player has $50 balance
    double balance = 50.0;
    double withdrawAmount = 100.0;
    
    // When: Checking if withdrawal is possible
    boolean canWithdraw = balance >= withdrawAmount;
    
    // Then: Withdrawal should not be allowed
    assertFalse(canWithdraw,
        "Should not allow withdrawal exceeding balance");
}
```

### Example 3: Testing State Transitions

```java
@Test
@DisplayName("Balance updates correctly after purchase")
public void testBalanceAfterPurchase() {
    // Given: Player has $1000 balance and makes $300 purchase
    double initialBalance = 1000.0;
    double purchaseAmount = 300.0;
    
    // When: Updating balance after purchase
    double newBalance = initialBalance - purchaseAmount;
    
    // Then: Balance should decrease by purchase amount
    assertEquals(700.0, newBalance, 0.001,
        "Balance should be reduced by purchase amount");
}
```

## Best Practices

### DO:

✅ **Use descriptive test names**
```java
@DisplayName("Buy order fails when player has insufficient balance")
public void testBuyOrderInsufficientBalance() { ... }
```

✅ **Test edge cases**
```java
@Test
@DisplayName("Zero balance handled correctly")
public void testZeroBalance() { ... }
```

✅ **Include clear assertion messages**
```java
assertEquals(expected, actual, 0.001, 
    "Clear message explaining what should happen");
```

✅ **Test both success and failure scenarios**
```java
testAddBalanceSuccess()
testAddBalanceFailure()
```

✅ **Use appropriate delta for floating-point comparisons**
```java
assertEquals(10.0, result, 0.001, "Message");  // Good
assertEquals(10.0, result, "Message");         // May fail due to precision
```

### DON'T:

❌ **Don't test multiple unrelated things in one test**
```java
// Bad: Testing multiple operations
@Test
public void testEverything() {
    testAddBalance();
    testRemoveBalance();
    testGetBalance();
}
```

❌ **Don't use hard-coded magic numbers without explanation**
```java
// Bad: What does 42 mean?
assertEquals(42, result);

// Good: Explain the value
double expectedFee = 10000.0 * 0.0025; // 0.25% of $10,000
assertEquals(25.0, expectedFee, 0.001);
```

❌ **Don't depend on test execution order**
```java
// Bad: Depends on previous test state
@Test
public void test1() { balance = 100; }

@Test
public void test2() { 
    // Assumes test1 ran first
    assertEquals(100, balance); 
}
```

## Running Your Tests

### Run all tests
```bash
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=YourServiceTest
```

### Run specific test method
```bash
mvn test -Dtest=YourServiceTest#testMethodName
```

### Run with verbose output
```bash
mvn test -X
```

## Debugging Tests

### Use descriptive failure messages
```java
assertEquals(expected, actual, 0.001,
    "Expected balance to be $150 after adding $50 to $100");
```

### Print debug information (remove before committing)
```java
System.out.println("Debug: balance = " + balance);
```

### Use IDE debugging
- Set breakpoints in your test
- Run test in debug mode
- Step through code to identify issues

## Test Coverage Goals

Aim to cover:
- ✅ Happy path (normal successful operation)
- ✅ Edge cases (boundary values, empty inputs)
- ✅ Error cases (invalid inputs, insufficient resources)
- ✅ State transitions (before/after operations)

## Common Patterns

### Testing Business Logic Calculations
```java
// Setup
double value1 = 100.0;
double value2 = 50.0;

// Execute
double result = value1 * value2;

// Verify
assertEquals(5000.0, result, 0.001);
```

### Testing Validation Logic
```java
// Setup
String input = "";

// Execute
boolean isValid = input != null && !input.trim().isEmpty();

// Verify
assertFalse(isValid, "Empty input should be invalid");
```

### Testing Range Checks
```java
// Setup
int quantity = 150;
int maxQuantity = 100;

// Execute
boolean isInRange = quantity <= maxQuantity;

// Verify
assertFalse(isInRange, "Quantity exceeds maximum");
```

## What Not to Test

These tests focus on **business logic**, not infrastructure:
- ❌ Database queries (requires database setup)
- ❌ Bukkit API integration (requires Bukkit server)
- ❌ Network operations (requires network)
- ❌ File I/O operations (requires file system)

## Continuous Integration

All tests must pass before code can be merged:
- Tests run automatically on push to main/develop
- Tests run automatically on pull requests
- Build fails if any test fails
- No merge until all tests pass

## Getting Help

- Check [TEST_SUITE.md](TEST_SUITE.md) for detailed documentation
- Look at existing tests for examples
- Ask questions in GitHub issues or pull requests
- Reference [JUnit 5 documentation](https://junit.org/junit5/docs/current/user-guide/)

## Checklist for New Tests

Before submitting your tests:

- [ ] Test class extends `TestBase` if needed
- [ ] Test methods have `@Test` annotation
- [ ] Test methods have `@DisplayName` annotation
- [ ] Each test follows Given-When-Then pattern
- [ ] Test names are descriptive
- [ ] Assertion messages are clear
- [ ] Tests cover success and failure cases
- [ ] Tests include edge cases
- [ ] Tests run successfully: `mvn test`
- [ ] No debug print statements left in code
- [ ] Tests are independent (no execution order dependency)

## Example Test Class Template

```java
package net.cyberneticforge.quickstocks.core.services;

import net.cyberneticforge.quickstocks.TestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for [ServiceName] operations.
 * [Describe what this test class validates]
 */
@DisplayName("[ServiceName] Tests")
public class YourServiceTest extends TestBase {
    
    @Test
    @DisplayName("Description of what is tested")
    public void testFeatureName() {
        // Given: Initial state
        double initialValue = 100.0;
        
        // When: Action performed
        double result = initialValue * 2;
        
        // Then: Expected outcome
        assertEquals(200.0, result, 0.001,
            "Result should be double the initial value");
    }
    
    @Test
    @DisplayName("Description of edge case")
    public void testEdgeCase() {
        // Test edge case logic
    }
    
    @Test
    @DisplayName("Description of error case")
    public void testErrorCase() {
        // Test error handling logic
    }
}
```

## Contributing

When adding tests:
1. Fork the repository
2. Create a feature branch
3. Write your tests following these guidelines
4. Run tests locally: `mvn test`
5. Commit with descriptive message
6. Create a pull request
7. Wait for CI/CD pipeline to pass
8. Address any review feedback

Thank you for contributing to QuickStocks testing! 🧪✨
