# Portfolio & Wallet System - Copilot Instructions

## Overview
The portfolio and wallet system manages player finances, holdings, watchlists, and trading history. It integrates with Vault economy or provides an internal wallet system.

## Architecture

### Key Components
```
core/services/features/portfolio/
├── WalletService.java      # Balance management & Vault integration
├── HoldingsService.java    # Portfolio/position management
├── WatchlistService.java   # Favorite instruments tracking
└── QueryService.java       # Portfolio analytics & queries
```

### Database Schema
**Tables:**
- `wallets` - Player balances (if not using Vault)
- `holdings` - Player positions/shares owned
- `watchlists` - Tracked instruments per player
- `trading_activity` - Trade history and audit log

## Key Features

### 1. Wallet System

**Dual Mode Operation:**
- **Vault Mode:** Uses external economy plugin (Essentials, CMI, etc.)
- **Internal Mode:** Uses built-in wallet system in database

**Automatic Detection:**
Location: `WalletService.java`
```java
public WalletService() {
    this.useVault = setupVaultEconomy();
    
    if (useVault) {
        logger.info("Using Vault economy integration");
    } else {
        logger.info("Using internal wallet system");
    }
}
```

**Vault Integration:**
- Reflection-based to avoid compile-time dependency
- Works with any Vault-compatible economy plugin
- Seamless fallback if Vault not available
- Tested with: Essentials, CMI, TheNewEconomy

**Internal Wallet:**
- Database-backed (wallets table)
- Per-player balance tracking
- Transaction logging
- No external dependencies

### 2. Balance Operations

**Get Balance:**
```java
WalletService walletService = QuickStocksPlugin.getWalletService();
double balance = walletService.getBalance(playerUuid);
```

**Set Balance (Admin):**
```java
walletService.setBalance(playerUuid, newAmount);
```

**Add Balance:**
```java
walletService.addBalance(playerUuid, amount);
```

**Remove Balance:**
```java
boolean success = walletService.removeBalance(playerUuid, amount);
if (!success) {
    // Insufficient funds
}
```

**Check Sufficient Balance:**
```java
boolean hasEnough = walletService.hasBalance(playerUuid, requiredAmount);
```

### 3. Holdings Management

**Overview:**
Tracks player ownership of instruments (stocks, crypto, company shares).

**Data Structure:**
```java
public record Holding(
    String playerUuid,
    String instrumentId,
    double quantity,
    double avgBuyPrice,
    long lastUpdated
) {}
```

**Add Holdings:**
```java
HoldingsService holdingsService = QuickStocksPlugin.getHoldingsService();
holdingsService.addHolding(playerUuid, instrumentId, quantity, price);
```
- Updates avg_buy_price using weighted average
- Creates record if doesn't exist
- Updates existing quantity if exists

**Remove Holdings:**
```java
holdingsService.removeHolding(playerUuid, instrumentId, quantity);
```
- Decreases quantity
- Deletes record if quantity reaches 0
- Throws exception if insufficient holdings

**Get Holdings:**
```java
// Single instrument
Holding holding = holdingsService.getHolding(playerUuid, instrumentId);

// All holdings for player
List<Holding> portfolio = holdingsService.getPlayerHoldings(playerUuid);
```

**Portfolio Value:**
```java
// Get total portfolio value at current market prices
double totalValue = holdingsService.getPortfolioValue(playerUuid);
```

### 4. Watchlist System

**Purpose:**
Players can track favorite instruments for quick access.

**Add to Watchlist:**
```java
WatchlistService watchlistService = QuickStocksPlugin.getWatchlistService();
watchlistService.addToWatchlist(playerUuid, instrumentId);
```
- Fires `WatchlistAddEvent`
- Timestamp recorded

**Remove from Watchlist:**
```java
watchlistService.removeFromWatchlist(playerUuid, instrumentId);
```
- Fires `WatchlistRemoveEvent`

**Get Watchlist:**
```java
List<String> instrumentIds = watchlistService.getWatchlist(playerUuid);
```

**Check if Watching:**
```java
boolean isWatching = watchlistService.isWatching(playerUuid, instrumentId);
```

**Clear Watchlist:**
```java
watchlistService.clearWatchlist(playerUuid);
```

### 5. Query Service

**Purpose:**
Analytics and complex queries for portfolio data.

**Get Top Holdings:**
```java
QueryService queryService = QuickStocksPlugin.getQueryService();
List<Holding> topHoldings = queryService.getTopHoldings(playerUuid, limit);
```

**Get Performance:**
```java
double profitLoss = queryService.calculateProfitLoss(playerUuid, instrumentId);
double returnPercent = queryService.calculateReturn(playerUuid, instrumentId);
```

**Get Trading Activity:**
```java
List<Trade> recentTrades = queryService.getRecentTrades(playerUuid, limit);
```

## Commands

### Wallet Command (`/wallet`)
Handler: `commands/WalletCommand.java`

**Subcommands:**
- `/wallet` - Show balance
- `/wallet balance` - Show balance
- `/wallet add <amount>` - Add funds (admin, requires `quickstocks.wallet.add`)
- `/wallet set <amount>` - Set balance (admin, requires `quickstocks.wallet.set`)

### Watch Command (`/watch`)
Handler: `commands/WatchCommand.java`

**Subcommands:**
- `/watch` - View watchlist
- `/watch add <SYMBOL>` - Add to watchlist
- `/watch remove <SYMBOL>` - Remove from watchlist
- `/watch list` - View watchlist
- `/watch info <SYMBOL>` - View instrument details
- `/watch clear` - Clear entire watchlist

### Market Command (Portfolio)
Handler: `commands/MarketCommand.java`

