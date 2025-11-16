# Copilot Feature Documentation Index

## Overview
This directory contains comprehensive, feature-specific documentation for GitHub Copilot to provide deep context when working on QuickStocks features.

## Purpose
These documents provide detailed information about each major feature, including:
- Architecture and component organization
- Database schemas and relationships
- Configuration options
- Code patterns and examples
- Development guidelines
- Testing strategies
- Troubleshooting guides

## When to Use These Docs

### For GitHub Copilot
Copilot should consult these documents when:
- Modifying business logic for a specific feature
- Adding functionality to existing features
- Understanding data flow and dependencies
- Writing tests for a feature
- Debugging feature-specific issues
- Making architectural decisions

### For Developers
Use these docs when:
- Learning how a feature works
- Extending existing features
- Troubleshooting issues
- Writing tests
- Understanding design decisions
- Planning new features

## Feature Documentation

### Core Systems

#### 🏪 [Market & Trading System](./market-trading.md)
**Size:** 10.5KB | **Complexity:** High

**Covers:**
- Trading engine architecture
- Instrument types (stocks, crypto, items, company shares)
- Price calculation algorithms
- Fee and slippage systems
- Circuit breakers
- Market simulation
- Order execution flow

**Key Components:**
- `TradingService` / `EnhancedTradingService`
- `StockMarketService`
- `FeeService` / `SlippageService`
- `CircuitBreakerService`
- `StockPriceCalculator`

**When to Consult:**
- Adding new instrument types
- Modifying trading logic
- Implementing new fee structures
- Working with price algorithms
- Debugging trade execution

---

#### 🏢 [Company Management System](./company-management.md)
**Size:** 14.2KB | **Complexity:** High

**Covers:**
- Company creation and types
- Employee management and invitations
- Job roles and permissions
- Financial operations (deposits/withdrawals)
- Company plots (land ownership)
- Salary system
- IPO process and share trading
- Company Settings GUI

**Key Components:**
- `CompanyService`
- `InvitationService`
- `CompanyPlotService`
- `SalaryService`
- `CompanySettingsGUI`

**When to Consult:**
- Modifying company features
- Adding new permissions
- Working with employee management
- Implementing company finances
- Debugging plot ownership

---

#### ₿ [Cryptocurrency System](./crypto-system.md)
**Size:** 11.8KB | **Complexity:** Medium

**Covers:**
- Default cryptocurrency seeding
- Personal cryptocurrency creation
- Company cryptocurrency creation
- Creation costs and validation
- Symbol and name validation rules
- Crypto trading integration

**Key Components:**
- `CryptoService`
- `CryptoCommand`

**When to Consult:**
- Adding new default cryptos
- Modifying creation costs
- Working with crypto validation
- Implementing crypto features
- Debugging crypto creation

---

#### 💼 [Portfolio & Wallet System](./portfolio-wallet.md)
**Size:** 11.2KB | **Complexity:** Medium

**Covers:**
- Wallet service with Vault integration
- Balance operations
- Holdings management
- Portfolio value calculations
- Watchlist system
- Query service for analytics
- Dual mode operation (Vault vs internal)

**Key Components:**
- `WalletService`
- `HoldingsService`
- `WatchlistService`
- `QueryService`

**When to Consult:**
- Working with player balances
- Modifying holdings logic
- Implementing Vault integration
- Adding portfolio features
- Debugging wallet operations

---

### Infrastructure

#### 🗄️ [Database & Persistence Layer](./database-persistence.md)
**Size:** 13.1KB | **Complexity:** Medium

**Covers:**
- Multi-provider support (SQLite/MySQL/PostgreSQL)
- Database schema (all tables)
- Migration system and versioning
- Connection pooling with HikariCP
- Query interface (Db.java)
- Configuration loading
- Transaction management

**Key Components:**
- `DatabaseManager`
- `DataSourceProvider`
- `Db` (query interface)
- `MigrationRunner`
- `ConfigLoader`

**When to Consult:**
- Creating database migrations
- Adding new tables
- Modifying schema
- Working with queries
- Debugging database issues
- Switching database providers

---

#### 🎨 [GUI System](./gui-system.md)
**Size:** 12.9KB | **Complexity:** Medium

**Covers:**
- Market GUI (browsing and trading)
- Company Settings GUI
- Plot Edit GUI
- GUI configuration system (guis.yml)
- Item builder patterns
- Event handling for clicks
- Pagination
- Permission-aware displays

**Key Components:**
- `MarketGUI`
- `CompanySettingsGUI`
- `PlotEditGUI`
- GUI event listeners

**When to Consult:**
- Creating new GUIs
- Modifying existing GUIs
- Working with GUI configuration
- Implementing click actions
- Debugging GUI displays

