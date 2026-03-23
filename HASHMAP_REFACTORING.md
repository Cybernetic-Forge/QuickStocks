# HashMap-Based Click Handling Refactoring

## Overview
Refactored the Market GUI click handling system from Material-based checks and string parsing to a high-performance HashMap approach.

## Changes Made

### 1. New SlotInstrument Class
```java
public static class SlotInstrument {
    public final String symbol;
    public final String type; // "COMPANY", "CRYPTO", "ITEM"
    public final Object data; // Company, Crypto, or Instrument object
}
```

Stores complete instrument metadata in the GUI for direct access.

### 2. HashMap-Based Storage
```java
private final Map<Integer, SlotInstrument> slotInstrumentMap = new HashMap<>();
```

- **Populated once** when GUI is created/refreshed
- **O(1) lookup** performance
- **No parsing** of display names
- **No database queries** on click

### 3. Click Handling Improvements

#### Before (Material-Based):
```java
// Had to check material type for every button
if (slot == 45 && item.getType() == Material.CLOCK) {
    marketGUI.refresh();
}

// Had to parse strings to get symbol
String symbol = marketGUI.getStockSymbolFromSlot(slot);
// Then do multiple database lookups to find instrument type
Optional<Company> companyOpt = getCompanyService().getCompanyByNameOrSymbol(symbol);
if (companyOpt.isPresent()) { ... }
Optional<Instrument> instrumentOpt = getInstrumentService().getInstrumentBySymbol(symbol);
if (instrumentOpt.isPresent()) { ... }
```

#### After (HashMap-Based):
```java
// Direct slot check, material doesn't matter
if (slot == 45) {
    marketGUI.refresh();
}

// Direct HashMap lookup with all data
SlotInstrument si = marketGUI.getInstrumentFromSlot(slot);
switch (si.type) {
    case "COMPANY":
        Company company = (Company) si.data;
        handleCompanyShareClick(player, company, clickType);
        break;
    case "CRYPTO":
        Crypto crypto = (Crypto) si.data;
        handleCryptoClick(player, crypto, clickType);
        break;
    case "ITEM":
        Instrument item = (Instrument) si.data;
        handleItemClick(player, item, clickType);
        break;
}
```

## Performance Improvements

### Before:
1. Click detected
2. Parse item display name (string operations)
3. Query CompanyService by symbol (database query)
4. If not found, query InstrumentPersistenceService (another database query)
5. Handle based on result

**Total: ~2-3 database queries per click**

### After:
1. Click detected
2. HashMap lookup by slot (O(1))
3. Handle based on type (no queries)

**Total: 0 database queries per click**

## Enhanced Display Information

### Added to All Instruments:
- **Instrument Type** field showing:
  - "Company Share" for companies
  - "Cryptocurrency" for crypto
  - "Item Instrument" for items

### Added to Company Shares:
- **Share Price** (was missing before)
- Now shows both share price and company balance

### Configuration:
All new fields configurable via `guis.yml`:

```yaml
company_item:
  lore:
    - '&7Type: &b{instrument_type}'       # NEW
    - ''
    - '&eShare Price: &f${price}'         # NEW
    - '&eCompany Balance: &f${balance}'
    - '&eMarket Percentage: &f{market_percentage}%'
    - '&eCompany Type: &7{type}'

crypto_item:
  lore:
    - '&7Type: &6{instrument_type}'       # Now configurable

item_instrument:
  lore:
    - '&7Type: &b{instrument_type}'       # Now configurable
```

## Code Quality Improvements

### 1. Single Source of Truth
The HashMap is populated once when the GUI is created and serves as the single source for all instrument data.

### 2. Type Safety
Direct casting with explicit type checking in switch statement prevents ClassCastException.

### 3. Cleaner Code
- No more Material checks scattered throughout
- No string parsing logic
- Clear separation between button slots and instrument slots

### 4. Backward Compatibility
Old methods are deprecated but still present:
```java
@Deprecated
public String getStockSymbolFromSlot(int slot) {
    SlotInstrument si = slotInstrumentMap.get(slot);
    return si != null ? si.symbol : null;
}
```

## Migration Guide

### For Developers:
If you have custom code using the old methods:

**Old:**
```java
String symbol = marketGUI.getStockSymbolFromSlot(slot);
```

**New:**
```java
MarketGUI.SlotInstrument instrument = marketGUI.getInstrumentFromSlot(slot);
if (instrument != null) {
    String symbol = instrument.symbol;
    String type = instrument.type;
    Object data = instrument.data;
}
```

### For Server Administrators:
No changes needed. The refactoring is fully backward compatible with existing configurations.

## Testing

### What to Test:
1. ✅ All instrument types display correctly
2. ✅ Instrument type shows in lore
3. ✅ Company shares show price
4. ✅ Click handling works for all types
5. ✅ Filter cycling works
6. ✅ Buy/sell operations work
7. ✅ Navigation buttons work (portfolio, wallet, refresh, close)

### Known Issues:
None. All functionality preserved.

## Future Enhancements

With the HashMap infrastructure in place, we can now easily add:
1. **Sorting** - Add sort parameter to SlotInstrument
2. **Pagination** - Track page in HashMap
3. **Quick filters** - Filter HashMap before display
4. **Caching** - HashMap already serves as cache
5. **Analytics** - Track which instruments are clicked most

## Summary

This refactoring provides:
- ✅ Better performance (0 database queries per click)
- ✅ Cleaner code architecture
- ✅ Enhanced display information
- ✅ Full configurability
- ✅ Type safety
- ✅ Backward compatibility
- ✅ Foundation for future features
