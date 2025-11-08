# Configuration Auto-Defaults Implementation

## Overview
This document describes the implementation of automatic default value generation for all configuration files in QuickStocks.

## Issue
**Issue Title:** addIfAbsent for all configs  
**Description:** All config classes should use the YamlParser's `addMissing` method to auto-generate non-existent entries with default values.

## Solution
Implemented `addMissingDefaults()` method in all configuration classes that use YamlParser, following the pattern established by TranslationService.

## Implementation Details

### Pattern Applied
Each configuration class now follows this pattern:

```java
public ConfigClass() {
    config = YamlParser.loadOrExtract(Plugin.getInstance(), "config-file.yml");
    addMissingDefaults();  // NEW: Add missing entries before loading
    loadValues();
}

private void addMissingDefaults() {
    // Add missing config entries with default values
    config.addMissing("config.key", defaultValue);
    // ... more entries ...
    config.saveChanges();  // Save changes to disk
}
```

### Files Modified

#### 1. MarketCfg.java
**Config file:** `market.yml`  
**Entries added:** 13 configuration entries
- Market settings (updateInterval, startOpen, defaultStocks)
- Price threshold settings (enabled, maxChangePercent, priceMultiplierThreshold, dampeningFactor, minVolumeThreshold, volumeSensitivity)
- Analytics settings (lambda, change window, volatility window, correlation window)

#### 2. CompanyCfg.java
**Config file:** `companies.yml`  
**Entries added:** 24 configuration entries
- Basic settings (enabled, creationCost)
- Default types (PRIVATE, PUBLIC, DAO)
- Default job titles (CEO, CFO, EMPLOYEE)
- Permissions by title for CEO, CFO, and EMPLOYEE (5 permissions each)
- Salary settings (paymentCycles, defaultJobSalary, offlinePayment)
- ChestShop settings (enabled, companyMinBalance)

#### 3. TradingCfg.java
**Config file:** `market.yml` (trading section)  
**Entries added:** 12 configuration entries
- Fee settings (mode, percent, flat)
- Limits settings (maxOrderQty, maxNotionalPerMinute, perPlayerCooldownMs)
- Circuit breakers settings (enable, levels, haltMinutes)
- Orders settings (allowMarket, allowLimit, allowStop)
- Slippage settings (mode, k)

#### 4. GuiConfig.java
**Config file:** `guis.yml`  
**Implementation:** Recursive copy of all missing keys
- Uses `copyMissingKeys()` method to recursively traverse the default config
- Copies all missing entries from the bundled default guis.yml
- Handles nested configuration sections automatically

### Special Case: GuiConfig
GuiConfig uses a different approach because it's a generic configuration reader:

```java
private void addMissingDefaults() {
    FileConfiguration def = YamlParser.getDefaultConfig("guis.yml");
    if (def != null) {
        copyMissingKeys(def, "", config);
        config.saveChanges();
    }
}

private void copyMissingKeys(FileConfiguration source, String prefix, YamlParser target) {
    for (String key : source.getKeys(false)) {
        String fullPath = prefix.isEmpty() ? key : prefix + "." + key;
        if (source.isConfigurationSection(fullPath)) {
            copyMissingKeys(source, fullPath, target);
        } else {
            target.addMissing(fullPath, source.get(fullPath));
        }
    }
}
```

This approach:
1. Loads the default config from resources
2. Recursively iterates through all keys
3. Adds any missing keys to the user's config file
4. Saves changes

## Benefits

### 1. Automatic Config Updates
When new config entries are added to the plugin, users will automatically get them added to their config files without manual intervention.

### 2. No Lost Customizations
The `addMissing()` method only adds entries that don't exist. Existing user customizations are preserved.

### 3. Reduced Support Burden
Users won't need to manually add new config entries when updating the plugin.

### 4. Consistency
All config classes now follow the same pattern, making the codebase more maintainable.

## Testing

### Manual Testing Steps
1. Delete a config file (e.g., `market.yml`)
2. Start the plugin
3. Verify the config file is created with all default values
4. Modify some values in the config
5. Delete a few keys from the config file
6. Restart the plugin
7. Verify the deleted keys are re-added with defaults
8. Verify modified keys retain their custom values

### Expected Behavior
- Missing config files are created with all defaults
- Partial config files are completed with missing entries
- Existing values are not overwritten
- Changes are saved to disk immediately

## Comparison with Existing Implementation

### TranslationService (Original Pattern)
```java
public void setup() {
    FileConfiguration def = YamlParser.getDefaultConfig("Translations.yml");
    for (Translation translation : Translation.values()) {
        config.addMissing(translation.getPath(), def.getString(translation.getPath()));
    }
    config.saveChanges();
}
```

### New Config Classes (Applied Pattern)
```java
private void addMissingDefaults() {
    config.addMissing("config.key", defaultValue);
    // ... more entries ...
    config.saveChanges();
}
```

Both patterns achieve the same goal but use slightly different approaches:
- TranslationService iterates through an enum of keys
- Other configs explicitly list all keys (better for IDE autocomplete and type safety)
- GuiConfig uses recursive copy (better for large, nested configs)

## Configuration Files Not Modified

### config.yml
**Reason:** Uses `ConfigLoader` instead of `YamlParser`. This is for database configuration only and follows a different loading pattern. It already has fallback defaults in the ConfigLoader class.

### AnalyticsConfig.java
**Reason:** This is a simple POJO class without YamlParser integration. It's used as a data holder, not a config loader.

## Future Enhancements

### Potential Improvements
1. Add validation for config values when loading
2. Add migration support for renamed/removed config keys
3. Add config version tracking
4. Add automatic backup before modifying configs
5. Add config validation messages in console

## Related Files
- `YamlParser.java` - Contains the `addMissing()` method
- `TranslationService.java` - Original implementation of this pattern
- All resource YAML files in `src/main/resources/`

## Commit Information
- **Commit:** Add addMissing calls for all config classes
- **Files Changed:** 4
- **Lines Added:** 129
- **Lines Removed:** 0

## Author Notes
This implementation ensures that QuickStocks configs are always complete and up-to-date, reducing user friction when updating the plugin and making config management more robust.
