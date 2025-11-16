# Cryptocurrency System - Copilot Instructions

## Overview
The cryptocurrency system allows creation of custom crypto instruments alongside default cryptocurrencies. Both player-created and company-created cryptos are supported.

## Architecture

### Key Components
```
core/services/features/market/
└── CryptoService.java           # Cryptocurrency management

commands/
└── CryptoCommand.java           # /crypto command handler
```

### Database Schema
Cryptocurrencies use the unified `instruments` table:
```sql
-- Crypto-specific instrument entry
INSERT INTO instruments (
    id,
    type,              -- 'CRYPTO' or 'CUSTOM_CRYPTO'
    symbol,           -- 'BTC', 'MYCOIN', etc.
    display_name,     -- 'Bitcoin', 'My Custom Coin'
    mc_material,      -- NULL for crypto (not Minecraft item)
    decimals,         -- Usually 8 for crypto
    created_by,       -- Player UUID or 'SYSTEM'
    created_at        -- Timestamp
) VALUES (...);
```

## Key Features

### 1. Crypto Types

**Default Cryptocurrencies:**
- Seeded automatically at plugin startup
- Examples: BTC, ETH, DOGE, etc.
- Created by 'SYSTEM'
- Type: `CRYPTO`

**Custom Cryptocurrencies:**
- Created by players via `/crypto create`
- Created by companies via `/crypto company create`
- Type: `CUSTOM_CRYPTO`
- Requires creation cost payment

### 2. Default Crypto Seeding

**Location:** `CryptoService.seedDefaultCryptos()`

**Default Cryptos:**
```java
private static final List<CryptoConfig> DEFAULT_CRYPTOS = List.of(
    new CryptoConfig("BTC", "Bitcoin", 50000.0, 8),
    new CryptoConfig("ETH", "Ethereum", 3000.0, 8),
    new CryptoConfig("DOGE", "Dogecoin", 0.10, 8),
    new CryptoConfig("XRP", "Ripple", 0.50, 8),
    new CryptoConfig("LTC", "Litecoin", 150.0, 8)
);

public void seedDefaultCryptos() {
    for (CryptoConfig crypto : DEFAULT_CRYPTOS) {
        if (!instrumentExists(crypto.symbol())) {
            createCrypto(
                crypto.symbol(),
                crypto.displayName(),
                "SYSTEM",
                crypto.initialPrice(),
                crypto.decimals()
            );
        }
    }
}
```

**Timing:** Called during plugin initialization in `QuickStocksPlugin.onEnable()`

### 3. Personal Cryptocurrency Creation

**Command:** `/crypto create <symbol> <name>`

**Requirements:**
- Permission: `maksy.stocks.crypto.create`
- Cost: Configurable (default: $1000)
- Sufficient wallet balance

**Process:**
```java
public String createCustomCrypto(
    String symbol,
    String displayName,
    String createdBy,
    double initialPrice,
    int decimals
) throws SQLException {
    // Validate inputs
    validateSymbol(symbol);
    validateDisplayName(displayName);
    
    // Check for duplicate
    if (instrumentExists(symbol)) {
        throw new IllegalArgumentException("Symbol already exists");
    }
    
    // Deduct creation cost
    double cost = getCryptoConfig().getPersonalConfig().getCreationCost();
    if (!walletService.removeBalance(createdBy, cost)) {
        throw new InsufficientFundsException();
    }
    
    // Create instrument
    String instrumentId = UUID.randomUUID().toString();
    database.execute(
        "INSERT INTO instruments (id, type, symbol, display_name, decimals, created_by, created_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
        instrumentId,
        "CUSTOM_CRYPTO",
        symbol.toUpperCase(),
        displayName,
        decimals,
        createdBy,
        System.currentTimeMillis()
    );
    
    // Create initial market state
    createInstrumentState(instrumentId, initialPrice);
    
    // Fire event
    Bukkit.getPluginManager().callEvent(
        new CryptoCreateEvent(createdBy, symbol, displayName)
    );
    
    return instrumentId;
}
```

**Example:**
```bash
/crypto create MYCOIN "My Custom Coin"
```

