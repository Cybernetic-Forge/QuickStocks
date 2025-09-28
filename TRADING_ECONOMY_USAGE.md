# Trading Economy Features Usage Guide

This document explains how to use the new trading economy features including fees, limits, circuit breakers, order types, and slippage.

## Configuration

Add the following section to your `config.yml`:

```yaml
trading:
  fee:
    mode: percent   # percent | flat | mixed
    percent: 0.25   # % of notional
    flat: 0.0
  limits:
    maxOrderQty: 10000
    maxNotionalPerMinute: 250000
    perPlayerCooldownMs: 750
  circuitBreakers:
    enable: true
    levels: [7, 13, 20]     # halt thresholds in % move from daily open
    haltMinutes: [15, 15, -1] # -1 = rest of session
  orders:
    allowMarket: true
    allowLimit: true
    allowStop: true
  slippage:
    mode: linear            # none | linear | sqrtImpact
    k: 0.0005               # impact coefficient (tune)
```

## Database Migration

The new features require database schema changes. Run the V3 migration:
- `V3__trading_economy.sql` adds new columns to `orders` table and creates `trading_halts`, `player_trade_limits`, and `trading_sessions` tables.

## Usage Examples

### Enhanced Trading Service

```java
// Initialize with configuration for enhanced features
TradingConfig config = loadTradingConfig(); // Load from config.yml
TradingService tradingService = new TradingService(database, walletService, holdingsService, config);

// Create different order types
OrderRequest marketOrder = OrderRequest.marketOrder("player123", "AAPL", "BUY", 100.0);
OrderRequest limitOrder = OrderRequest.limitOrder("player123", "AAPL", "BUY", 100.0, 150.0);
OrderRequest stopOrder = OrderRequest.stopOrder("player123", "AAPL", "SELL", 50.0, 140.0);

// Execute orders with full economy features
TradeResult result1 = tradingService.executeOrder(marketOrder);
TradeResult result2 = tradingService.executeOrder(limitOrder);
TradeResult result3 = tradingService.executeOrder(stopOrder);
```

### Backward Compatibility

```java
// Legacy constructor still works (no enhanced features)
TradingService legacyService = new TradingService(database, walletService, holdingsService);

// Legacy methods work as before
TradeResult result = legacyService.executeBuyOrder("player123", "AAPL", 100.0);
```

## Feature Details

### Fees
- **Percent mode**: Fee = notional × (percent / 100)
- **Flat mode**: Fixed fee amount regardless of trade size
- **Mixed mode**: Combines percentage and flat fees
- Fees are deducted from wallet balance for buys, from proceeds for sells
- All fees are recorded in the `orders.fee_paid` column

### Slippage
- **Linear mode**: Impact = k × quantity
- **Sqrt mode**: Impact = k × √quantity  
- **None mode**: No slippage applied
- Buy orders get worse prices (higher), sell orders get worse prices (lower)
- Represents market impact and liquidity constraints

### Rate Limits
- **Max Order Quantity**: Prevents excessively large individual orders
- **Max Notional Per Minute**: Limits total trading volume per player per minute
- **Cooldown**: Minimum time between trades for each player
- Violations return descriptive error messages

### Circuit Breakers
- **Thresholds**: Default 7%, 13%, 20% price moves from daily open
- **Halt Duration**: Configurable minutes for each level (-1 = rest of session)
- **Session Tracking**: Daily open prices stored for percentage calculations
- **Halt Recording**: All halts recorded in `trading_halts` table

### Order Types
- **Market Orders**: Execute immediately at current price
- **Limit Orders**: Execute only at limit price or better
- **Stop Orders**: Trigger at stop price, then execute as market order
- Order type availability controlled by configuration
- Enhanced order history includes order type and prices

## Error Messages

### Rate Limiting
- "Order quantity 15000.00 exceeds maximum allowed 10000.00"
- "Trading cooldown active. Please wait 0.3 seconds"
- "Adding this trade would exceed the per-minute notional limit"

### Circuit Breakers
- "Trading is halted due to circuit breaker (Level 2)"

### Order Validation
- "Order type LIMIT is not allowed"
- "Limit price is required and must be positive for LIMIT orders"
- "Order conditions not met for execution"

### Fees and Balance
- "Insufficient funds. Required: $10025.00 (including $25.00 fee)"

## Implementation Notes

### Slippage Tuning
- Start with small k values (0.0001 - 0.001)
- Linear mode good for small orders, sqrt mode for larger impact mitigation
- Monitor actual vs expected execution prices

### Circuit Breaker Calibration
- 7%/13%/20% mirrors real market standards
- Adjust halt durations based on game dynamics
- Consider disabling for high-volatility instruments

### Fee Structure
- Low percentage fees (0.1% - 0.5%) maintain trading activity
- Flat fees can disadvantage small trades
- Mixed mode provides minimum revenue per trade

### Rate Limiting Balance
- Set limits high enough to allow normal trading
- Use cooldowns to prevent spam/manipulation
- Monitor and adjust based on player behavior