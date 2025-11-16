# Testing Strategy - Copilot Instructions

## Overview
QuickStocks uses a comprehensive testing approach combining automated unit tests with MockBukkit and manual integration testing on Minecraft servers.

## Test Infrastructure

### Framework
- **JUnit 5** - Test framework
- **MockBukkit** - Bukkit API mocking
- **Maven Surefire** - Test execution
- **GitHub Actions** - CI/CD with automated testing

### Test Location
```
src/test/java/net/cyberneticforge/quickstocks/
├── TestBase.java                    # Base test class with MockBukkit setup
└── core/services/
    ├── FeeServiceTest.java          # Trading fee calculations
    ├── WalletServiceTest.java       # Wallet operations
    ├── HoldingsServiceTest.java     # Portfolio management
    ├── TradingServiceTest.java      # Trading logic
    └── CompanyServiceTest.java      # Company operations
```

## Current Test Coverage

### ✅ Tested Components (78 test cases)

**FeeServiceTest** - Trading fee calculations
- Fee calculation with different percentages
- Minimum fee enforcement
- Zero value handling
- Configuration-based fees

**WalletServiceTest** - Wallet operations
- Get/set balance operations
- Add balance functionality
- Remove balance with sufficient/insufficient funds
- Balance validation
- Vault integration fallback

**HoldingsServiceTest** - Portfolio management
- Add holdings with average price calculation
- Remove holdings (full and partial)
- Get holdings for player and instrument
- Portfolio value calculation
- Insufficient holdings error handling

**TradingServiceTest** - Trading logic
- Buy orders with sufficient funds
- Buy orders with insufficient funds
- Sell orders with sufficient holdings
- Sell orders with insufficient holdings
- Fee integration in trades
- Holdings updates after trades

**CompanyServiceTest** - Company operations
- Company creation with cost deduction
- Employee management (add/remove)
- Permission checking
- Financial operations (deposit/withdraw)
- Job assignment

### ⚠️ Manual Testing Required

**GUI Components:**
- MarketGUI interactions
- CompanySettingsGUI functionality
- PlotEditGUI features
- Click event handling
- Visual display validation

**Commands:**
- Command syntax validation
- Tab completion
- Permission checking
- Error message display
- Multi-step command flows

**Integrations:**
- Vault economy integration
- ChestShop shop management
- WorldGuard region creation
- Plugin load order

**Database Migrations:**
- Schema version tracking
- Migration execution
- Rollback handling
- Multi-provider compatibility

**Market Simulation:**
- Price updates over time
- Circuit breaker triggers
- Market events
- Performance under load

## Test Structure

### Base Test Class
**Location:** `src/test/java/.../TestBase.java`

```java
public abstract class TestBase {
    protected static ServerMock server;
    protected PlayerMock player;
    protected Db database;
    
    @BeforeAll
    public static void setUpServer() {
        server = MockBukkit.mock();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        player = server.addPlayer();
        
        // Setup in-memory database
        database = new Db(DataSourceProvider.createSQLiteDataSource(":memory:"));
        
        // Run migrations
        MigrationRunner migrationRunner = new MigrationRunner(database);
        migrationRunner.runMigrations();
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }
    
    @AfterAll
    public static void tearDownServer() {
        MockBukkit.unmock();
    }
}
```

### Test Pattern Example
```java
@Test
void testAddBalance() throws SQLException {
    // Given - Setup test data
    String playerUuid = player.getUniqueId().toString();
    double initialBalance = 1000.0;
    walletService.setBalance(playerUuid, initialBalance);
    
    // When - Execute action
    walletService.addBalance(playerUuid, 500.0);
    
    // Then - Verify results
    double finalBalance = walletService.getBalance(playerUuid);
    assertEquals(1500.0, finalBalance, 0.01);
}

@Test
void testRemoveBalance_InsufficientFunds() throws SQLException {
    // Given
    String playerUuid = player.getUniqueId().toString();
    walletService.setBalance(playerUuid, 100.0);
    
    // When
    boolean success = walletService.removeBalance(playerUuid, 200.0);
    
    // Then
    assertFalse(success);
    assertEquals(100.0, walletService.getBalance(playerUuid), 0.01);
}
```

## Running Tests

### Maven Commands
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest

# Run specific test method
mvn test -Dtest=WalletServiceTest#testAddBalance

# Run with verbose output
mvn test -X

# Generate coverage report (if jacoco configured)
mvn jacoco:report
```

### CI/CD Pipeline
**Location:** `.github/workflows/test.yml`

**Triggers:**
- Every push to any branch
- Every pull request
- Manual workflow dispatch

**Steps:**
1. Checkout code
2. Setup Java 21
3. Cache Maven dependencies
4. Run tests with `mvn test`
5. Report results
6. Fail build if tests fail

## Writing New Tests

### Test Naming Convention
```java
// Pattern: test<MethodName>_<Scenario>_<ExpectedResult>
@Test
void testExecuteBuyOrder_SufficientFunds_Success() { }

@Test
void testExecuteBuyOrder_InsufficientFunds_Failure() { }

@Test
void testAddHolding_NewInstrument_CreatesRecord() { }

