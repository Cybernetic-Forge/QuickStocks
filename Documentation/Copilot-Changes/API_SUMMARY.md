# QuickStocks API Summary

## Quick Reference

This document provides a quick reference for the QuickStocks API structure.

## Package Structure

```
net.cyberneticforge.quickstocks.api
├── QuickStocksAPI.java              # Main API entry point
├── events/                          # Custom Bukkit events
│   ├── CircuitBreakerTriggeredEvent.java
│   ├── CompanyCreateEvent.java
│   ├── CompanyEmployeeJoinEvent.java
│   ├── CompanyEmployeeLeaveEvent.java
│   ├── CompanyIPOEvent.java
│   ├── CompanyTransactionEvent.java
│   ├── CryptoCreateEvent.java
│   ├── InstrumentBuyEvent.java
│   ├── InstrumentPriceUpdateEvent.java
│   ├── InstrumentSellEvent.java
│   ├── MarketCloseEvent.java
│   ├── MarketOpenEvent.java
│   ├── ShareBuyEvent.java
│   ├── ShareSellEvent.java
│   ├── WalletBalanceChangeEvent.java
│   ├── WatchlistAddEvent.java
│   └── WatchlistRemoveEvent.java
└── managers/                        # Domain-specific managers
    ├── CompanyManager.java
    ├── CryptoManager.java
    ├── MarketManager.java
    ├── PortfolioManager.java
    ├── TradingManager.java
    ├── WalletManager.java
    └── WatchlistManager.java
```

## Manager Overview

| Manager | Purpose | Key Methods |
|---------|---------|-------------|
| **CompanyManager** | Company CRUD, IPO, shares, employees | `createCompany()`, `enableMarket()`, `buyShares()`, `sellShares()` |
| **TradingManager** | Buy/sell instruments, portfolio value | `buy()`, `sell()`, `getPortfolioValue()`, `getTradeHistory()` |
| **MarketManager** | Instrument data, prices, market state | `getInstrument()`, `getCurrentPrice()`, `getPriceHistory()`, `openMarket()` |
| **WalletManager** | Balance management | `getBalance()`, `addBalance()`, `subtractBalance()`, `hasBalance()` |
| **WatchlistManager** | Track favorite instruments | `addToWatchlist()`, `removeFromWatchlist()`, `getWatchlist()` |
| **CryptoManager** | Custom cryptocurrency creation | `createCrypto()`, `getCrypto()`, `getAllCryptos()` |
| **PortfolioManager** | Holdings and P&L | `getAllHoldings()`, `getPortfolioValue()`, `getTotalProfitLoss()` |

## Event Overview

### Event Categories

**Company Events (5)**
- `CompanyCreateEvent` - Company created (cancellable)
- `CompanyIPOEvent` - Company goes public (cancellable)
- `CompanyEmployeeJoinEvent` - Employee joins company
- `CompanyEmployeeLeaveEvent` - Employee leaves company
- `CompanyTransactionEvent` - Deposit/withdrawal (cancellable)

**Trading Events (4)**
- `InstrumentBuyEvent` - Player buys instrument (cancellable)
- `InstrumentSellEvent` - Player sells instrument (cancellable)
- `ShareBuyEvent` - Player buys company shares (cancellable)
- `ShareSellEvent` - Player sells company shares (cancellable)

**Market Events (4)**
- `InstrumentPriceUpdateEvent` - Price updated
- `MarketOpenEvent` - Market opened
- `MarketCloseEvent` - Market closed
- `CircuitBreakerTriggeredEvent` - Circuit breaker triggered

**Wallet Events (1)**
- `WalletBalanceChangeEvent` - Balance changed

**Watchlist Events (2)**
- `WatchlistAddEvent` - Instrument added to watchlist (cancellable)
- `WatchlistRemoveEvent` - Instrument removed from watchlist (cancellable)

**Crypto Events (1)**
- `CryptoCreateEvent` - Custom crypto created (cancellable)

## Quick Start Code

### Get API Instance
```java
QuickStocksAPI api = QuickStocksAPI.getInstance();
```

