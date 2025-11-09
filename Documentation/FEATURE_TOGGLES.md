# Feature Toggle Implementation

## Overview
This document describes the feature toggle implementation for QuickStocks, allowing server administrators to disable entire systems or specific sub-features.

## Configuration Files

### companies.yml
- **companies.enabled**: Enable/disable the entire companies/corporations system
  - Default: `true`
  - When disabled: `/company` command is not registered, company listeners are not registered

### market.yml
- **market.enabled**: Enable/disable the entire market system
  - Default: `true`
  - When disabled: All market-related commands and listeners are not registered
  
#### Market Sub-features (market.features.*)
- **watchlist**: Enable/disable watchlist functionality (`/watch` command)
  - Default: `true`
  - Controls: `/watch` command registration, watchlist subcommands in `/market`

- **portfolio**: Enable/disable portfolio viewing
  - Default: `true`
  - Controls: Portfolio and history subcommands in `/market`, portfolio GUI

- **trading**: Enable/disable buying/selling shares
  - Default: `true`
  - Controls: Buy/sell subcommands in `/market`

- **marketDevice**: Enable/disable market device item
  - Default: `true`
  - Controls: `/marketdevice` command, market device listener, device crafting

- **stocksCommand**: Enable/disable /stocks command
  - Default: `true`
  - Controls: `/stocks` command registration

- **cryptoCommand**: Enable/disable /crypto command (custom cryptocurrency creation)
  - Default: `true`
  - Controls: `/crypto` command registration

## Implementation Details

### Command Checks
All commands check if their respective feature is enabled at the beginning of `onCommand()`:
- `CompanyCommand`: Checks `companyCfg.isEnabled()`
- `MarketCommand`: Checks `marketCfg.isEnabled()`
- `StocksCommand`: Checks `marketCfg.isStocksCommandEnabled()`
- `WatchCommand`: Checks `marketCfg.isWatchlistEnabled()`
- `MarketDeviceCommand`: Checks `marketCfg.isMarketDeviceEnabled()`
- `CryptoCommand`: Checks `marketCfg.isCryptoCommandEnabled()`

Sub-feature checks within MarketCommand:
- Buy/Sell: Checks `marketCfg.isTradingEnabled()`
- Portfolio/History: Checks `marketCfg.isPortfolioEnabled()`
- Watchlist: Checks `marketCfg.isWatchlistEnabled()`

### Command Registration (QuickStocksPlugin.registerCommands())
Commands are conditionally registered based on feature flags:
```java
// Only register market-related commands if market system is enabled
if (marketCfg.isEnabled()) {
    registerCommand("market", new MarketCommand(...));
    if (marketCfg.isMarketDeviceEnabled()) {
        registerCommand("marketdevice", new MarketDeviceCommand());
    }
    if (marketCfg.isWatchlistEnabled()) {
        registerCommand("watch", new WatchCommand());
    }
    if (marketCfg.isStocksCommandEnabled()) {
        registerCommand("stocks", new StocksCommand());
    }
}

// Only register company command if companies system is enabled
if (companyCfg.isEnabled()) {
    registerCommand("company", new CompanyCommand());
}
```

### Listener Registration (QuickStocksPlugin.registerListeners())
Listeners are conditionally registered based on feature flags:
```java
// Only register market-related listeners if market system is enabled
if (marketCfg.isEnabled()) {
    if (marketCfg.isMarketDeviceEnabled()) {
        // Register MarketDeviceListener
    }
    if (marketCfg.isPortfolioEnabled() || marketCfg.isTradingEnabled()) {
        // Register MarketGUIListener, PortfolioGUIListener
    }
}

// Only register company-related listeners if companies system is enabled
if (companyCfg.isEnabled()) {
    // Register CompanySettingsGUIListener
    if (companyCfg.isChestShopEnabled() && hookManager.isHooked(...)) {
        // Register ChestShop listeners
    }
}
```

## Translation Messages

### New Translation Keys
- `General.FeatureDisabled`: Generic message when a feature is disabled
- `General.CompaniesDisabled`: Specific message for companies system
- `General.MarketDisabled`: Specific message for market system

### Translations.yml
```yaml
General:
  FeatureDisabled: '&cThis feature is currently disabled.'
  CompaniesDisabled: '&cThe companies system is currently disabled.'
  MarketDisabled: '&cThe market system is currently disabled.'
```

## Testing

### Test Scenarios

1. **Disable Companies System**
   - Set `companies.enabled: false` in companies.yml
   - Restart server
   - Expected: `/company` command not available, no company listeners registered

2. **Disable Market System**
   - Set `market.enabled: false` in market.yml
   - Restart server
   - Expected: All market commands not available, no market listeners registered

3. **Disable Watchlist Sub-feature**
   - Set `market.features.watchlist: false` in market.yml
   - Keep `market.enabled: true`
   - Restart server
   - Expected: `/watch` command not available, `/market watchlist` shows disabled message

4. **Disable Trading Sub-feature**
   - Set `market.features.trading: false` in market.yml
   - Keep `market.enabled: true`
   - Restart server
   - Expected: `/market buy` and `/market sell` show disabled message

5. **Disable Market Device**
   - Set `market.features.marketDevice: false` in market.yml
   - Restart server
   - Expected: `/marketdevice` command not available, device listener not registered

## Benefits

1. **Full Control**: Server admins can disable entire systems they don't want to use
2. **Fine-grained Control**: Sub-features can be disabled individually for customization
3. **Performance**: Disabled features don't register commands or listeners, reducing overhead
4. **Security**: Disabled features can't be accessed even if players know the commands
5. **User Experience**: Clear messages inform players when features are disabled

## Backward Compatibility

All feature flags default to `true`, ensuring existing installations continue to work without changes.