### 4. Company Cryptocurrency Creation

**Command:** `/crypto company create <company> <symbol> <name>`

**Requirements:**
- Player must be company employee
- Permission: `MANAGE_COMPANY` in company role
- Cost deducted from company balance
- Company must have sufficient funds

**Process:**
Similar to personal crypto, but:
- Cost deducted from company balance
- `created_by` set to company ID
- Company owns the crypto

**Example:**
```bash
/crypto company create TechCorp TECHCOIN "TechCorp Coin"
```

### 5. Crypto Configuration

**Location:** `market.yml`
```yaml
crypto:
  enabled: true
  
  # Default cryptocurrencies
  defaultCryptos:
    - symbol: BTC
      name: Bitcoin
      initialPrice: 50000.0
      decimals: 8
    - symbol: ETH
      name: Ethereum
      initialPrice: 3000.0
      decimals: 8
    # ... more defaults
  
  # Personal cryptocurrency creation
  personalCrypto:
    enabled: true
    creationCost: 1000.0
    minInitialPrice: 0.01
    maxInitialPrice: 1000000.0
    minDecimals: 0
    maxDecimals: 8
    allowedSymbolChars: "[A-Z0-9_]"
    maxSymbolLength: 10
    maxNameLength: 50
  
  # Company cryptocurrency creation
  companyCrypto:
    enabled: true
    creationCost: 5000.0  # More expensive than personal
    requirePermission: MANAGE_COMPANY
```

### 6. Crypto Trading

**Same as Any Instrument:**
Once created, cryptocurrencies trade exactly like stocks:

```bash
# Buy crypto
/market buy MYCOIN 100

# Sell crypto
/market sell MYCOIN 50

# View in market GUI
/market
```

**Market Behavior:**
- Same price algorithm as stocks
- Subject to circuit breakers
- Trading fees apply
- Slippage on large orders
- Historical data tracked

### 7. Crypto Validation

**Symbol Validation:**
```java
private void validateSymbol(String symbol) {
    if (symbol == null || symbol.isEmpty()) {
        throw new IllegalArgumentException("Symbol cannot be empty");
    }
    
    if (symbol.length() > maxSymbolLength) {
        throw new IllegalArgumentException("Symbol too long");
    }
    
    if (!symbol.matches(allowedSymbolPattern)) {
        throw new IllegalArgumentException("Invalid characters in symbol");
    }
    
    // Check if symbol already exists
    if (instrumentExists(symbol)) {
        throw new IllegalArgumentException("Symbol already exists");
    }
}
```

**Name Validation:**
```java
private void validateDisplayName(String name) {
    if (name == null || name.isEmpty()) {
        throw new IllegalArgumentException("Name cannot be empty");
    }
    
    if (name.length() > maxNameLength) {
        throw new IllegalArgumentException("Name too long");
    }
}
```

**Price Validation:**
```java
private void validateInitialPrice(double price) {
    if (price < minInitialPrice || price > maxInitialPrice) {
        throw new IllegalArgumentException(
            "Price must be between $" + minInitialPrice + " and $" + maxInitialPrice
        );
    }
}
```

## Commands

### Crypto Command (`/crypto`)
Handler: `commands/CryptoCommand.java`

**Subcommands:**
- `/crypto create <symbol> <name>` - Create personal crypto
  - Permission: `maksy.stocks.crypto.create`
  - Cost: From wallet balance
  
- `/crypto company create <company> <symbol> <name>` - Create company crypto
  - Permission: Company employee with `MANAGE_COMPANY`
  - Cost: From company balance

### Tab Completion
```java
@Override
public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
        return List.of("create", "company");
    }
    
    if (args.length == 2 && args[0].equalsIgnoreCase("company")) {
        return List.of("create");
    }
    
    if (args.length == 3 && args[0].equalsIgnoreCase("company") && args[1].equalsIgnoreCase("create")) {
        // Suggest player's companies
        return companyService.getPlayerCompanies(sender.getName());
    }
    
    return null;
}
```

## Events

```java
// Crypto created
CryptoCreateEvent event = new CryptoCreateEvent(
    creatorUuid,
    symbol,
    displayName
);
Bukkit.getPluginManager().callEvent(event);
```