### Access Managers
```java
// Company operations
api.getCompanyManager().createCompany(uuid, "TechCorp", "PUBLIC");

// Trading operations
api.getTradingManager().buy(uuid, instrumentId, 100);

// Market data
api.getMarketManager().getCurrentPrice(instrumentId);

// Wallet operations
api.getWalletManager().getBalance(uuid);

// Watchlist operations
api.getWatchlistManager().addToWatchlist(uuid, instrumentId);

// Crypto operations
api.getCryptoManager().createCrypto(uuid, "BTC", "Bitcoin");

// Portfolio data
api.getPortfolioManager().getPortfolioValue(uuid);
```

### Listen to Events
```java
@EventHandler
public void onCompanyCreate(CompanyCreateEvent event) {
    Player creator = event.getCreator();
    String companyName = event.getCompanyName();
    // Handle event
}
```

## Integration Steps

1. **Add dependency** - Add QuickStocks to your plugin dependencies
2. **Add to plugin.yml** - Add `depend: [QuickStocks]`
3. **Get API instance** - `QuickStocksAPI.getInstance()`
4. **Use managers** - Access domain-specific managers
5. **Register event listeners** - Listen to QuickStocks events

## Event Firing Points

Events should be fired by the QuickStocks plugin at these points:

- **CompanyCreateEvent**: Before/after creating company in CompanyService
- **CompanyIPOEvent**: Before enabling market in CompanyMarketService
- **CompanyEmployeeJoinEvent**: After accepting invitation in InvitationService
- **CompanyEmployeeLeaveEvent**: After removing employee in CompanyService
- **CompanyTransactionEvent**: Before deposit/withdraw in CompanyService
- **InstrumentBuyEvent**: Before executing buy in TradingService
- **InstrumentSellEvent**: Before executing sell in TradingService
- **ShareBuyEvent**: Before buying shares in CompanyMarketService
- **ShareSellEvent**: Before selling shares in CompanyMarketService
- **InstrumentPriceUpdateEvent**: After price update in SimulationEngine
- **MarketOpenEvent**: After opening market in StockMarketService
- **MarketCloseEvent**: After closing market in StockMarketService
- **CircuitBreakerTriggeredEvent**: When circuit breaker triggers
- **WalletBalanceChangeEvent**: After balance changes in WalletService
- **WatchlistAddEvent**: Before adding to watchlist in WatchlistService
- **WatchlistRemoveEvent**: Before removing from watchlist in WatchlistService
- **CryptoCreateEvent**: Before creating crypto in CryptoService

## Domain Coverage

The API covers all major QuickStocks domains:

✅ **Companies** - Full lifecycle (create, manage, IPO, shares, employees, jobs)
✅ **Trading** - Buy/sell instruments and shares
✅ **Market** - Instrument data, prices, history, market control
✅ **Wallet** - Balance management
✅ **Watchlist** - Tracking favorite instruments
✅ **Crypto** - Custom cryptocurrency creation
✅ **Portfolio** - Holdings and profit/loss tracking

## Error Handling

All methods that interact with the database throw `SQLException`. External plugins should handle these:

```java
try {
    double balance = api.getWalletManager().getBalance(playerUuid);
} catch (SQLException e) {
    // Handle database error
}
```

## Thread Safety

- All manager methods are designed to be thread-safe
- Database operations use connection pooling
- Events are fired on the main thread (Bukkit standard)

## Next Steps for QuickStocks Plugin

To complete the API integration, the QuickStocks plugin needs to:

1. **Initialize API** - Call `QuickStocksAPI.initialize()` in `onEnable()`
2. **Fire events** - Add event firing to service methods
3. **Reset API** - Call `QuickStocksAPI.reset()` in `onDisable()`

Example initialization in QuickStocksPlugin.java:
```java
@Override
public void onEnable() {
    // ... existing initialization ...
    
    // Initialize API for external plugins
    QuickStocksAPI.initialize(
        companyService,
        companyMarketService,
        tradingService,
        holdingsService,
        stockMarketService,
        instrumentService,
        walletService,
        watchlistService,
        cryptoService
    );
    
    getLogger().info("QuickStocks API initialized for external plugins");
}

@Override
public void onDisable() {
    // ... existing cleanup ...
    
    // Reset API
    QuickStocksAPI.reset();
}
```

## Documentation

See `API_DOCUMENTATION.md` for complete documentation including:
- Detailed usage examples for each manager
- Complete event documentation with examples
- Error handling patterns
- Best practices
- Complete example plugin

## Support

For questions or issues with the API:
- GitHub Issues: https://github.com/Cybernetic-Forge/QuickStocks/issues
- Main Documentation: See `/Documentation` folder
