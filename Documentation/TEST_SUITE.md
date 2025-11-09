# QuickStocks Test Suite Documentation

## Overview

This document describes the automated test suite for QuickStocks, implemented using MockBukkit and JUnit 5.

## Test Framework

- **Testing Framework**: JUnit 5 (Jupiter)
- **Mocking Framework**: MockBukkit v3.134.1
- **Build Tool**: Maven with maven-surefire-plugin
- **Java Version**: Java 21

## Test Structure

```
src/test/java/
└── net/cyberneticforge/quickstocks/
    ├── TestBase.java                          # Base test class with MockBukkit setup
    └── core/services/
        ├── FeeServiceTest.java                # Fee calculation tests
        ├── WalletServiceTest.java             # Balance operation tests
        ├── TradingServiceTest.java            # Trading transaction tests
        ├── CompanyServiceTest.java            # Company management tests
        └── HoldingsServiceTest.java           # Portfolio operation tests
```

## Test Coverage Summary

### Total Test Cases: 78

#### FeeServiceTest (8 tests)
Tests fee calculation logic for different modes:
- Percent-based fees (0.25% default)
- Flat fees
- Mixed fees (percent + flat)
- Edge cases (zero/negative notional values)
- Buy/sell fee calculations

**Key Test Cases:**
- `testCalculateFeePercent()` - Verifies 0.25% fee on $10,000 = $25
- `testCalculateFeeMixed()` - Verifies combined percentage + flat fee
- `testCalculateTotalCostWithFees()` - Buy order total cost calculation
- `testCalculateNetProceedsAfterFees()` - Sell order net proceeds calculation

#### WalletServiceTest (14 tests)
Tests wallet balance operations:
- Initial balance (zero for new players)
- Add balance operations
- Remove balance (with sufficient/insufficient funds)
- Balance checks
- Set balance operations
- Mixed operations (add/remove sequences)
- Edge cases (exact balance, large values, fractional amounts)

**Key Test Cases:**
- `testAddBalance()` - $100 + $50 = $150
- `testRemoveBalanceInsufficient()` - Cannot remove $100 from $50 balance
- `testHasBalanceSufficient()` - Balance check validation
- `testMixedOperations()` - Complex sequence of add/remove operations

#### TradingServiceTest (17 tests)
Tests buy/sell order logic:
- Buy order cost calculations
- Sell order proceeds calculations
- Balance validation for buy orders
- Holdings validation for sell orders
- Fee impact on trades
- Holdings and balance updates after trades
- Edge cases (zero price, unavailable price, fractional shares)

**Key Test Cases:**
- `testBuyOrderCostCalculation()` - 100 shares × $50 = $5,000
- `testBuyOrderInsufficientBalance()` - Cannot buy $5,000 trade with $1,000 balance
- `testSellOrderInsufficientHoldings()` - Cannot sell 100 shares if only own 50
- `testBuyOrderWithFees()` - $5,000 trade + 0.25% fee = $5,012.50 total
- `testBalanceAfterBuy()` - $10,000 - $3,000 = $7,000 remaining

#### CompanyServiceTest (20 tests)
Tests company management operations:
- Company creation validation (name, type, balance)
- Company deposit/withdrawal operations
- Employee management (join, leave)
- Permission system (owner vs employee)
- Company type transitions (going public)
- Invitation system (expiration logic)
- Edge cases (name uniqueness, insufficient balance)

**Key Test Cases:**
- `testCompanyCreationSufficientBalance()` - $1,500 balance ≥ $1,000 cost
- `testCompanyWithdrawalInsufficientBalance()` - Cannot withdraw $2,000 from $1,000 balance
- `testOwnerPermissions()` - Owner has all permissions
- `testInvitationExpiration()` - Invitation expires after 7 days
- `testCompanyGoPublic()` - PRIVATE company can go public when requirements met

#### HoldingsServiceTest (19 tests)
Tests portfolio/holdings operations:
- Add holdings (new position, updating average cost)
- Remove holdings (partial, complete)
- Average cost calculations
- Unrealized P&L calculations
- Portfolio value calculations
- Edge cases (zero holdings, fractional shares, same price additions)

