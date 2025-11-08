# QuickStocks - Copilot Instructions

## 📋 Table of Contents
- [Quick Start](#quick-start) - Essential info for getting started
- [Development Environment Setup](#development-environment-setup) - Build and dev setup
- [Architecture](#architecture) - Project structure and organization
- [Core Features Implemented](#core-features-implemented) - Current functionality
- [Development Guidelines](#development-guidelines) - Coding patterns and standards
- [Public API for Developers](#public-api-for-developers) - Events and manager interfaces
- [Plugin Integrations](#plugin-integrations) - Vault, ChestShop, WorldGuard
- [Testing Strategy](#-testing-strategy) - How to test changes
- [Troubleshooting Guide](#-troubleshooting-guide) - Common issues and solutions
- [Architecture Decisions](#architecture-decisions) - Why things are built this way

## Quick Start
QuickStocks is a Minecraft Paper plugin (version 1.21.8) that provides a comprehensive stock market and company management system.

**🎯 Critical Info for Copilot - Read This First:**
- 📦 **Package**: `net.cyberneticforge.quickstocks` (NOT `com.example.quickstocks` - this is outdated)
- ☕ **Java Version**: Java 21 required (pom.xml setting; README needs update)
- 🏗️ **Architecture**: Clean architecture with service layers (core/api/infrastructure/commands/gui)
- 🗄️ **Database**: Multi-provider (SQLite/MySQL/PostgreSQL) with schema migrations in `src/main/resources/migrations/`
- ⚙️ **Configuration**: Multi-file config system (config.yml, market.yml, trading.yml, companies.yml, guis.yml)
- 🎮 **Commands**: All 7 commands fully implemented (/stocks, /market, /company, /crypto, /wallet, /watch, /marketdevice)
- 🔌 **Soft Dependencies**: ChestShop, WorldGuard, Vault (all optional, plugin works without them)
- 🧪 **Testing**: Primarily manual on Minecraft server; limited automated tests
- 🚀 **Build**: Standard Maven; external repos may be unreachable in sandboxed environments (expected)

**When Making Changes:**
1. Use the correct package name (`net.cyberneticforge.quickstocks`)
2. Database changes require new migration files (`VX__description.sql`)
3. Configuration changes go in appropriate YAML file (not all in config.yml)
4. Follow the service layer pattern (don't bypass services to access DB directly)
5. Manual testing on Minecraft server is required for most changes
6. Update this file when making significant architectural changes

## Project Overview
QuickStocks provides players with an immersive stock trading experience based on real-world market factors and behaviors. The plugin features realistic price calculations, comprehensive market simulation, and full database persistence.

## Architecture
The project follows a **clean architecture pattern** with clear separation of concerns:

```
src/main/java/net/cyberneticforge/quickstocks/
├── QuickStocksPlugin.java          # Main plugin class
├── api/                            # Public API layer
│   ├── events/                    # Event system (market, company, wallet events)
│   └── managers/                  # Manager interfaces (market, portfolio, company)
├── commands/                       # Command handlers
│   ├── StocksCommand.java         # /stocks command
│   ├── MarketCommand.java         # /market command
│   ├── CompanyCommand.java        # /company command
│   ├── CryptoCommand.java         # /crypto command
│   ├── WalletCommand.java         # /wallet command
│   ├── WatchCommand.java          # /watch command
│   └── MarketDeviceCommand.java   # /marketdevice command
├── core/                           # Business logic layer
│   ├── services/                  # Business logic services
│   ├── model/                     # Data models
│   ├── algorithms/                # Core algorithms
│   └── enums/                     # Enumerations
├── gui/                            # GUI components
├── hooks/                          # Plugin integrations
│   └── chestshop/                 # ChestShop integration
├── infrastructure/                 # Infrastructure layer
│   ├── config/                    # Configuration management
│   ├── db/                        # Database layer
│   │   ├── DatabaseManager.java   # Central DB coordinator
│   │   ├── DataSourceProvider.java # Connection pooling
│   │   └── MigrationRunner.java   # Schema migration management
│   └── logging/                   # Logging infrastructure
│       └── PluginLogger.java      # Centralized logger with debug levels
├── listeners/                      # Event listeners
│   └── shops/                     # Shop-related listeners
└── utils/                          # Utility classes
```

## Development Environment Setup

### Prerequisites
- **Java 21+** (required by pom.xml; note: README says 17+ but pom.xml is configured for 21)
- **Maven 3.9+** for build management
- **IDE**: IntelliJ IDEA recommended (.idea folder configured)
- **Minecraft Server**: Paper 1.21.8 or compatible Spigot/Bukkit server for testing

### Build Commands
```bash
# Standard Maven build
mvn clean compile

# Run tests (if available)
mvn test

# Package for production
mvn clean package

# Note: External repositories may not be accessible in sandboxed environments
# This is expected behavior and does not indicate a project issue
```

### Common Issues & Solutions
- **External repo unreachable**: Network connectivity issues are expected in sandboxed environments; dependencies should be cached
- **Database errors**: Check SQLite file permissions (plugins/QuickStocks/data.db) or MySQL/PostgreSQL connectivity
- **Build failures**: Ensure Java 21+ and Maven 3.9+ are installed
- **Plugin not loading**: Verify plugin.yml is properly configured and all dependencies are present

### 📂 Key Files & Locations
**Configuration:**
- `src/main/resources/config.yml` - Main plugin configuration (database, logging, features)
- `src/main/resources/market.yml` - Market and analytics settings
- `src/main/resources/trading.yml` - Trading economy settings
- `src/main/resources/companies.yml` - Company system configuration
- `src/main/resources/guis.yml` - GUI layout configuration
- `src/main/resources/plugin.yml` - Minecraft plugin descriptor
- `src/main/resources/migrations/` - Database schema migrations

**Core Services:**
- `src/main/java/net/cyberneticforge/quickstocks/core/services/` - Business logic services
- `src/main/java/net/cyberneticforge/quickstocks/infrastructure/db/DatabaseManager.java` - Database coordinator
- `src/main/java/net/cyberneticforge/quickstocks/core/algorithms/` - Price calculation and algorithms

**Plugin Entry Points:**
- `src/main/java/net/cyberneticforge/quickstocks/QuickStocksPlugin.java` - Main plugin class
- `src/main/java/net/cyberneticforge/quickstocks/commands/` - Command handlers

**Build Files:**
- `pom.xml` - Maven configuration
- `.gitignore` - Git ignore rules
- `README.md` - Project overview and features

## Core Features Implemented

### 1. Database Layer & Persistence System
- **Location**: `infrastructure.db` package
- **Components**:
  - **DatabaseManager**: Central coordinator for DB initialization, migrations, and connection management
  - **ConfigLoader**: Loads database configuration from `config.yml` or properties files
  - **DatabaseConfig**: Configuration holder supporting SQLite, MySQL, and PostgreSQL
  - **DataSourceProvider**: Connection pooling and database provider abstraction
  - **Db**: Database utility wrapper with simplified query/update operations
  - **MigrationRunner**: Schema migration management with versioning support
- **Migration System**: 
  - **Location**: `src/main/resources/migrations/`
  - **Current Version**: V1 (V1__init.sql)
  - **Features**: Automatic schema versioning, rollback tracking, migration status logging

### 2. Database Schema (v1)
- **instruments** table: Core instrument registry
  - Supports multiple types: ITEM, CRYPTO, EQUITY, INDEX, FUND, CUSTOM_CRYPTO
  - Generic schema for Minecraft items, cryptocurrencies, and traditional stocks
  - Fields: id (UUID), type, symbol, display_name, mc_material, decimals, created_by, created_at
- **instrument_state** table: Current market state for fast reads
  - Real-time price and volume data
  - Performance metrics: change_1h, change_24h, volatility_24h, market_cap
  - Updated via market simulation engine
- **instrument_price_history** table: Append-only historical data
  - Complete price/volume history for analytics
  - Tracks reasons for price movements (market factors)
  - Indexed by instrument and timestamp for efficient queries

### 3. Centralized Logging System
- **Location**: `infrastructure.logging.PluginLogger`
- **Features**:
  - Configurable debug levels (0-3) via `config.yml`
  - Level 0: OFF - Only errors/warnings
  - Level 1: INFO - Basic operational messages (default)
  - Level 2: DEBUG - Detailed debug information
  - Level 3: TRACE - Very verbose tracing
  - Thread-safe for concurrent access
  - Replaces all `java.util.logging.Logger` instances
- **Usage**: `private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();`
- **Configuration Key**: `logging.debugLevel` (0-3, default: 1)
- **Documentation**: See `Documentation/Copilot-Changes/LOGGING_SYSTEM.md`

### 4. Configuration System
- **Primary Config**: `src/main/resources/config.yml`
  - `logging.debugLevel`: Debug verbosity level (0-3, default: 1)
  - `database.provider`: sqlite | mysql | postgres
  - `database.sqlite.file`: SQLite database file path
  - `database.mysql.*`: MySQL connection parameters
  - `database.postgres.*`: PostgreSQL connection parameters
  - `features.companies.enabled`: Enable/disable company system
  - `features.market.enabled`: Enable/disable market system
  - `metrics.enabled`: Enable bStats anonymous statistics

- **Market Config**: `src/main/resources/market.yml`
  - Market update intervals, circuit breakers, price limits
  - Market device configuration
  - Analytics settings

- **Trading Config**: `src/main/resources/trading.yml`
  - Trading fees, slippage settings
  - Position limits and restrictions

- **Companies Config**: `src/main/resources/companies.yml`
  - Company creation costs and types
  - Default job titles and permissions
  - IPO settings

- **GUIs Config**: `src/main/resources/guis.yml`
  - GUI layout and item configurations
  - Menu structures and navigation

### 5. Realistic Stock Price Algorithm
- **Location**: `core.algorithms.StockPriceCalculator`
- **Features**:
  - Multi-factor price calculation considering 25+ real-world market factors
  - Technical analysis (moving averages, support/resistance)
  - Momentum and trend following
  - Random market noise and occasional major events
  - Mean reversion to prevent unrealistic prices
  - Sector-specific factor weighting

### 6. Comprehensive Market Factors
- **Location**: `core.enums.MarketFactor`
- **Categories**:
  - Economic Indicators (inflation, interest rates, GDP, unemployment)
  - Market Sentiment (investor confidence, fear/greed index, media sentiment)
  - Industry Specific (sector performance, commodity prices, regulations)
  - Global Events (geopolitical events, natural disasters, pandemics)
  - Technical Factors (trading volume, liquidity, algorithmic trading)
  - Company Specific (earnings, dividends, management changes)
  - Seasonal/Cyclical patterns
  - Random Events (market manipulation, flash crashes, social media buzz)

### 7. Stock Market Service
- **Location**: `core.services.StockMarketService`
- **Capabilities**:
  - Stock registration and management
  - Real-time price updates (every 5 seconds)
  - Market statistics and analytics
  - Sector-based analysis
  - Performance tracking (top/worst performers)
  - Market sentiment calculation
  - Event simulation

### 8. Item Seeding System  
- **Location**: `application.boot.ItemSeeder`
- **Features**:
  - Automatically seeds Minecraft items as tradeable instruments
  - Maps Bukkit Material types to instrument symbols
  - Handles mock materials for testing environments
  - Configurable seeding via `market.defaultStocks` config

### 9. Testing Infrastructure
- **Status**: Limited automated test coverage
- **Approach**: Primarily manual testing on Minecraft server
- **Future**: Test infrastructure can be added in `src/test/java/net/cyberneticforge/quickstocks/`
- **Recommendation**: Create unit tests for core business logic in `core/services/` and `core/algorithms/`

### 10. Command System (Implemented)
- **Stock Commands** (`/stocks`, aliases: `stock`, `quote`):
  - View stock market information and quotes
  - Display top performing instruments
  - Get detailed information about specific instruments

- **Market Commands** (`/market`, aliases: `trade`, `trading`):
  - Browse and trade in the market
  - Buy/sell instruments
  - View portfolio and history
  - Manage watchlist

- **Company Commands** (`/company`, aliases: `corp`, `corporation`):
  - Create and manage companies
  - Invite/manage employees
  - Handle deposits and withdrawals
  - Go public and trade shares

- **Crypto Commands** (`/crypto`):
  - Create custom cryptocurrency instruments
  - Requires `quickstocks.command.crypto.create` permission

- **Wallet Commands** (`/wallet`, aliases: `money`, `balance`):
  - View and manage wallet balance
  - Admin commands for balance manipulation

- **Watch Commands** (`/watch`, aliases: `watchlist`, `wl`):
  - Add/remove instruments from watchlist
  - View watchlist and instrument details

- **Market Device** (`/marketdevice`, aliases: `mdevice`):
  - Give Market Link Device items to players
  - Requires operator permission

- **Implementation**: All command handlers are in `src/main/java/net/cyberneticforge/quickstocks/commands/`

### 11. Company/Corporation System
- **Location**: `core.services.CompanyService`, `core.services.InvitationService`
- **Database Tables**: 
  - `companies` - Company registry with name, type, owner, balance
  - `company_jobs` - Job titles/roles with permissions
  - `company_employees` - Employee memberships
  - `company_invitations` - Invitation tracking with expiration
  - `company_tx` - Transaction history (deposits/withdrawals)
- **Features**:
  - Create companies with configurable types (PRIVATE, PUBLIC, DAO)
  - Creation cost system (default: $1000, configurable)
  - Role-based permission system (CEO, CFO, EMPLOYEE)
  - Invite system with 7-day expiration
  - Shared company balance with deposit/withdraw operations
  - Transaction history tracking
  - Job title management with custom permissions
  - Employee roster management
- **Command**: `/company` (aliases: `/corp`, `/corporation`)
  - `create <name> <type>` - Create a new company
  - `info [name]` - View company details
  - `list` - List all companies
  - `invite <company> <player> <job>` - Invite a player
  - `invitations` - View pending invitations
  - `accept <id>` - Accept an invitation
  - `decline <id>` - Decline an invitation
  - `deposit <company> <amount>` - Deposit funds
  - `withdraw <company> <amount>` - Withdraw funds (requires permission)
  - `employees <company>` - List employees
  - `jobs <company>` - List job titles
  - `createjob <company> <title> <perms>` - Create custom job title
  - `assignjob <company> <player> <job>` - Assign job to employee
- **Configuration**:
  - `companies.enabled` - Enable/disable the system
  - `companies.creationCost` - Cost to create a company
  - `companies.defaultTypes` - Available company types
  - `companies.defaultJobTitles` - Default job titles created with company
  - `companies.permissionsByTitle` - Default permissions for each job title
- **Permissions**:
  - `quickstocks.company.create` - Create companies (default: true)
  - `quickstocks.company.manage` - Manage companies (default: true)

## Current Implementation Status

### ✅ Completed
- [x] Project structure and clean architecture
- [x] Database layer and persistence system
- [x] Database migrations system with schema versioning
- [x] Multi-database support (SQLite, MySQL, PostgreSQL)
- [x] Configuration system with multiple config files
- [x] Full command system implementation
  - [x] `/stocks` - Market information and quotes
  - [x] `/market` - Trading interface
  - [x] `/company` - Company management
  - [x] `/crypto` - Cryptocurrency creation
  - [x] `/wallet` - Balance management
  - [x] `/watch` - Watchlist management
  - [x] `/marketdevice` - Market Link Device
- [x] Company/Corporation system
  - [x] Company creation and management
  - [x] Employee management with roles
  - [x] IPO and share trading
  - [x] Transaction history
- [x] Market system
  - [x] Real-time price updates
  - [x] Circuit breakers
  - [x] Trading fees and slippage
  - [x] Price history tracking
- [x] GUI system for market browsing
- [x] ChestShop integration (soft dependency)
- [x] WorldGuard integration (soft dependency)
- [x] Vault economy integration
- [x] bStats anonymous statistics
- [x] Centralized logging system
- [x] Public API for developers
- [x] **Comprehensive test suite with MockBukkit (78 test cases)**
- [x] **GitHub Actions CI/CD pipeline with test validation**

### 🔄 In Progress
- [ ] Performance optimization for large player counts
- [ ] Additional market analytics features

### 📋 Planned Features
- [ ] Web dashboard for market monitoring
- [ ] REST API for external integrations
- [ ] Market maker bots for liquidity
- [ ] Advanced charting and visualization
- [ ] Economic events tied to Minecraft mechanics
- [ ] Multi-server synchronization
- [ ] Advanced company features (dividends, stock splits)
- [ ] Options and derivatives trading

## Key Design Principles

1. **Realism**: Stock prices behave similarly to real-world markets
2. **Transparency**: All market factors are visible and documented
3. **Scalability**: Architecture supports adding new features easily
4. **Testability**: Comprehensive test coverage with simulation capabilities
5. **Performance**: Efficient algorithms suitable for game server environment

## Development Guidelines

### 🚀 Essential Patterns for Copilot

**NEW FEATURES - Follow this pattern:**
1. **Service Layer**: Add business logic to `core/services/`
   ```java
   public class YourService {
       private final DatabaseManager dbManager;
       
       public YourService(DatabaseManager dbManager) {
           this.dbManager = dbManager;
       }
       
       // Service methods here
   }
   ```

2. **Database Changes**: Create migration files
   ```sql
   -- src/main/resources/migrations/VX__your_change.sql
   -- Where X is the next version number
   ALTER TABLE instruments ADD COLUMN your_field TEXT;
   ```

3. **Models**: Place in `core/model/` with proper validation
   ```java
   public class YourModel {
       // Use record classes when possible
       private final String field;
       
       public YourModel(String field) {
           if (field == null || field.isEmpty()) {
               throw new IllegalArgumentException("Field cannot be null or empty");
           }
           this.field = field;
       }
   }
   ```

4. **Configuration**: Add to appropriate config file
   - System config → `config.yml`
   - Market settings → `market.yml`
   - Trading settings → `trading.yml`
   - Company settings → `companies.yml`
   - GUI layouts → `guis.yml`

**COMMANDS - Minecraft integration:**
```java
// Pattern for new commands
public class YourCommand implements CommandExecutor, TabCompleter {
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    // Use Adventure Components for rich text
    // Inject services via constructor
    // Handle permissions properly
}
```

**LOGGING - Use centralized PluginLogger:**
```java
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

public class YourService {
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    public void doWork() {
        logger.info("Basic operational message");      // Level 1+
        logger.debug("Detailed debug information");    // Level 2+
        logger.trace("Very verbose tracing");          // Level 3+
        logger.warning("Warning message");             // Always logged
        logger.severe("Critical error");               // Always logged
    }
}
```

### 🔒 Code Standards & Anti-Patterns

**✅ DO:**
- Use constructor injection for dependencies
- Add comprehensive JavaDoc comments for public APIs
- Follow existing error handling patterns
- Maintain thread safety for concurrent operations
- Use `infrastructure.db` package for all persistence
- Create migrations for schema changes (V2__*.sql, etc.)
- Use PluginLogger instead of java.util.logging.Logger
- Use appropriate log levels (info/debug/trace for different verbosity)

**❌ DON'T:**
- Access database directly - use service layer
- Skip migration files for schema changes
- Break IoC patterns with static dependencies
- Ignore thread safety in multi-player environment
- Modify existing working functionality unnecessarily
- Duplicate code across services - use delegation or extraction instead
- Add redundant @SuppressWarnings at method level when class already has it
- Use java.util.logging.Logger directly - use PluginLogger instead

### When Working on This Project:
1. **Always update these instructions** when making significant changes
2. **Maintain the clean architecture** - new features should follow the established patterns
3. **Use the database layer** - all persistence should go through the infrastructure.db package
4. **Follow migration patterns** - schema changes require new migration files (VX__*.sql)
5. **Avoid code duplication** - delegate to existing services, extract common code to utilities
6. **Document breaking changes** in the section below
7. **Test thoroughly** - manual testing on Minecraft server is essential
8. **Consider performance impact** - this runs in a game server environment
9. **Use appropriate config files** - don't clutter config.yml with domain-specific settings
10. **Respect soft dependencies** - ChestShop and WorldGuard should be optional

### Best Practices from GitHub Copilot Guide:
- **Make small, focused changes** - Each PR should address one specific issue
- **Write clear commit messages** - Describe what changed and why
- **Update documentation** - Keep README and docs in sync with code changes
- **Test before committing** - Verify changes work on a test server
- **Use meaningful variable names** - Code should be self-documenting
- **Add comments for complex logic** - Explain the "why", not the "what"
- **Follow existing code style** - Consistency is important
- **Consider backwards compatibility** - Don't break existing configurations or data

### Code Standards:
- Use clear, descriptive variable and method names
- Add comprehensive JavaDoc comments for public APIs
- Follow existing error handling patterns
- Maintain thread safety for concurrent operations
- Use appropriate data structures for performance
- Follow DRY (Don't Repeat Yourself) principle - avoid code duplication
- Delegate to existing services rather than duplicating logic

### 🧪 Testing Strategy

**Comprehensive Test Suite with MockBukkit:**
The project now includes an automated test suite with 78 test cases covering core business logic.

**Testing Infrastructure:**
- ✅ **MockBukkit** integration for Bukkit API mocking
- ✅ **JUnit 5** test framework
- ✅ **Maven Surefire** plugin for test execution
- ✅ **GitHub Actions** CI/CD pipeline with test validation
- ✅ **78 test cases** covering:
  - Fee calculations (FeeServiceTest)
  - Wallet operations (WalletServiceTest)
  - Trading logic (TradingServiceTest)
  - Company management (CompanyServiceTest)
  - Portfolio operations (HoldingsServiceTest)

**Testing Checklist:**
- ✅ Automated unit tests for business logic (78 test cases)
- ✅ CI/CD pipeline enforces test passing
- ✅ Manual testing on Minecraft server still required for full integration
- ✅ Database migrations tested via MigrationRunner
- ⚠️ Configuration loading from config files (manual testing)
- ⚠️ Performance under load (manual testing needed)
- ⚠️ Thread safety in multi-player environments (manual testing)

**Testing Approach:**
1. **Unit Tests**: Business logic tests in `src/test/java` with MockBukkit
2. **Integration Tests**: Test on a development Minecraft server
3. **Manual Testing**: Verify commands, GUIs, and user interactions in-game
4. **Database Testing**: Verify migrations and data persistence
5. **CI/CD Testing**: Automated testing on every push/PR via GitHub Actions

**When Adding Tests:**
- Use JUnit 5 for new test classes
- Extend `TestBase` class for MockBukkit setup
- Follow Given-When-Then pattern with descriptive names
- Mock external dependencies (database, Minecraft APIs) when needed
- Test both success and failure scenarios
- Include edge cases and boundary conditions
- Focus on business logic that can be tested without Minecraft server
- Run `mvn test` before committing to ensure tests pass

**Running Tests:**
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest

# Run specific test method
mvn test -Dtest=WalletServiceTest#testAddBalance
```

See [TEST_SUITE.md](../Documentation/TEST_SUITE.md) for detailed test documentation.

## Breaking Changes Log

### Previous Development History
See `Documentation/Copilot-Changes/` for detailed implementation notes and migration guides for specific features.

### Version 1.0.0-SNAPSHOT (Current Release)
- **Date**: 2025-11-XX
- **Major Features**:
  - **NEW**: Complete market trading system
  - **NEW**: Company/Corporation management system
  - **NEW**: Full command implementation (/stocks, /market, /company, /crypto, /wallet, /watch)
  - **NEW**: GUI system for market browsing
  - **NEW**: ChestShop integration (soft dependency)
  - **NEW**: WorldGuard integration (soft dependency)
  - **NEW**: Vault economy integration
  - **NEW**: bStats anonymous statistics
  - **NEW**: Multi-file configuration system (config.yml, market.yml, trading.yml, companies.yml, guis.yml)
  - **NEW**: Public API for developers
  - **NEW**: Database migration system with schema versioning
  - **NEW**: Centralized logging system with debug levels
  - **NEW**: Circuit breakers and trading limits
  - **NEW**: Market Link Device for portable market access
- **Config Files**: config.yml, market.yml, trading.yml, companies.yml, guis.yml
- **Database**: SQLite (default), MySQL, PostgreSQL support
- **Package**: net.cyberneticforge.quickstocks
- **Java Version**: 21+ required
- **Minecraft Version**: 1.21.8 (Paper recommended)

*Add new breaking changes here when they occur, including:*
- *Date of change*
- *Description of what changed*
- *Impact on existing code*
- *Migration steps if needed*

## Architecture Decisions

### Why IoC Architecture?
- **Testability**: Easy to mock dependencies and create unit tests
- **Maintainability**: Clear separation of concerns
- **Extensibility**: New features can be added without modifying existing code
- **Flexibility**: Different implementations can be swapped easily

### Why These Market Factors?
Based on research of real-world stock market influences, covering:
- Economic fundamentals
- Psychological factors
- Technical indicators
- External events
- Company-specific news

### Why Database Layer Architecture?
- **Persistence**: Market data survives server restarts
- **Scalability**: Supports large numbers of instruments and historical data
- **Performance**: Indexed queries for fast market data retrieval
- **Analytics**: Historical data enables trend analysis and reporting
- **Multi-Server**: Database can be shared across server instances
- **Backup/Recovery**: Standard database backup procedures apply

### Why Generic Instrument Schema?
- **Flexibility**: Single schema supports stocks, crypto, items, indices
- **Extensibility**: Easy to add new instrument types
- **Consistency**: Uniform price tracking and history across all types
- **Minecraft Integration**: Items are first-class tradeable instruments
- **Future-Proof**: Supports complex financial instruments

### Why Migration System?
- **Version Control**: Schema changes are tracked and versioned
- **Safe Deployments**: Automatic schema updates prevent manual errors
- **Rollback Support**: Failed migrations are logged and can be recovered
- **Team Development**: Consistent database state across environments

## Public API for Developers

### Event System
The plugin provides a comprehensive event system for developers to hook into:

**Location**: `src/main/java/net/cyberneticforge/quickstocks/api/events/`

**Available Events**:
- **Market Events**: `MarketOpenEvent`, `MarketCloseEvent`, `InstrumentPriceUpdateEvent`, `CircuitBreakerTriggeredEvent`
- **Trading Events**: `InstrumentBuyEvent`, `ShareBuyEvent`, `ShareSellEvent`
- **Company Events**: `CompanyCreateEvent`, `CompanyIPOEvent`, `CompanyEmployeeJoinEvent`, `CompanyEmployeeLeaveEvent`
- **Wallet Events**: `WalletBalanceChangeEvent`
- **Watchlist Events**: `WatchlistAddEvent`, `WatchlistRemoveEvent`
- **Crypto Events**: `CryptoCreateEvent`

**Usage Example**:
```java
@EventHandler
public void onMarketOpen(MarketOpenEvent event) {
    // React to market opening
}
```

### Manager Interfaces
Public interfaces for accessing plugin functionality:

**Location**: `src/main/java/net/cyberneticforge/quickstocks/api/managers/`

**Available Managers**:
- `MarketManager` - Access market data and state
- `PortfolioManager` - Manage player portfolios
- `TradingManager` - Execute trades programmatically
- `CompanyManager` - Access company information
- `CryptoManager` - Manage cryptocurrency instruments

**Access Pattern**:
```java
QuickStocksPlugin plugin = (QuickStocksPlugin) Bukkit.getPluginManager().getPlugin("QuickStocks");
MarketManager marketManager = plugin.getMarketManager();
```

## Plugin Integrations

### Vault Economy (Optional)
- **Status**: ✅ Implemented
- **Purpose**: Integrate with economy plugins for wallet management
- **Automatic Detection**: WalletService detects and uses Vault if available
- **Fallback**: Built-in wallet system if Vault not present

### ChestShop (Optional Soft Dependency)
- **Status**: ✅ Implemented
- **Purpose**: Companies can own and manage chest shops
- **Features**: Employee-based shop management, revenue tracking
- **Configuration**: Automatic detection, no configuration needed

### WorldGuard (Optional Soft Dependency)
- **Status**: ✅ Implemented
- **Purpose**: Region-based permission control
- **Custom Flags**: `quickstocks-plots` flag for plot purchases
- **Graceful Degradation**: Works without WorldGuard installed

## Notes for Future Development
- Consider adding seasonal events tied to Minecraft calendar
- Implement company earnings based on player activity
- Add sector rotation based on server events
- Plan for multi-server market synchronization via shared database
- Consider real-time price feeds from external APIs for crypto/stocks
- Add advanced analytics and reporting features
- Implement dividends and stock splits for companies
- Add options and derivatives trading

## 🔧 Troubleshooting Guide

### Common Build Issues
**Problem**: Maven can't resolve external repositories
```bash
# This is expected in sandboxed environments
# Dependencies should be cached from previous builds
# Solution: Ensure dependencies are available in local Maven repository
```

**Problem**: Compilation errors
```bash
# Solution: Check Java version (requires Java 21+)
java -version
# Solution: Clean and rebuild
mvn clean compile
```

**Problem**: Database connection errors
```bash
# Solution: Verify SQLite file path in config.yml
# Solution: For MySQL/PostgreSQL, check connection parameters
# Default SQLite location: plugins/QuickStocks/data.db
```

### Runtime Issues
**Database Connection Failures:**
- Check config.yml database settings
- Verify SQLite file permissions (plugins/QuickStocks/data.db)
- For MySQL/PostgreSQL: validate connection parameters

**Market Simulation Not Working:**
- Verify StockMarketService is properly initialized
- Check if market.updateInterval is set in config.yml
- Ensure database tables are created (run migrations)

**Command Registration Failures:**
- Verify plugin.yml contains command definitions
- Check that command classes extend proper Bukkit interfaces
- Ensure proper permissions are configured

### Performance Issues
- **High CPU**: 
  - Reduce market update frequency in `market.yml`
  - Check for infinite loops in market calculations
  - Profile database queries for optimization opportunities
- **Memory leaks**: 
  - Check for unclosed database connections
  - Verify event listeners are properly unregistered
  - Monitor HikariCP connection pool usage
- **Slow queries**: 
  - Add database indexes for frequently accessed data
  - Consider query optimization in service layer
  - Check database migration logs for schema issues

### Development Workflow Issues
- **Config changes not taking effect**: Restart the server; config is loaded on startup
- **GUI not displaying correctly**: Check `guis.yml` for layout configuration
- **Commands not working**: Verify plugin.yml registration and permissions
- **Integration issues**: Check ChestShop/WorldGuard/Vault are properly loaded

### Getting Help
1. Check server logs for detailed error messages (increase `logging.debugLevel` if needed)
2. Review database migration logs in `schema_version` table
3. Validate configuration file syntax (all YAML files)
4. Check `Documentation/` folder for feature-specific guides
5. Review `Documentation/Copilot-Changes/` for implementation details
6. Verify soft dependencies (ChestShop, WorldGuard, Vault) are compatible versions