@Test
void testAddHolding_ExistingInstrument_UpdatesQuantity() { }
```

### Given-When-Then Pattern
```java
@Test
void testFeatureScenario() throws Exception {
    // Given - Setup preconditions
    String playerUuid = "test-uuid";
    setupTestData(playerUuid);
    
    // When - Execute the action being tested
    Result result = serviceUnderTest.doAction(playerUuid, parameters);
    
    // Then - Verify the expected outcome
    assertTrue(result.isSuccess());
    assertEquals(expectedValue, result.getValue());
    verify(mockObject).methodWasCalled();
}
```

### Mocking External Dependencies
```java
@Test
void testWithMockedDependency() {
    // Create mock
    InstrumentService mockInstrumentService = mock(InstrumentService.class);
    
    // Define behavior
    when(mockInstrumentService.getInstrument(any()))
        .thenReturn(new Instrument(...));
    
    // Inject mock into service
    TradingService tradingService = new TradingService(
        database,
        mockInstrumentService,
        walletService
    );
    
    // Test
    Result result = tradingService.executeBuyOrder(...);
    
    // Verify mock was called
    verify(mockInstrumentService).getInstrument("AAPL");
}
```

### Testing Database Operations
```java
@Test
void testDatabaseOperation() throws SQLException {
    // Insert test data
    database.execute(
        "INSERT INTO companies (id, name, type, owner_uuid, balance, created_at) VALUES (?, ?, ?, ?, ?, ?)",
        "test-id", "TestCorp", "PUBLIC", "owner-uuid", 1000.0, System.currentTimeMillis()
    );
    
    // Query and verify
    Map<String, Object> company = database.querySingle(
        "SELECT * FROM companies WHERE id = ?",
        "test-id"
    );
    
    assertNotNull(company);
    assertEquals("TestCorp", company.get("name"));
    assertEquals(1000.0, (Double) company.get("balance"), 0.01);
}
```

### Testing Exceptions
```java
@Test
void testInvalidInput_ThrowsException() {
    // Verify exception is thrown
    assertThrows(IllegalArgumentException.class, () -> {
        service.methodWithValidation(invalidInput);
    });
}

@Test
void testInsufficientFunds_ReturnsError() throws SQLException {
    // Setup insufficient funds
    walletService.setBalance(playerUuid, 10.0);
    
    // Attempt expensive operation
    OrderResult result = tradingService.executeBuyOrder(
        playerUuid, "AAPL", 100, 100.0
    );
    
    // Verify failure
    assertFalse(result.success());
    assertTrue(result.message().contains("Insufficient funds"));
}
```

## Manual Testing Checklist

### Before Release
- [ ] Start test Minecraft server (Paper 1.21.8)
- [ ] Install QuickStocks plugin
- [ ] Test all commands work
- [ ] Create test company
- [ ] Execute test trades
- [ ] Verify GUI displays correctly
- [ ] Test with multiple players
- [ ] Check console for errors
- [ ] Verify database persistence
- [ ] Test plugin reload
- [ ] Test server restart

### Integration Testing
- [ ] Install Vault + economy plugin
- [ ] Verify Vault integration works
- [ ] Install ChestShop
- [ ] Create company-owned shop
- [ ] Verify revenue tracking
- [ ] Install WorldGuard
- [ ] Buy company plot
- [ ] Verify region creation
- [ ] Test employee permissions

### Performance Testing
- [ ] Add 100+ instruments to market
- [ ] Simulate 10+ active traders
- [ ] Check price update performance
- [ ] Monitor database query times
- [ ] Check memory usage over time
- [ ] Test with large trade volumes

### Edge Case Testing
- [ ] Zero/negative value trades
- [ ] Special characters in names
- [ ] Very large numbers
- [ ] Concurrent transactions
- [ ] Plugin conflicts
- [ ] Database connection loss
- [ ] Permission edge cases

## Test Guidelines

### What to Test
✅ **Business Logic:**
- Calculations (fees, prices, averages)
- Validation rules
- State transitions
- Data transformations

✅ **Service Layer:**
- CRUD operations
- Transaction handling
- Error handling
- Permission checking

✅ **Database Operations:**
- Query correctness
- Data integrity
- Migration execution
- Connection handling

### What NOT to Test
❌ **Framework Code:**
- Bukkit API behavior
- Database driver internals
- Third-party library functions

❌ **Configuration Loading:**
- YAML parsing (tested by library)
- File I/O operations

❌ **UI/Display Logic:**
- Message formatting
- Color codes
- GUI rendering

### Test Data Management
```java
// Use meaningful test data
private static final String TEST_PLAYER_UUID = "test-player-123";
private static final String TEST_COMPANY_ID = "test-company-456";
private static final String TEST_INSTRUMENT_ID = "inst-AAPL";

// Use constants for expected values
private static final double EXPECTED_FEE_RATE = 0.002;  // 0.2%
private static final double DELTA = 0.01;  // For floating point comparisons

// Clean up after tests
@AfterEach
void tearDown() throws Exception {
    database.execute("DELETE FROM holdings WHERE player_uuid = ?", TEST_PLAYER_UUID);
    database.execute("DELETE FROM companies WHERE id = ?", TEST_COMPANY_ID);
}
```

## Continuous Improvement

### Adding Tests for New Features
1. Write tests BEFORE implementing feature (TDD)
2. Test happy path first
3. Add error cases
4. Add edge cases
5. Verify with manual testing
6. Update documentation

### Test Maintenance
- Run tests before every commit
- Fix failing tests immediately
- Update tests when behavior changes
- Remove obsolete tests
- Refactor duplicated test code

### Coverage Goals
- **Target:** 70%+ coverage for service layer
- **Priority:** Business logic > Data access > Commands
- **Focus:** Critical paths (trading, finances)

## Troubleshooting

### MockBukkit Issues
- Ensure MockBukkit version matches Paper version
- Check for unsupported Bukkit APIs
- Use mocks for unavailable features

### Database Test Issues
- Use `:memory:` SQLite for fast tests
- Run migrations in @BeforeEach
- Clean data between tests
- Don't rely on execution order

### Flaky Tests
- Avoid time-dependent tests
- Mock random number generators
- Use fixed test data
- Avoid threading in tests

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Test suite details: `Documentation/TEST_SUITE.md`
- Contributing guide: `Documentation/CONTRIBUTING_TESTS.md`
