# Market Unification - Company Shares as Market Stocks

## Overview

The market system has been unified to use **company shares** as the primary market stocks. Previously, there was confusion between traditional "instruments" (items, crypto, etc.) and company shares. This has now been clarified.

## Key Changes

### 1. Market Command Unification

The `/market` command now exclusively works with **company shares**:

- **`/market buy <company> <quantity>`** - Buy company shares (formerly `/market buyshares`)
- **`/market sell <company> <quantity>`** - Sell company shares (formerly `/market sellshares`)
- **`/market shareholders <company>`** - View company shareholders
- **`/market portfolio`** - View your company share holdings
- **`/market history`** - View your company share transaction history
- **`/market browse`** - Browse companies on the market

### 2. Removed Commands

The following commands have been **removed** as they're now unified:
- ~~`/market buyshares`~~ → Use `/market buy`
- ~~`/market sellshares`~~ → Use `/market sell`
- ~~`/market confirm`~~ → Direct purchase, no confirmation needed

### 3. System Architecture

#### Market Stocks (Company Shares)
- **Command**: `/market`
- **Service**: `CompanyMarketService`
- **Tables**: `companies`, `company_shareholders`, `company_share_tx`
- **Purpose**: Trading company shares on the market

#### Traditional Instruments (Items, Crypto, etc.)
- **Command**: `/stocks`, `/crypto`, `/watch`
- **Service**: `TradingService`, `QueryService`
- **Tables**: `instruments`, `instrument_state`, `user_holdings`
- **Purpose**: Tracking item prices, crypto, and other non-company financial instruments

### 4. Database Changes

#### Company Share Holdings
```sql
-- Player holdings are now in company_shareholders table
SELECT cs.shares, cs.avg_cost, c.name, c.symbol
FROM company_shareholders cs
JOIN companies c ON cs.company_id = c.id
WHERE cs.player_uuid = ?
```

#### Share Transaction History
```sql
-- Transaction history is in company_share_tx table
SELECT tx.type, tx.shares, tx.price, tx.ts, c.name
FROM company_share_tx tx
JOIN companies c ON tx.company_id = c.id
WHERE tx.player_uuid = ?
ORDER BY tx.ts DESC
```

### 5. Service Updates

#### CompanyMarketService
- **Primary service for market operations**
- Handles buying/selling company shares
- Calculates share prices based on company balance
- Manages shareholder records
- Handles IPO (Initial Public Offering) and delisting

#### QueryService
- **Added company-specific query methods**:
  - `getTopCompaniesOnMarket()` - Top companies by balance
  - `findCompanyBySymbol()` - Find company by symbol
  - `getMarketCompanySymbols()` - Company symbols for tab completion
  - `getCompanySharePrice()` - Current share price
  - `getRecentShareTransactions()` - Transaction history

- **Legacy methods** (for instruments):
  - `getTopGainers()` - For traditional instruments
  - `findBySymbol()` - For traditional instruments
  - Note: These methods are still used by `/stocks`, `/crypto`, etc.

#### TradingService
- **Now documented as instrument-only**
- Used for traditional instrument trading
- **NOT used for market stocks** (company shares)
- Market stocks use `CompanyMarketService`

### 6. Tab Completion

Tab completion now shows **company symbols** for market commands:
```java
/market buy <TAB>  // Shows: TECH, MSFT, APPL (company symbols on market)
/market sell <TAB> // Shows: TECH, MSFT, APPL (company symbols on market)
```

## Usage Examples

### Buying Company Shares
```
/market buy TechCorp 100
/market buy TECH 100      # Using symbol
```

### Selling Company Shares
```
/market sell TechCorp 50
/market sell TECH 50      # Using symbol
```

### Viewing Portfolio
```
/market portfolio
```
Output shows:
- Cash balance
- Portfolio value (total company shares)
- Individual company holdings with P&L

### Viewing Transaction History
```
/market history
```
Output shows recent buy/sell transactions of company shares.

### Browsing Market
```
/market browse
/market list
/market
```
Opens GUI showing all companies on the market.

## Migration Notes

### For Players
- Old `/market buyshares` → Use `/market buy`
- Old `/market sellshares` → Use `/market sell`
- Holdings automatically preserved in `company_shareholders` table

### For Developers
- Use `CompanyMarketService` for all market stock operations
- `QueryService` has new company-specific methods
- `TradingService` is now explicitly for instruments only
- Portfolio display queries `company_shareholders` instead of `user_holdings`

## Technical Details

### Share Pricing
Each company has 10,000 shares:
```
Share Price = Company Balance ÷ 10,000
```

Example: If company balance is $50,000, each share costs $5.00

### Market Percentage
Controls how many shares can be sold:
- 70% (default) = 7,000 shares available for trading
- 100% = all 10,000 shares available

### Holdings Tracking
- Player shareholdings stored in `company_shareholders`
- Average cost tracking for P&L calculations
- Real-time price based on company balance

### Transaction History
- All buy/sell transactions logged in `company_share_tx`
- Includes timestamp, price, quantity
- Used for history display and analytics

## Future Enhancements

Potential improvements:
- Market analytics dashboard
- Company performance metrics
- Dividend distribution system
- Advanced order types (limit orders, stop-loss)
- Portfolio performance tracking
- Market indices (top 10, most active, etc.)

## Related Documentation

- [COMPANY_FEATURE.md](COMPANY_FEATURE.md) - Company system overview
- [COMPANY_MARKET_GUIDE.md](COMPANY_MARKET_GUIDE.md) - Company market operations
- Database migrations: `V8__companies.sql`, `V9__company_market.sql`