**Subcommands:**
- `/market portfolio` - View holdings
- `/market history` - View trade history
- `/market performance` - View profit/loss

## Configuration

### Wallet Configuration
**Location:** `config.yml`
```yaml
features:
  wallet:
    enabled: true
    useVault: auto  # auto, true, false
    startingBalance: 10000.0  # If using internal wallet
```

### Watchlist Configuration
**Location:** `market.yml`
```yaml
market:
  watchlist:
    enabled: true
    maxItems: 50  # Max instruments per player
```

## Events

```java
// Balance changed
WalletBalanceChangeEvent event = new WalletBalanceChangeEvent(
    playerUuid,
    oldBalance,
    newBalance,
    reason
);
Bukkit.getPluginManager().callEvent(event);

// Watchlist modified
WatchlistAddEvent event = new WatchlistAddEvent(playerUuid, instrumentId);
Bukkit.getPluginManager().callEvent(event);

WatchlistRemoveEvent event = new WatchlistRemoveEvent(playerUuid, instrumentId);
Bukkit.getPluginManager().callEvent(event);
```

## Development Guidelines

### Adding Balance Operations
```java
// Always use try-catch for SQL operations
try {
    walletService.addBalance(playerUuid, amount);
    
    // Log the operation
    logger.info("Added $" + amount + " to " + playerUuid);
    
    // Fire event if needed
    Bukkit.getPluginManager().callEvent(
        new WalletBalanceChangeEvent(...)
    );
} catch (SQLException e) {
    logger.warning("Failed to add balance: " + e.getMessage());
    // Handle error
}
```

### Updating Holdings
```java
// Use transactions for multi-step operations
try {
    database.beginTransaction();
    
    // Deduct balance
    walletService.removeBalance(playerUuid, cost);
    
    // Add holdings
    holdingsService.addHolding(playerUuid, instrumentId, quantity, price);
    
    database.commitTransaction();
} catch (Exception e) {
    database.rollbackTransaction();
    throw e;
}
```

### Querying Portfolio
```java
// Get holdings with current prices
List<Holding> holdings = holdingsService.getPlayerHoldings(playerUuid);
for (Holding holding : holdings) {
    Instrument instrument = instrumentService.getInstrument(holding.instrumentId());
    double currentValue = holding.quantity() * instrument.currentPrice();
    double cost = holding.quantity() * holding.avgBuyPrice();
    double profitLoss = currentValue - cost;
    
    // Display to player
}
```

## Testing

### WalletService Tests
**Location:** `test/.../WalletServiceTest.java`

**Test Coverage:**
- Balance operations (get, set, add, remove)
- Insufficient funds handling
- Vault vs internal mode
- Transaction consistency

### HoldingsService Tests
**Location:** `test/.../HoldingsServiceTest.java`

**Test Coverage:**
- Add/remove holdings
- Average price calculations
- Insufficient holdings errors
- Portfolio queries

### Example Test:
```java
@Test
void testAddBalance() throws SQLException {
    // Given
    String playerUuid = "test-uuid";
    double initialBalance = 1000.0;
    walletService.setBalance(playerUuid, initialBalance);
    
    // When
    walletService.addBalance(playerUuid, 500.0);
    
    // Then
    double finalBalance = walletService.getBalance(playerUuid);
    assertEquals(1500.0, finalBalance, 0.01);
}
```

## Common Patterns

### Buying with Balance Check
```java
double cost = quantity * price;
if (!walletService.hasBalance(playerUuid, cost)) {
    Translation.InsufficientFunds.sendMessage(player);
    return;
}

walletService.removeBalance(playerUuid, cost);
holdingsService.addHolding(playerUuid, instrumentId, quantity, price);
```

### Selling with Holdings Check
```java
Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
if (holding == null || holding.quantity() < quantity) {
    Translation.InsufficientHoldings.sendMessage(player);
    return;
}

holdingsService.removeHolding(playerUuid, instrumentId, quantity);
double revenue = quantity * price;
walletService.addBalance(playerUuid, revenue);
```

### Portfolio Summary Display
```java
List<Holding> holdings = holdingsService.getPlayerHoldings(playerUuid);
double totalValue = 0.0;

player.sendMessage("§6=== Your Portfolio ===");
for (Holding holding : holdings) {
    Instrument instrument = instrumentService.getInstrument(holding.instrumentId());
    double value = holding.quantity() * instrument.currentPrice();
    totalValue += value;
    
    player.sendMessage(String.format(
        "§e%s: §f%.2f @ $%.2f = §a$%.2f",
        instrument.symbol(),
        holding.quantity(),
        instrument.currentPrice(),
        value
    ));
}
player.sendMessage(String.format("§6Total Value: §a$%.2f", totalValue));
```

## Performance Considerations

### Caching
- Cache watchlist for active players
- Cache portfolio for quick access
- Invalidate on updates

### Batch Queries
- Load all holdings in one query
- Join with instruments for current prices
- Avoid N+1 query problems

### Indexing
- Index `holdings.player_uuid`
- Index `watchlists.player_uuid`
- Composite index on `(player_uuid, instrument_id)`

## Troubleshooting

### Balance Not Updating
- Check if Vault integration is enabled
- Verify economy plugin is loaded
- Check for SQL errors in logs
- Verify transactions are committed

### Holdings Incorrect
- Check for race conditions in trading
- Verify transactions are atomic
- Check average price calculation
- Review trade history logs

### Watchlist Issues
- Check max items limit
- Verify instrument exists
- Check for duplicate entries
- Review event firing

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Market trading: `.github/copilot/features/market-trading.md`
- Database layer: `.github/copilot/features/database-persistence.md`
- Plugin integrations: `.github/copilot/features/plugin-integrations.md`
