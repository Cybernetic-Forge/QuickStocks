# Configuration File Migration Guide

## Summary

The QuickStocks configuration has been reorganized into separate files for better maintainability and organization. This change follows the same pattern used by `GuiConfig` with `guis.yml`.

## What Changed

### Before
All configuration was in a single `config.yml` file (133 lines).

### After
Configuration is split into multiple focused files:
- **config.yml** (36 lines) - Database, features, and metrics
- **market.yml** (35 lines) - Market settings, market device, and analytics
- **trading.yml** (20 lines) - Trading economy configuration
- **companies.yml** (47 lines) - Companies/corporations system

## New Configuration Classes

Three new configuration manager classes were created following the `YamlParser` pattern:

### 1. MarketCfg (`infrastructure/config/MarketCfg.java`)
Manages configuration from `market.yml`:
- Market settings (updateInterval, startOpen, defaultStocks)
- Price threshold configuration (dampening, volume sensitivity)
- Market device recipe settings
- Analytics configuration (lambda, windows)

**Usage:**
```java
MarketCfg marketCfg = QuickStocksPlugin.getMarketCfg();
int updateInterval = marketCfg.getUpdateInterval();
double lambda = marketCfg.getAnalyticsLambda();
```

### 2. TradingCfg (`infrastructure/config/TradingCfg.java`)
Manages configuration from `trading.yml`:
- Fee configuration (mode, percent, flat)
- Trading limits (maxOrderQty, cooldowns)
- Circuit breakers (levels, halt durations)
- Order types (market, limit, stop)
- Slippage settings

**Usage:**
```java
TradingCfg tradingCfg = QuickStocksPlugin.getTradingCfg();
TradingConfig config = tradingCfg.getTradingConfig();
double feePercent = config.getFee().getPercent();
```

### 3. CompanyCfg (`infrastructure/config/CompanyCfg.java`)
Manages configuration from `companies.yml`:
- Basic settings (enabled, creationCost, types)
- Job titles and permissions
- Salary configuration (payment cycles, offline payment)
- ChestShop integration settings

**Usage:**
```java
CompanyCfg companyCfg = QuickStocksPlugin.getCompanyCfg();
CompanyConfig config = companyCfg.getCompanyConfig();
double creationCost = config.getCreationCost();
```

## Migration for Existing Installations

1. **Automatic Migration**: On first run with the new version, the plugin will:
   - Create the new config files (market.yml, trading.yml, companies.yml) with default values
   - Update config.yml to remove migrated sections
   - Add a note in config.yml pointing to the new files

2. **Manual Steps** (if you have custom settings):
   - Review your old config.yml backup
   - Copy your custom settings to the appropriate new files
   - Restart the server or use `/quickstocks reload`

## Code Changes

### QuickStocksPlugin.java
- Added static getters for new config managers
- Initialization code updated to load all config files
- Analytics service now uses values from MarketCfg

### ConfigLoader.java
- Updated to read from both config.yml and market.yml
- Maintains backward compatibility for price threshold settings

## Benefits

1. **Better Organization**: Related settings are grouped together
2. **Easier Maintenance**: Smaller, focused configuration files
3. **Follows Established Pattern**: Uses same YamlParser pattern as GuiConfig
4. **Cleaner Separation**: Market, trading, and company concerns are separate
5. **Improved Documentation**: Each file can be documented independently

## Rollback

If needed, you can merge all the separate files back into config.yml by:
1. Copying contents of market.yml, trading.yml, and companies.yml
2. Removing the NOTE comment from config.yml
3. Reverting to the previous plugin version

## Testing

All YAML files have been validated for correct syntax. The configuration classes follow the same pattern as the existing GuiConfig implementation, ensuring consistency and reliability.