**Key Test Cases:**
- `testAddHoldingUpdatesAvgCost()` - 50 @ $40 + 50 @ $60 = 100 @ $50 avg
- `testUnrealizedProfitGain()` - 100 shares @ $40 avg, now $55 = $1,500 gain
- `testPortfolioTotalValue()` - Sum of all holdings at current prices
- `testAvgCostAfterPartialSell()` - Average cost remains unchanged after sell

## Running the Tests

### Via Maven

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest

# Run specific test method
mvn test -Dtest=WalletServiceTest#testAddBalance

# Run tests with verbose output
mvn test -X
```

### Via IDE

Most Java IDEs (IntelliJ IDEA, Eclipse, VS Code) can run JUnit 5 tests directly:
1. Navigate to the test class
2. Right-click on the class or method
3. Select "Run Test" or "Debug Test"

## CI/CD Integration

### GitHub Actions Workflow

The project includes a GitHub Actions workflow (`.github/workflows/build-test.yml`) that:
1. Checks out the code
2. Sets up JDK 21
3. Caches Maven dependencies
4. Compiles the project
5. **Runs all tests (fails build if tests fail)**
6. Generates test reports
7. Packages the plugin (only if tests pass)
8. Uploads the plugin artifact

### Build Status

The build pipeline requires all tests to pass before creating a distributable artifact. This ensures code quality and prevents regressions.

## Test Design Philosophy

### Focus on Business Logic

These tests focus on **business logic validation** rather than full integration tests:
- Tests verify mathematical calculations (fees, balances, P&L)
- Tests validate business rules (insufficient funds, permission checks)
- Tests check state transitions (balance updates, holdings changes)
- Tests do **not** require a running Minecraft server or database

### Expected Results Pattern

Each test follows the Given-When-Then pattern:
```java
@Test
@DisplayName("Clear description of what is tested")
public void testMethodName() {
    // Given: Initial state and conditions
    double balance = 100.0;
    double cost = 50.0;
    
    // When: Action is performed
    boolean canPurchase = balance >= cost;
    
    // Then: Assert expected result
    assertTrue(canPurchase, "Description of expected behavior");
}
```

### Advantages of This Approach

1. **Fast Execution**: Tests run in milliseconds, no server startup required
2. **Reliable**: No external dependencies (database, network, Bukkit server)
3. **Maintainable**: Clear test cases with descriptive names
4. **Regression Prevention**: Business logic changes are immediately validated
5. **Documentation**: Tests serve as examples of expected behavior

## Limitations

### What These Tests Don't Cover

- **Database Integration**: Tests don't verify SQL queries or schema
- **Bukkit API Integration**: No actual Minecraft server interactions
- **Plugin Loading**: Plugin lifecycle and initialization not tested
- **Command Execution**: Command parsing and execution not covered
- **GUI Interactions**: GUI menus and click handlers not tested

### Future Enhancements

Consider adding:
1. **Integration Tests**: Tests with actual database (H2 in-memory)
2. **MockBukkit Integration Tests**: Full plugin loading and command execution
3. **Performance Tests**: Load testing for high-volume operations
4. **End-to-End Tests**: Complete user workflows on test server

## Contributing

When adding new features:

1. **Write tests first** (TDD approach recommended)
2. **Follow existing patterns** (Given-When-Then, descriptive names)
3. **Test edge cases** (zero values, negative numbers, boundary conditions)
4. **Document complex scenarios** (add comments for non-obvious logic)
5. **Run tests before committing** (`mvn test`)

## Troubleshooting

### Tests Not Running

- Verify JDK 21 is installed: `java -version`
- Clean Maven cache: `mvn clean`
- Rebuild: `mvn clean test`

### Compilation Errors

- Check pom.xml dependencies are correct
- Ensure test files are in `src/test/java` directory
- Verify package declarations match directory structure

### CI Pipeline Failures

- Check GitHub Actions logs for details
- Verify external dependencies are accessible
- Ensure pom.xml is valid XML

## References

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [MockBukkit Documentation](https://github.com/MockBukkit/MockBukkit)
- [Maven Surefire Plugin](https://maven.apache.org/surefire/maven-surefire-plugin/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

## Contact

For questions about the test suite:
- Open an issue on GitHub
- Reference this documentation in your question
- Include test output/logs for debugging help
