# Market & Trading System - Copilot Instructions

## Overview
The market and trading system is the core feature of QuickStocks, providing a realistic stock market simulation with company shares, cryptocurrencies, and item trading.

## Architecture

### Key Components
```
core/services/features/market/
├── StockMarketService.java       # Core market simulation engine
├── TradingService.java           # Basic trading operations
├── EnhancedTradingService.java   # Advanced trading with slippage
├── CompanyMarketService.java     # Company share trading
├── CryptoService.java            # Cryptocurrency management
├── FeeService.java               # Trading fee calculations
├── SlippageService.java          # Market slippage calculations
├── CircuitBreakerService.java    # Trading halt system
├── RateLimitService.java         # Anti-spam protection
└── InstrumentPersistenceService.java  # Instrument database operations
```

### Database Schema
**Tables:**
- `instruments` - Core instrument registry (stocks, crypto, items, company shares)
- `instrument_state` - Current market state (price, volume, volatility)
- `instrument_price_history` - Historical price data for analytics
- `holdings` - Player positions/holdings
- `trading_activity` - Trade history and audit log

## Key Features

### 1. Instrument Types
The system supports multiple instrument types in a unified schema:
- **ITEM** - Minecraft items as tradeable assets
- **CRYPTO** - Cryptocurrencies (both default and custom)
- **EQUITY** - Traditional stocks
- **INDEX** - Market indices
- **FUND** - Investment funds
- **CUSTOM_CRYPTO** - Player-created cryptocurrencies
- **COMPANY** - Company shares (special handling)

### 2. Price Calculation Algorithm
Location: `core/algorithms/StockPriceCalculator.java`

**Realistic Market Factors:**
- Economic indicators (inflation, interest rates, GDP)
- Market sentiment (fear/greed index)
- Technical analysis (moving averages, support/resistance)
- Industry-specific factors
- Global events
- Random market noise

**Implementation Pattern:**
```java
// When updating prices
StockPriceCalculator calculator = new StockPriceCalculator();
double newPrice = calculator.calculatePrice(
    currentPrice,
    marketFactors,
    technicalIndicators,
    randomEvents
);
```

### 3. Trading Flow

**Buy Flow:**
1. Validate player has sufficient balance
2. Calculate trading fees (FeeService)
3. Apply slippage if volume is large (SlippageService)
4. Check circuit breakers (CircuitBreakerService)
5. Execute trade (deduct balance, add holdings)
6. Update instrument state (volume, price)
7. Record trade history
8. Fire `InstrumentBuyEvent`

**Sell Flow:**
1. Validate player has sufficient holdings
2. Calculate trading fees
3. Apply slippage
4. Execute trade (add balance, remove holdings)
5. Update instrument state
6. Record trade history
7. Fire appropriate sell event

**Code Pattern:**
```java
// In TradingService or EnhancedTradingService
OrderResult result = tradingService.executeBuyOrder(
    OrderRequest.builder()
        .playerUuid(uuid)
        .instrumentId(instrumentId)
        .quantity(qty)
        .limitPrice(maxPrice) // optional
        .build()
);

if (result.success()) {
    // Show success message
} else {
    // Show error: result.message()
}
```

### 4. Circuit Breakers
Automatic trading halts when price moves too quickly.

**Configuration:** `market.yml`
```yaml
market:
  circuitBreakers:
    enabled: true
    priceChangeThreshold: 0.15  # 15% change triggers halt
    cooldownMinutes: 5
```

**Implementation:**
- CircuitBreakerService checks price change before trades
- Halts triggered automatically
- Cooldown period before resuming
- Events fired: `CircuitBreakerTriggeredEvent`

### 5. Trading Fees & Slippage

**Fee Calculation:**
Location: `FeeService.java`
```java
double fee = feeService.calculateTradingFee(tradeValue);
// Based on configuration in market.yml
```

**Slippage:**
Location: `SlippageService.java`
- Large orders move the market
- Calculated based on order size vs daily volume
- Applied to final execution price

### 6. Company Share Trading
Special handling for company shares via `CompanyMarketService`:

**IPO Process:**
1. Company goes public (`/company ipo`)
2. Initial share price set
3. Instrument created with type COMPANY
4. Shares distributed to owners/employees
5. Trading enabled on market

**Share Trading:**
- Buy shares: `/market buy <SYMBOL> <quantity>`
- Sell shares: `/market sell <SYMBOL> <quantity>`
- View shareholders: `/market shareholders <SYMBOL>`

**Ownership Transfer:**
When a player buys >50% shares, they become company owner automatically.

### 7. Cryptocurrency System
Location: `CryptoService.java`

**Types:**
- Default cryptos (seeded at startup)
- Player-created custom cryptos (`/crypto create`)
- Company cryptos (`/crypto company create`)

**Creation Cost:**
Configurable in `market.yml` under `crypto.personalCrypto.creationCost`

**Pattern:**
```java
String instrumentId = cryptoService.createCustomCrypto(
    "MYCOIN",
    "My Custom Coin",
    playerUuid,
    initialPrice,
    decimals
);
```

## Configuration

