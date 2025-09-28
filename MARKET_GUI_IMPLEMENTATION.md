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
2. Validate sufficient holdings
3. Remove shares from holdings
4. Credit wallet balance
5. Record order in history  
6. Rollback on any failure

### Average Cost Calculation
For buy orders that add to existing positions:
```java
double totalValue = (existingQty × existingAvgCost) + (newQty × newPrice);
double newAvgCost = totalValue / (existingQty + newQty);
```

## Economy Integration

### Vault Integration (Future)
- Placeholder methods for Vault economy plugin integration
- Automatic fallback to internal wallet system
- Configurable via service initialization

### Internal Wallet System
- SQLite-based wallet storage
- Default balance: $0.00
- Admin commands for balance management

## Usage Examples

### Basic Trading Flow
```bash
# Check balance
/wallet balance

# Add starting funds (admin)
/wallet add 10000

# Browse market
/market browse

# Buy shares with confirmation
/market buy AAPL 10
/market confirm buy AAPL 10

# Check portfolio
/market portfolio

# Sell shares
/market sell AAPL 5
/market confirm sell AAPL 5

# View trading history
/market history
```

### Administrative Tasks
```bash
# Set player balance
/wallet set 5000

# Add funds to player
/wallet add 1000
```

## Error Handling

### Trade Validation
- **Insufficient Funds**: Buy orders validate wallet balance
- **Insufficient Shares**: Sell orders validate holdings
- **Invalid Instruments**: Symbol lookup validation
- **Invalid Quantities**: Positive number validation

### Transaction Safety
- Rollback mechanisms for failed trades
- Database transaction-like behavior
- Detailed error messages to players

## Performance Considerations

### Database Optimization
- Indexed player UUID and timestamp columns
- Efficient JOIN queries for portfolio views
- Separate state tables for fast price lookups

### Memory Management
- Lazy loading of portfolio data
- Limited order history results
- Connection pooling via HikariCP

## Testing

A demo program `MarketTradingDemo.java` demonstrates:
- Wallet operations
- Portfolio management
- Buy/sell order execution
- Order history tracking
- P&L calculations

## Future Enhancements

### Planned Features
- Order types (limit orders, stop-loss)
- Real-time price alerts
- Portfolio analytics and charts
- Multi-currency support
- Market maker functionality

### GUI Enhancements
- Inventory-based GUI for browsing
- Click-to-trade interfaces
- Real-time portfolio updates
- Advanced filtering and search

## Integration Points

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