## Development Guidelines

### Adding New Default Crypto
1. Add to `DEFAULT_CRYPTOS` list in `CryptoService`
2. Run plugin to seed automatically
3. No database migration needed (uses existing schema)

### Modifying Creation Cost
1. Update `market.yml`:
```yaml
crypto:
  personalCrypto:
    creationCost: 2000.0  # New cost
```
2. Reload config or restart server
3. No code changes needed

### Custom Validation Rules
Add to `CryptoService`:
```java
private void validateCustomRule(String symbol, String name) {
    // Check for offensive words
    if (containsOffensiveContent(name)) {
        throw new IllegalArgumentException("Name contains inappropriate content");
    }
    
    // Prevent system prefixes
    if (symbol.startsWith("SYS_")) {
        throw new IllegalArgumentException("Cannot use system prefix");
    }
}
```

## Testing

### Test Coverage
**Location:** `test/.../CryptoServiceTest.java` (to be added)

**Test Cases:**
```java
@Test
void testCreateCustomCrypto_Success() {
    // Valid creation with sufficient funds
}

@Test
void testCreateCustomCrypto_InsufficientFunds() {
    // Should fail without enough balance
}

@Test
void testCreateCustomCrypto_DuplicateSymbol() {
    // Should reject duplicate symbols
}

@Test
void testCreateCustomCrypto_InvalidSymbol() {
    // Should reject invalid characters
}

@Test
void testCreateCompanyCrypto_Success() {
    // Company crypto creation
}

@Test
void testCreateCompanyCrypto_NoPermission() {
    // Should fail without permission
}
```

### Manual Testing
```bash
# Test personal crypto creation
/wallet add 10000
/crypto create TEST "Test Coin"
/market buy TEST 10

# Test company crypto creation
/company create MyCorp PUBLIC
/company deposit MyCorp 10000
/crypto company create MyCorp MCOIN "MyCorp Coin"
/market buy MCOIN 5
```

## Common Patterns

### Creating Crypto Programmatically
```java
CryptoService cryptoService = QuickStocksPlugin.getCryptoService();

try {
    String instrumentId = cryptoService.createCustomCrypto(
        "MYCOIN",
        "My Custom Coin",
        player.getUniqueId().toString(),
        10.0,  // Initial price
        8      // Decimals
    );
    
    player.sendMessage("§aCrypto created: " + instrumentId);
} catch (InsufficientFundsException e) {
    player.sendMessage("§cInsufficient funds to create crypto");
} catch (IllegalArgumentException e) {
    player.sendMessage("§c" + e.getMessage());
}
```

### Checking if Crypto Exists
```java
boolean exists = cryptoService.cryptoExists("MYCOIN");
if (exists) {
    player.sendMessage("§cThat symbol is already taken");
}
```

### Getting Crypto Creator
```java
Instrument crypto = instrumentService.getInstrument(instrumentId);
String creatorUuid = crypto.createdBy();

if ("SYSTEM".equals(creatorUuid)) {
    player.sendMessage("§7This is a default cryptocurrency");
} else {
    player.sendMessage("§7Created by: " + getPlayerName(creatorUuid));
}
```

## Performance Considerations

### Crypto Seeding
- Runs once on startup
- Checks existence before creating
- Uses batch operations for multiple cryptos

### Crypto Queries
- Same indexes as other instruments
- Symbol lookups are fast (UNIQUE index)
- Type filtering efficient with index

## Troubleshooting

### Crypto Not Tradeable
- Verify instrument_state record created
- Check market is enabled
- Verify circuit breaker not triggered

### Creation Fails
- Check wallet/company balance
- Verify permissions
- Check symbol uniqueness
- Review validation rules

### Symbol Conflicts
- Check for case-insensitive duplicates
- Review existing instruments
- Check both CRYPTO and CUSTOM_CRYPTO types

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Market trading: `.github/copilot/features/market-trading.md`
- Company management: `.github/copilot/features/company-management.md`
- Database layer: `.github/copilot/features/database-persistence.md`
