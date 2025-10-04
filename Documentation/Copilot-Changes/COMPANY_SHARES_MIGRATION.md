# Company Shares Migration to Instruments Infrastructure

## Overview

Company shares now leverage the existing instruments infrastructure instead of using separate tables. This provides better integration with QueryService, TradingService, and all existing market analytics.

## Architecture

### Before (Deprecated)
```
Company Shares:
  - company_shareholders (holdings)
  - company_share_tx (transactions)
  - Separate tracking system
```

### After (Current)
```
Company Shares as Instruments:
  - instruments (type='EQUITY', id='COMPANY_{companyId}')
  - instrument_state (current price)
  - instrument_price_history (price tracking)
  - user_holdings (player shareholdings)
  - orders (transaction history)
```

## How It Works

### 1. Company Goes on Market (IPO)

When `CompanyMarketService.enableMarket()` is called:

```java
// Create instrument entry
String instrumentId = "COMPANY_" + companyId;
INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
VALUES (instrumentId, 'EQUITY', company.symbol, company.name, 2, now);

// Create instrument state with initial price
double sharePrice = company.balance / 10000.0;
INSERT INTO instrument_state (instrument_id, last_price, market_cap, updated_at)
VALUES (instrumentId, sharePrice, company.balance, now);
```

### 2. Buying Shares

When `CompanyMarketService.buyShares()` is called:

```java
// Use TradingService to execute the buy
TradingService.executeBuyOrder(playerUuid, instrumentId, quantity);

// This automatically:
// - Updates user_holdings table
// - Records transaction in orders table
// - Handles wallet balance

// Then update company balance
UPDATE companies SET balance = balance + totalCost WHERE id = companyId;

// Update instrument price
UPDATE instrument_state SET last_price = newPrice WHERE instrument_id = instrumentId;
INSERT INTO instrument_price_history (instrument_id, price, ts) VALUES (...);
```

### 3. Selling Shares

When `CompanyMarketService.sellShares()` is called:

```java
// Use TradingService to execute the sell
TradingService.executeSellOrder(playerUuid, instrumentId, quantity);

// This automatically:
// - Updates user_holdings table (removes shares)
// - Records transaction in orders table
// - Handles wallet balance

// Then update company balance (buyback)
UPDATE companies SET balance = balance - totalValue WHERE id = companyId;

// Update instrument price
UPDATE instrument_state SET last_price = newPrice WHERE instrument_id = instrumentId;
```

### 4. Company Delists

When `CompanyMarketService.disableMarket()` is called:

```java
// Pay out all shareholders
FOR EACH shareholder IN user_holdings WHERE instrument_id = instrumentId:
    TradingService.executeSellOrder(shareholder, instrumentId, shares);

// Clean up instrument data
DELETE FROM instrument_price_history WHERE instrument_id = instrumentId;
DELETE FROM instrument_state WHERE instrument_id = instrumentId;
DELETE FROM instruments WHERE id = instrumentId;

UPDATE companies SET on_market = 0 WHERE id = companyId;
```

## Benefits

### 1. Unified Infrastructure
- QueryService methods work automatically with company shares
- TradingService handles all trade execution logic
- No duplicate code for holdings and transaction tracking

### 2. Automatic Analytics
- Price history tracked in `instrument_price_history`
- Can use existing analytics queries (top gainers, volume, etc.)
- Market overview shows all instruments including company shares

### 3. Consistent Data Model
- All tradable assets use the same structure
- Portfolio display shows everything in one view
- Order history includes all trades (items, crypto, company shares)

### 4. Extensibility
- Easy to add new instrument types
- Leverage existing price simulation for company shares
- Can apply market factors and volatility calculations

## Data Migration

### For Existing Deployments

If you have existing data in `company_shareholders` and `company_share_tx` tables:

```sql
-- 1. Create instruments for companies already on market
INSERT INTO instruments (id, type, symbol, display_name, decimals, created_at)
SELECT 
    'COMPANY_' || id,
    'EQUITY',
    symbol,
    name,
    2,
    created_at
FROM companies
WHERE on_market = 1 AND symbol IS NOT NULL;

-- 2. Create instrument state for these companies
INSERT INTO instrument_state (instrument_id, last_price, last_volume, change_1h, change_24h, volatility_24h, market_cap, updated_at)
SELECT 
    'COMPANY_' || id,
    balance / 10000.0,  -- Share price
    0.0,
    0.0,
    0.0,
    0.0,
    balance,
    strftime('%s', 'now') * 1000
FROM companies
WHERE on_market = 1;

-- 3. Migrate shareholder holdings
INSERT INTO user_holdings (player_uuid, instrument_id, qty, avg_cost)
SELECT 
    player_uuid,
    'COMPANY_' || company_id,
    shares,
    avg_cost
FROM company_shareholders
WHERE shares > 0;

-- 4. Migrate transaction history
INSERT INTO orders (id, player_uuid, instrument_id, side, qty, price, ts)
SELECT 
    id,
    player_uuid,
    'COMPANY_' || company_id,
    type,  -- BUY or SELL
    shares,
    price,
    ts
FROM company_share_tx;

-- 5. Verify migration
SELECT 
    c.name,
    c.symbol,
    COUNT(uh.player_uuid) as shareholders,
    SUM(uh.qty) as total_shares
FROM companies c
LEFT JOIN instruments i ON i.id = 'COMPANY_' || c.id
LEFT JOIN user_holdings uh ON uh.instrument_id = i.id
WHERE c.on_market = 1
GROUP BY c.id, c.name, c.symbol;
```

### Post-Migration Cleanup (Optional)

After verifying the migration was successful:

```sql
-- Backup old tables first!
CREATE TABLE company_shareholders_backup AS SELECT * FROM company_shareholders;
CREATE TABLE company_share_tx_backup AS SELECT * FROM company_share_tx;

-- Then optionally drop old tables
-- DROP TABLE company_shareholders;
-- DROP TABLE company_share_tx;
```

## Code Changes Summary

### CompanyMarketService.java
- `enableMarket()`: Creates instrument entries
- `buyShares()`: Uses `TradingService.executeBuyOrder()`
- `sellShares()`: Uses `TradingService.executeSellOrder()`
- `disableMarket()`: Cleans up instrument data
- `getShareholders()`: Queries `user_holdings` instead of `company_shareholders`

### MarketCommand.java
- `showPortfolio()`: Queries `user_holdings` joined with `instruments` (type='EQUITY')
- `showOrderHistory()`: Queries `orders` table for transactions

### QueryService.java
- `getRecentShareTransactions()`: Queries `orders` with instrument_id filter

### QuickStocksPlugin.java
- Calls `companyMarketService.setTradingServices()` to wire up dependencies

## Testing Checklist

- [ ] Create a company and set trading symbol
- [ ] Enable market (IPO) - verify instrument created
- [ ] Check `instruments` table has entry with type='EQUITY'
- [ ] Check `instrument_state` has correct initial price
- [ ] Buy shares - verify `user_holdings` updated
- [ ] Check `orders` table has BUY transaction
- [ ] Check company balance increased
- [ ] Check instrument price updated
- [ ] Sell shares - verify `user_holdings` updated
- [ ] Check `orders` table has SELL transaction
- [ ] Check company balance decreased
- [ ] View portfolio - see company shares listed
- [ ] View order history - see transactions
- [ ] Disable market - verify all shareholders paid out
- [ ] Check instrument removed from all tables

## Backwards Compatibility

Old methods are deprecated but still work:
- `getPlayerShares(companyId, playerUuid)` - internally uses `user_holdings`
- `getIssuedShares(companyId)` - internally uses `user_holdings`

The old tables (`company_shareholders`, `company_share_tx`) can remain in the database for backwards compatibility or reference, but are no longer actively used by the code.

## Future Enhancements

With company shares as instruments, we can now:
1. Apply market simulation to company share prices
2. Track volatility and price movements
3. Show company shares in market analytics (top gainers, losers, etc.)
4. Implement advanced order types (limit orders, stop-loss)
5. Create market indices that include company shares
6. Apply economic factors to company valuations

## Questions?

If you encounter issues or need clarification, refer to:
- `MARKET_UNIFICATION.md` - Original market architecture
- `MARKET_FIX_SUMMARY.md` - Previous fix summary
- `COMPANY_FEATURE.md` - Company system overview