---

### Integrations & Testing

#### 🔌 [Plugin Integrations](./plugin-integrations.md)
**Size:** 12.4KB | **Complexity:** Medium

**Covers:**
- Vault economy integration
- ChestShop integration (company-owned shops)
- WorldGuard integration (plot protection)
- PlaceholderAPI (planned)
- DiscordSRV (planned)
- Soft dependency patterns
- Graceful degradation

**Key Components:**
- `WalletService` (Vault integration)
- `ChestShopHook`
- `WorldGuardHook`
- Hook manager system

**When to Consult:**
- Adding new plugin integrations
- Modifying existing integrations
- Debugging integration issues
- Working with soft dependencies
- Implementing reflection-based integrations

---

#### 🧪 [Testing Strategy](./testing-strategy.md)
**Size:** 11.7KB | **Complexity:** Medium

**Covers:**
- MockBukkit test infrastructure
- JUnit 5 test patterns
- Test coverage (78 test cases)
- CI/CD pipeline
- Manual testing guidelines
- Test data management
- Given-When-Then pattern
- Mocking strategies

**Key Components:**
- `TestBase` class
- Service layer tests
- Test utilities

**When to Consult:**
- Writing new tests
- Understanding test patterns
- Setting up test data
- Debugging test failures
- Improving test coverage
- Manual testing procedures

---

## Quick Reference

### By Use Case

**Adding a New Feature:**
1. Read main [copilot-instructions.md](../../copilot-instructions.md)
2. Consult relevant feature doc(s)
3. Follow established patterns
4. Write tests (see [testing-strategy.md](./testing-strategy.md))
5. Update documentation

**Modifying Existing Feature:**
1. Consult specific feature doc
2. Understand current architecture
3. Follow established patterns
4. Update tests if needed
5. Update feature doc if behavior changes

**Debugging an Issue:**
1. Identify which feature is affected
2. Consult troubleshooting section in feature doc
3. Check common issues
4. Review code patterns
5. Check database schema if data-related

**Understanding Data Flow:**
1. Start with feature doc
2. Check database schema section
3. Review service layer patterns
4. Trace through code examples
5. Check related feature docs via cross-references

### By Component Type

| Component | Primary Doc | Secondary Docs |
|-----------|-------------|----------------|
| Commands | Feature-specific | testing-strategy.md |
| Services | Feature-specific | database-persistence.md |
| Database | database-persistence.md | Feature-specific |
| GUIs | gui-system.md | Feature-specific |
| Tests | testing-strategy.md | Feature-specific |
| Config | Feature-specific | Main copilot-instructions.md |
| Integrations | plugin-integrations.md | portfolio-wallet.md |

### Document Structure

Each feature document follows this structure:
1. **Overview** - Brief description
2. **Architecture** - Component locations and organization
3. **Key Features** - Numbered list of major capabilities
4. **Configuration** - Config file sections and examples
5. **Commands** - Command syntax and usage
6. **Events** - Event system integration
7. **Development Guidelines** - How to extend features
8. **Common Patterns** - Code examples and patterns
9. **Performance Considerations** - Optimization tips
10. **Troubleshooting** - Common issues and solutions
11. **Related Documentation** - Cross-references

## Maintenance

### Keeping Docs Updated

**When Code Changes:**
- Update feature doc if behavior changes
- Update code examples if patterns change
- Update schema if database changes
- Update configuration examples if config changes

**What to Update:**
- Architecture diagrams if structure changes
- Code examples if implementation changes
- Configuration if options change
- Troubleshooting if new issues discovered

**When to Create New Docs:**
- When adding major new feature
- When feature complexity warrants dedicated doc
- When existing doc becomes too large (>15KB)

## Contributing

### Documentation Standards
- Use clear headings and structure
- Provide code examples for patterns
- Include configuration examples
- Add troubleshooting sections
- Cross-reference related docs
- Keep examples up-to-date

### Style Guide
- Use emoji for visual markers (📋 🗄️ ⚙️)
- Use code blocks with syntax highlighting
- Use tables for structured data
- Use bullet lists for related items
- Use numbered lists for procedures
- Keep language clear and concise

## See Also

- **[Main Copilot Instructions](../../copilot-instructions.md)** - Overview and guidelines
- **[Documentation Index](../../Documentation/README.md)** - User documentation
- **[Code Quality Analysis](../../Documentation/CODE_QUALITY_ANALYSIS.md)** - Quality assessment
- **[Contributing Guide](../../Documentation/CONTRIBUTING_TESTS.md)** - How to contribute

---

**Last Updated:** 2025-11-16  
**Total Docs:** 8 feature-specific guides  
**Total Size:** ~98KB  
**Coverage:** All major features and systems
