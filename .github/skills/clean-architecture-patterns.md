# Clean Architecture Patterns - QuickStocks Skill

**Domain**: Software Architecture & Design Patterns
**Last Updated**: 2026-01-28

## Overview

QuickStocks follows clean architecture principles with clear separation of concerns across layers. This skill defines the architectural patterns used throughout the codebase.

## Architecture Layers

### 1. Core Layer (`core/`)

**Purpose**: Business logic and domain models

**Components**:
- `services/` - Business logic services
- `model/` - Domain entities and value objects
- `algorithms/` - Core algorithms (price calculation, etc.)
- `enums/` - Domain enumerations

**Rules**:
- ✅ No dependencies on infrastructure or frameworks
- ✅ Pure business logic
- ✅ Framework-agnostic
- ❌ No database code
- ❌ No Minecraft API dependencies

**Example**:
```java
package net.cyberneticforge.quickstocks.core.services;

public class WalletService {
    private final DatabaseManager dbManager;
    
    public WalletService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public void addBalance(UUID playerId, double amount) {
        // Pure business logic
        // Delegates to infrastructure for persistence
    }
}
```

### 2. API Layer (`api/`)

**Purpose**: Public contracts for external integrations

**Components**:
- `events/` - Custom event system for plugin hooks
- `managers/` - Manager interfaces for external access

**Rules**:
- ✅ Stable public interfaces
- ✅ Event-driven integration points
- ❌ No implementation details
- ❌ No breaking changes without major version bump

**Example**:
```java
package net.cyberneticforge.quickstocks.api.events;

public class MarketOpenEvent extends Event {
    private final LocalDateTime openTime;
    
    // Public API for other plugins to hook into
}
```

### 3. Infrastructure Layer (`infrastructure/`)

**Purpose**: Technical implementations and external concerns

**Components**:
- `config/` - Configuration management
- `db/` - Database layer and persistence
- `logging/` - Centralized logging

**Rules**:
- ✅ Implements technical details
- ✅ Depends on core layer
- ✅ Framework and library integrations
- ❌ No business logic

**Example**:
```java
package net.cyberneticforge.quickstocks.infrastructure.db;

public class DatabaseManager {
    private final DataSourceProvider provider;
    
    public void executeQuery(String sql) {
        // Database-specific implementation
    }
}
```

### 4. Commands Layer (`commands/`)

**Purpose**: User interface via Minecraft commands

**Components**:
- Command executors for each command
- Tab completers for autocompletion

**Rules**:
- ✅ Thin layer - delegates to services
- ✅ Input validation
- ✅ Permission checks
- ✅ User feedback
- ❌ No business logic

**Example**:
```java
package net.cyberneticforge.quickstocks.commands;

public class MarketCommand implements CommandExecutor {
    private final MarketService marketService;
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Validate input
        // Check permissions
        // Delegate to service
        // Send response to user
        return true;
    }
}
```

### 5. GUI Layer (`gui/`)

**Purpose**: Visual interfaces for players

**Components**:
- GUI builders and handlers
- Menu systems

**Rules**:
- ✅ Delegates to services
- ✅ Event handling for clicks
- ❌ No business logic

### 6. Listeners Layer (`listeners/`)

**Purpose**: React to Minecraft events

**Components**:
- Event listeners for game events

**Rules**:
- ✅ Delegates to services
- ❌ No business logic

## Dependency Flow

```
Commands  ────┐
GUI       ────┤
Listeners ────┼──> Core Services ──> Infrastructure
              │
API       ────┘
```

**Key Principle**: Dependencies flow inward, never outward.

## Service Layer Patterns

### Constructor Injection

```java
public class TradingService {
    private final DatabaseManager dbManager;
    private final WalletService walletService;
    private final FeeService feeService;
    
    public TradingService(DatabaseManager dbManager, 
                         WalletService walletService,
                         FeeService feeService) {
        this.dbManager = dbManager;
        this.walletService = walletService;
        this.feeService = feeService;
    }
}
```

### Service Initialization in Plugin

```java
public class QuickStocksPlugin extends JavaPlugin {
    private WalletService walletService;
    private TradingService tradingService;
    
    @Override
    public void onEnable() {
        DatabaseManager dbManager = new DatabaseManager(config);
        
        // Create services in dependency order
        this.walletService = new WalletService(dbManager);
        this.feeService = new FeeService();
        this.tradingService = new TradingService(dbManager, walletService, feeService);
        
        // Register commands with services
        getCommand("market").setExecutor(new MarketCommand(tradingService));
    }
}
```

