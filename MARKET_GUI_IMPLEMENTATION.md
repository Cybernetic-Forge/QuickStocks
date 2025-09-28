# Market GUI Implementation

This document describes the implementation of the market GUI with holdings & orders functionality for QuickStocks.

## Overview

The implementation adds a complete trading system with player portfolios, wallet management, and order execution. Players can browse instruments, buy/sell shares with confirmations, and manage their portfolios.

## Database Schema Changes

### V2 Migration: Holdings & Orders

Added `V2__holdings_orders.sql` migration with three new tables:

```sql
-- Player holdings for instruments (portfolio)
CREATE TABLE IF NOT EXISTS user_holdings (
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  qty           REAL NOT NULL DEFAULT 0,
  avg_cost      REAL NOT NULL DEFAULT 0,
  PRIMARY KEY (player_uuid, instrument_id),
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

-- Order history and tracking
CREATE TABLE IF NOT EXISTS orders (
  id            TEXT PRIMARY KEY,
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  side          TEXT NOT NULL,     -- BUY | SELL
  qty           REAL NOT NULL,
  price         REAL NOT NULL,
  ts            INTEGER NOT NULL,
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_player_ts ON orders(player_uuid, ts);

-- Internal wallet system for economy (fallback if Vault not available)
CREATE TABLE IF NOT EXISTS wallets (
  player_uuid TEXT PRIMARY KEY,
  balance     REAL NOT NULL DEFAULT 0
);
```

## Core Services

### WalletService
- **Purpose**: Manages player wallet balances
- **Features**:
  - Vault economy integration (with fallback to internal system)
  - Balance validation and transactions
  - Thread-safe operations
- **Key Methods**:
  - `getBalance(playerUuid)` - Get current balance
  - `addBalance(playerUuid, amount)` - Add funds
  - `removeBalance(playerUuid, amount)` - Remove funds with validation
  - `hasBalance(playerUuid, amount)` - Check sufficient funds

### HoldingsService
- **Purpose**: Manages player portfolios and positions
- **Features**:
  - Position tracking with average cost calculation
  - Portfolio value calculation
  - Automatic position cleanup (removes zero positions)
- **Key Methods**:
  - `getHoldings(playerUuid)` - Get all holdings
  - `addHolding(playerUuid, instrumentId, qty, price)` - Add shares (BUY)
  - `removeHolding(playerUuid, instrumentId, qty)` - Remove shares (SELL)
  - `getPortfolioValue(playerUuid)` - Calculate total portfolio value

### TradingService
- **Purpose**: Executes market orders and maintains order history
- **Features**:
  - Market order execution at current prices
  - Transaction safety with rollback on failures
  - Order history tracking
  - Balance and position validation
- **Key Methods**:
  - `executeBuyOrder(playerUuid, instrumentId, qty)` - Execute buy order
  - `executeSellOrder(playerUuid, instrumentId, qty)` - Execute sell order
  - `getOrderHistory(playerUuid, limit)` - Get recent orders

## Command Interface

### /wallet Command
```
/wallet [balance|add|set] [amount]
```
- **balance**: Show current wallet balance
- **add**: Add money to wallet (requires permission)
- **set**: Set wallet balance (requires permission)

**Permissions**:
- `quickstocks.wallet.add` (default: op)
- `quickstocks.wallet.set` (default: op)

### /market Command
```
/market [browse|buy|sell|portfolio|history|confirm]
```

#### Market Browse
- Shows top 10 gainers with color-coded price changes
- Displays symbol, name, current price, and 24h change
- Green ▲ for gains, red ▼ for losses

#### Buy/Sell Orders
1. **Initiation**: `/market buy SYMBOL QTY` or `/market sell SYMBOL QTY`
   - Shows order preview with current price and total cost/value
   - Validates balance (buy) or holdings (sell)
   - Provides confirmation command

2. **Confirmation**: `/market confirm buy SYMBOL QTY`
   - Executes the actual trade
   - Updates wallet balance and holdings
   - Records order in history

#### Portfolio Management
- Shows cash balance, portfolio value, and total assets
- Lists all holdings with:
  - Current quantity and average cost
  - Current market price
  - Unrealized P&L (dollar amount and percentage)
  - Color-coded gains/losses

#### Order History
- Shows recent trading activity
- Displays order side, symbol, quantity, price, and timestamp
- Color-coded BUY (green) and SELL (red) orders

## Trading Logic

### Buy Order Execution
1. Get current price from `instrument_state`
2. Calculate total cost (`quantity × price`)
3. Validate sufficient wallet balance
4. Debit wallet balance
5. Add shares to holdings (with average cost calculation)
6. Record order in history
7. Rollback on any failure

### Sell Order Execution
1. Get current price from `instrument_state`
2. Validate sufficient holdings quantity
3. Calculate total value (`quantity × price`)
4. Remove shares from holdings
5. Credit wallet balance
6. Record order in history

### Average Cost Calculation
When buying shares of an existing position:
```
new_avg_cost = (existing_qty * existing_avg_cost + new_qty * new_price) / (existing_qty + new_qty)
```

## Implementation Details

### Database Persistence
- All services use the existing `Db` abstraction layer
- Transactions ensure data consistency
- Foreign key constraints maintain referential integrity

### Error Handling
- Comprehensive validation before order execution  
- Rollback mechanisms for failed transactions
- Clear error messages to players

### Performance Considerations
- Indexed order history queries
- Efficient portfolio value calculations
- Minimal database round trips

## Usage Examples

### Trading Flow
```
Player: /market browse
System: Shows top gainers list

Player: /market buy AAPL 10
System: Preview: Buy 10 shares of AAPL @ $150.00 = $1500.00
        Your balance: $2000.00 → $500.00
        Type: /market confirm buy AAPL 10

Player: /market confirm buy AAPL 10  
System: ✅ Bought 10 shares of AAPL @ $150.00

Player: /market portfolio
System: Shows updated holdings and P&L
```

### Wallet Management
```
Player: /wallet
System: Your balance: $1,000.00

Admin: /wallet add PlayerName 500
System: Added $500.00 to PlayerName's wallet

Player: /wallet  
System: Your balance: $1,500.00
```

## Future Enhancements

### Limit Orders
- Database schema ready with `orders` table
- Could implement pending orders with price triggers
- Would require background processing

### Advanced Portfolio Analytics
- Historical performance tracking
- Dividend/split handling  
- Sector allocation analysis

### Integration Opportunities
- Vault economy plugin integration
- GUI-based trading interface
- Real-time price alerts

## Testing Strategy

### Unit Tests
- Service layer validation
- Database transaction integrity
- Edge case handling (insufficient funds, etc.)

### Integration Tests  
- Full trading flow validation
- Database migration testing
- Command interface testing

### Performance Tests
- Portfolio calculation efficiency
- Concurrent trading scenarios
- Database query optimization

## Deployment Notes

### Database Migration
- V2 migration runs automatically on plugin startup
- Existing data remains intact
- New tables created with proper indexes

### Configuration
- No additional configuration required
- Uses existing database settings
- Vault integration automatic if available

### Permissions
- New permissions added to `plugin.yml`
- Default settings ensure proper access control
- Admin commands restricted to operators

### Existing Systems
- Uses existing `instruments` and `instrument_state` tables
- Integrates with simulation engine for live prices  
- Compatible with existing `/stocks` command
- Works with crypto creation system

### Plugin Integration
- All commands registered in `plugin.yml`
- Services initialized in main plugin class
- Database migrations run automatically
- Permissions system integration

This implementation provides a solid foundation for a complete trading system within Minecraft, with room for future enhancements and integrations.