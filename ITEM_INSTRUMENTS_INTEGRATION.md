# ItemInstruments Integration in Market GUI

## Overview
This document describes the integration of ItemInstruments (Minecraft materials as tradeable assets) into the QuickStocks market GUI system.

## Changes Made

### 1. Filter System Update
**File: `src/main/java/net/cyberneticforge/quickstocks/gui/MarketGUI.java`**

- **Renamed filter modes:**
  - `SHARES` → `COMPANY_SHARES`
  - `CRYPTO` → `CRYPTO_SHARES`
  
- **Added new filter mode:**
  - `ITEM_SHARES` - Filter to show only item instruments

- **Updated filter toggle cycle:**
  - ALL → COMPANY_SHARES → CRYPTO_SHARES → ITEM_SHARES → ALL

### 2. Item Display in GUI
**File: `src/main/java/net/cyberneticforge/quickstocks/gui/MarketGUI.java`**

- Added `createItemInstrumentItem()` method to render item instruments
- Item instruments display with their actual Minecraft material as the icon
- Price, 24h change, and volume information shown in lore
- Integrated with `InstrumentPersistenceService` to query ITEM type instruments

### 3. Trading Support
**File: `src/main/java/net/cyberneticforge/quickstocks/listeners/MarketGUIListener.java`**

- **New routing logic:**
  - `handleInstrumentClick()` - Routes clicks based on instrument type
  - `handleCompanyShareClick()` - Handles company shares (existing logic)
  - `handleGenericInstrumentClick()` - Handles crypto and item instruments
  
- **Trading operations:**
  - Left-click: Quick buy 1 unit
  - Right-click: Quick sell 1 unit
  - Shift+click: Prompt for custom amount
  - Middle/other click: Show instrument details
  
- **Integration:**
  - Uses `TradingService` for generic instrument trades
  - Uses `CompanyMarketService` for company shares
  - Full error handling and user feedback

### 4. Configurable Item Seeding
**File: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/market/ItemSeederService.java`**

- **Configuration source:**
  - Reads from `market.yml` under `market.items.seedItems`
  - Falls back to hardcoded defaults if config unavailable
  
- **Customization:**
  - Set any Minecraft material with custom initial price
  - Set price to 0 or omit to skip seeding that item
  - Easy to add/remove items from the seed list

**File: `src/main/resources/market.yml`**

```yaml
market:
  items:
    enabled: true
    seedOnStartup: true
    seedItems:
      DIAMOND: 100.0
      EMERALD: 80.0
      GOLD_INGOT: 50.0
      # ... more items
```

### 5. GUI Configuration
**File: `src/main/resources/guis.yml`**

- **Updated filter configurations:**
  ```yaml
  filter:
    all: { name: '&6Filter: &eALL', material: COMPASS }
    company_shares: { name: '&6Filter: &eCOMPANY SHARES', material: PAPER }
    crypto_shares: { name: '&6Filter: &eCRYPTO', material: GOLD_NUGGET }
    item_shares: { name: '&6Filter: &eITEM SHARES', material: DIAMOND }
  ```

- **Added item instrument display config:**
  ```yaml
  item_instrument:
    name: '&b{display_name} &7({symbol})'
    lore:
      - '&7Type: &bItem Instrument'
      - '&ePrice: &f${price}'
      - '&e24h Change: {change_color}{change_symbol}{change_24h}%'
      - '&e24h Volume: &f{volume}'
  ```

### 6. Translation Messages
**Files: `Translation.java`, `Translations.yml`**

- **New message keys:**
  - `Market_InstrumentDetails` - Shows instrument information
  - `Market_Error_InstrumentNotFound` - Unknown instrument error
  - `Market_Error_PriceNotAvailable` - Price unavailable error

## Usage

### For Players
1. Open the market GUI with `/market` command or Market Link Device
2. Click the filter button (slot 4) to cycle through filter modes
3. Select ITEM_SHARES to see only tradeable Minecraft items
4. Left-click an item to buy 1 unit
5. Right-click an item to sell 1 unit
6. Shift+click for custom amounts

### For Server Administrators

#### Customizing Seeded Items
Edit `market.yml`:

```yaml
market:
  items:
    seedItems:
      # Add new items
      ANCIENT_DEBRIS: 250.0
      
      # Remove items by commenting out or setting to 0
      # COBBLESTONE: 0
      
      # Adjust prices
      DIAMOND: 150.0  # Changed from 100.0
```

#### Enabling/Disabling Item Trading
```yaml
market:
  items:
    enabled: true  # Set to false to disable item instruments
    seedOnStartup: true  # Set to false to disable automatic seeding
```

## Technical Details

### Database Schema
Item instruments use the existing `instruments` table:
- `type`: 'ITEM'
- `symbol`: 'MC_<MATERIAL_NAME>' (e.g., 'MC_DIAMOND')
- `display_name`: Formatted name (e.g., 'Diamond')
- `mc_material`: Minecraft material name (e.g., 'DIAMOND')
- `decimals`: 0 (items use whole units)

### Price Management
- Item instruments use the same price tracking as other instruments
- `instrument_state` table stores current price, volume, changes
- `instrument_price_history` table stores historical data
- Prices can be affected by market simulation if enabled

### Trading System
- Uses `TradingService.executeBuyOrder()` and `executeSellOrder()`
- Includes fee calculations if configured
- Supports slippage and circuit breakers if configured
- Records transactions in player holdings

## Compatibility

- **Backward Compatible:** All existing functionality preserved
- **Config Migration:** Old configs work with new features disabled by default
- **Database:** Uses existing schema, no migration needed
- **API:** No breaking changes to public API

## Testing Checklist

- [x] Filter cycling works correctly
- [ ] Item instruments display in GUI with correct materials
- [ ] Buy/sell operations work for items
- [ ] Configuration loading works correctly
- [ ] Fallback to defaults works when config missing
- [ ] Translation messages display correctly
- [ ] Holdings show item instruments correctly
- [ ] Portfolio GUI includes item holdings
- [ ] Market simulation affects item prices (if enabled)

## Future Enhancements

1. **Item-specific features:**
   - Link item price to actual item availability in chest shops
   - Volume-based pricing based on item scarcity
   - Seasonal price variations

2. **Trading enhancements:**
   - Bulk buy/sell from inventory
   - Trade items directly from inventory
   - Auto-sell items on pickup

3. **GUI improvements:**
   - Sort items by price, change, volume
   - Search/filter items by name
   - Favorite items for quick access

## Support

For issues or questions:
1. Check the configuration files are valid YAML
2. Verify item materials are valid Minecraft materials
3. Check server logs for errors
4. Ensure `market.items.enabled` is true
5. Verify database is accessible and migrations ran

## Contributors

- Implementation: GitHub Copilot
- Testing: [TBD]
- Documentation: GitHub Copilot
