# QuickStocks API

This folder contains the public API for external plugins to hook into QuickStocks.

## Structure

- **`QuickStocksAPI.java`** - Main API entry point and singleton instance
- **`events/`** - Custom Bukkit events (17 events)
- **`managers/`** - Domain-specific manager classes (7 managers)

## Usage

```java
import net.cyberneticforge.quickstocks.api.QuickStocksAPI;

// Get API instance
QuickStocksAPI api = QuickStocksAPI.getInstance();

// Access managers
api.getCompanyManager().createCompany(uuid, "TechCorp", "PUBLIC");
api.getTradingManager().buy(uuid, instrumentId, 100);
api.getMarketManager().getCurrentPrice(instrumentId);
```

## Managers

- **CompanyManager** - Company operations (create, IPO, shares, employees)
- **TradingManager** - Trading operations (buy/sell)
- **MarketManager** - Market data and control
- **WalletManager** - Balance management
- **WatchlistManager** - Watchlist operations
- **CryptoManager** - Custom crypto creation
- **PortfolioManager** - Portfolio and P&L tracking

## Events

All events extend Bukkit's `Event` class and can be listened to using `@EventHandler`.

**Categories:**
- Company events (5)
- Trading events (4)
- Market events (4)
- Wallet events (1)
- Watchlist events (2)
- Crypto events (1)

## Documentation

See the repository root for complete documentation:
- `API_DOCUMENTATION.md` - Complete API reference with examples
- `API_SUMMARY.md` - Quick reference guide

## Integration

Add QuickStocks as a dependency in your `plugin.yml`:

```yaml
name: YourPlugin
depend: [QuickStocks]
```

## Thread Safety

All API methods are thread-safe and can be called from any thread. However, events are always fired on the main thread (Bukkit standard).

## Error Handling

Methods that interact with the database throw `SQLException`. Always handle these appropriately:

```java
try {
    api.getTradingManager().buy(uuid, instrumentId, 100);
} catch (SQLException e) {
    // Handle error
}
```