### Primary Config: `market.yml`
```yaml
market:
  enabled: true
  updateInterval: 5  # Price updates every 5 seconds
  
  trading:
    enabled: true
    defaultFeePercent: 0.002  # 0.2% trading fee
    maxSlippage: 0.05  # 5% max slippage
    
  circuitBreakers:
    enabled: true
    priceChangeThreshold: 0.15
    
  analytics:
    enabled: true
    
  crypto:
    enabled: true
    personalCrypto:
      enabled: true
      creationCost: 1000.0
```

## Commands

### Market Command (`/market`)
Handler: `commands/MarketCommand.java`

**Subcommands:**
- `/market` - Open market browser GUI
- `/market browse` - Same as above
- `/market buy <SYMBOL> <quantity>` - Buy shares
- `/market sell <SYMBOL> <quantity>` - Sell shares
- `/market portfolio` - View your holdings
- `/market history` - View trade history
- `/market shareholders <SYMBOL>` - View company shareholders

### Crypto Command (`/crypto`)
Handler: `commands/CryptoCommand.java`

**Subcommands:**
- `/crypto create <symbol> <name>` - Create custom crypto
- `/crypto company create <company> <symbol> <name>` - Create company crypto

## GUI Components

### MarketGUI
Location: `gui/MarketGUI.java`

**Features:**
- Browse all tradeable instruments
- Filter by type (stocks, crypto, company shares)
- Sort by price, volume, change %
- Click to buy/sell
- View instrument details

**Layout:** Configurable in `guis.yml`

## Events

### Trading Events
```java
// When player buys instrument
InstrumentBuyEvent event = new InstrumentBuyEvent(player, instrument, quantity, price);
Bukkit.getPluginManager().callEvent(event);

// When player buys company shares
ShareBuyEvent event = new ShareBuyEvent(player, company, quantity, price);
Bukkit.getPluginManager().callEvent(event);

// When player sells company shares
ShareSellEvent event = new ShareSellEvent(player, company, quantity, price);
Bukkit.getPluginManager().callEvent(event);
```

### Market Events
```java
// When market opens (if market hours enabled)
MarketOpenEvent event = new MarketOpenEvent();
Bukkit.getPluginManager().callEvent(event);

// When market closes
MarketCloseEvent event = new MarketCloseEvent();
Bukkit.getPluginManager().callEvent(event);

// When circuit breaker triggers
CircuitBreakerTriggeredEvent event = new CircuitBreakerTriggeredEvent(instrument, priceChange);
Bukkit.getPluginManager().callEvent(event);

// When price updates
InstrumentPriceUpdateEvent event = new InstrumentPriceUpdateEvent(instrument, oldPrice, newPrice);
Bukkit.getPluginManager().callEvent(event);
```

## Development Guidelines

### Adding New Instrument Types
1. Add enum value to `InstrumentType` in `core/enums/`
2. Update `InstrumentPersistenceService.createInstrument()` to handle new type
3. Update GUI filters to show new type
4. Add seeding logic if needed

### Modifying Price Algorithm
1. Edit `StockPriceCalculator.java`
2. Add new market factors to `core/enums/MarketFactor.java`
3. Update factor weights in calculation
4. Test with market simulation

### Adding Trading Features
1. Implement in appropriate service (TradingService, EnhancedTradingService)
2. Update database schema if needed (create migration)
3. Add events if state changes
4. Update commands/GUI to expose feature
5. Add configuration options to `market.yml`

### Testing Trading Logic
See `test/java/.../core/services/TradingServiceTest.java`:
- Test buy/sell with sufficient/insufficient funds
- Test fee calculations
- Test slippage calculations
- Test circuit breaker triggers
- Test holdings updates

## Common Patterns

### Getting Current Price
```java
Instrument instrument = instrumentService.getInstrument(instrumentId);
double currentPrice = instrument.currentPrice();
```

### Checking Holdings
```java
HoldingsService holdingsService = QuickStocksPlugin.getHoldingsService();
Holding holding = holdingsService.getHolding(playerUuid, instrumentId);
double quantity = holding != null ? holding.quantity() : 0.0;
```

### Recording Trade
```java
tradingService.recordTrade(
    playerUuid,
    instrumentId,
    quantity,
    price,
    TradeType.BUY,
    fee
);
```

## Performance Considerations

### Price Updates
- Run on async scheduler (5-second intervals)
- Batch update multiple instruments
- Use prepared statements for DB updates

### Large Order Handling
- Slippage automatically increases for large orders
- Consider splitting large orders in UI
- Rate limiting prevents spam

### Database Optimization
- Indexes on `instrument_state.instrument_id`
- Indexes on `holdings.player_uuid`
- Separate history table for analytics (append-only)

## Troubleshooting

### Price Not Updating
- Check `market.enabled` in config
- Verify StockMarketService is initialized
- Check database connection
- Look for errors in logs at DEBUG level

### Trading Fails
- Verify player has sufficient balance
- Check circuit breaker status
- Verify instrument exists and is tradeable
- Check rate limits

### Holdings Not Showing
- Verify holdings table has correct data
- Check for database errors
- Ensure quantity > 0 (zero holdings are deleted)

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Database layer: `.github/copilot/features/database-persistence.md`
- GUI system: `.github/copilot/features/gui-system.md`
- Company management: `.github/copilot/features/company-management.md`