## Data Flow Patterns

### Request Flow

1. **User Input** → Command/GUI
2. **Validation** → Command validates input
3. **Permission Check** → Command checks permissions
4. **Business Logic** → Service layer processes request
5. **Persistence** → Infrastructure layer saves to database
6. **Response** → Command sends result to user

### Example Flow

```java
// 1. User executes: /market buy DIAMOND 100
// 2. MarketCommand.onCommand() receives request

public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    // 3. Validate input
    if (args.length < 3) {
        sender.sendMessage("Usage: /market buy <symbol> <quantity>");
        return true;
    }
    
    // 4. Check permission
    if (!sender.hasPermission("quickstocks.market.buy")) {
        Translation.NoPermission.sendMessage(sender);
        return true;
    }
    
    Player player = (Player) sender;
    String symbol = args[1];
    int quantity = Integer.parseInt(args[2]);
    
    // 5. Delegate to service
    try {
        tradingService.buyInstrument(player.getUniqueId(), symbol, quantity);
        player.sendMessage("Purchase successful!");
    } catch (InsufficientFundsException e) {
        player.sendMessage("Insufficient funds!");
    }
    
    return true;
}
```

## Error Handling Patterns

### Service Layer Exceptions

```java
public class TradingService {
    public void buyInstrument(UUID playerId, String symbol, int quantity) 
            throws InsufficientFundsException, InstrumentNotFoundException {
        
        // Validate business rules
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        // Check funds
        double cost = calculateCost(symbol, quantity);
        if (!walletService.hasSufficientBalance(playerId, cost)) {
            throw new InsufficientFundsException();
        }
        
        // Execute transaction
        executeTransaction(playerId, symbol, quantity, cost);
    }
}
```

### Command Layer Handling

```java
try {
    tradingService.buyInstrument(playerId, symbol, quantity);
    Translation.PurchaseSuccess.sendMessage(player);
} catch (InsufficientFundsException e) {
    Translation.InsufficientFunds.sendMessage(player);
} catch (InstrumentNotFoundException e) {
    Translation.InstrumentNotFound.sendMessage(player, symbol);
} catch (Exception e) {
    logger.severe("Unexpected error during purchase", e);
    Translation.GenericError.sendMessage(player);
}
```

## Testing Patterns

### Service Layer Testing

```java
@Test
void testBuyInstrument() {
    // Given
    UUID playerId = UUID.randomUUID();
    String symbol = "DIAMOND";
    int quantity = 100;
    
    // Setup mocks
    when(walletService.getBalance(playerId)).thenReturn(10000.0);
    
    // When
    tradingService.buyInstrument(playerId, symbol, quantity);
    
    // Then
    verify(walletService).deductBalance(playerId, expectedCost);
    verify(holdingsService).addHolding(playerId, symbol, quantity);
}
```

## Configuration Patterns

### Multi-File Configuration

QuickStocks uses domain-specific config files:
- `config.yml` - System configuration (database, logging, features)
- `market.yml` - Market and trading settings
- `companies.yml` - Company system settings
- `guis.yml` - GUI layouts

### Configuration Loading

```java
public class MarketConfig {
    private final FileConfiguration config;
    
    public MarketConfig(QuickStocksPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "market.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public int getUpdateInterval() {
        return config.getInt("market.updateInterval", 5);
    }
}
```

## Best Practices

1. **Single Responsibility**: Each class has one clear purpose
2. **Dependency Injection**: Use constructor injection for dependencies
3. **Service Delegation**: Commands and GUIs should be thin, delegating to services
4. **Error Handling**: Handle errors at appropriate layer
5. **Testing**: Test business logic in service layer without Minecraft dependencies
6. **Configuration**: Use appropriate config file for the domain

## Anti-Patterns to Avoid

- ❌ Business logic in commands
- ❌ Direct database access from commands
- ❌ Circular dependencies between services
- ❌ Static singletons for services
- ❌ Framework dependencies in core layer
- ❌ Mixing concerns across layers

## Resources

- Main plugin class: `src/main/java/net/cyberneticforge/quickstocks/QuickStocksPlugin.java`
- Service layer: `src/main/java/net/cyberneticforge/quickstocks/core/services/`
- Architecture documentation: `.github/copilot/features/README.md`
