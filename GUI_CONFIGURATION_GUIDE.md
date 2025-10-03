# GUI Configuration System Guide

## Overview

All GUI components in QuickStocks have been externalized to the `guis.yml` configuration file. This allows server administrators to fully customize all GUI elements including titles, item names, lore, materials, slots, and colors without modifying the code.

## Changes Made

### 1. Created GUIConfigManager
- **Location**: `src/main/java/net/cyberneticforge/quickstocks/utils/GUIConfigManager.java`
- **Purpose**: Manages loading and retrieval of GUI configurations from `guis.yml`
- **Pattern**: Follows the same pattern as `TranslationManager` for consistency

### 2. Created guis.yml Configuration File
- **Location**: `src/main/resources/guis.yml`
- **Contains**: All GUI element configurations for:
  - Company Settings GUI
  - Portfolio GUI
  - Market GUI

### 3. Migrated to Adventure API (ChatUT.hexComp)
All GUIs now use the Adventure API through `ChatUT.hexComp()` instead of the deprecated `ChatColor` class. This provides:
- Hex color support (`&#RRGGBB`)
- Legacy color code support (`&a`, `&c`, etc.)
- MiniMessage format support

## Configuration Structure

### Color Codes
- **Legacy codes**: Use `&` prefix (e.g., `&a` for green, `&c` for red)
- **Hex colors**: Use `&#RRGGBB` format (e.g., `&#FF5555` for red)
- **Named colors**: Through Adventure API (e.g., `<color:red>`)

### GUI Section Format
Each GUI has its own section with the following structure:

```yaml
gui_name:
  title: "GUI Title with {placeholders}"
  size: 54  # Number of slots (must be multiple of 9)
  
  item_name:
    name: "Item Display Name"
    material: MATERIAL_NAME
    slot: 0
    lore:
      - "Lore line 1 with {placeholders}"
      - "Lore line 2"
```

### Placeholders
The configuration system supports dynamic placeholders that are replaced at runtime:
- `{company_name}` - Company name
- `{player_name}` - Player name
- `{balance}` - Formatted balance
- `{qty}` - Quantity
- `{symbol}` - Stock/company symbol
- And many more (see guis.yml for complete list)

## Customization Examples

### Change Company Settings GUI Title
```yaml
company_settings:
  title: "&6&lCompany Panel: &f{company_name}"
```

### Change Portfolio Item Colors
```yaml
portfolio:
  holding_item:
    profit_color: "&#00FF00"  # Bright green for profits
    loss_color: "&#FF0000"    # Bright red for losses
```

### Customize Market GUI Materials
```yaml
market:
  company_item:
    materials:
      tech: REDSTONE
      finance: EMERALD
      retail: CHEST
      default: PAPER
```

### Modify Button Slots
```yaml
company_settings:
  view_employees:
    slot: 19  # Change from default position
```

## Files Modified

### Core GUI Classes
1. **CompanySettingsGUI.java**
   - Now loads all text, materials, and colors from config
   - Uses `ChatUT.hexComp()` for Adventure API support
   - Accepts `GUIConfigManager` in constructor

2. **PortfolioGUI.java**
   - Configurable holdings display with profit/loss colors
   - Dynamic material selection based on performance
   - All buttons and messages from config

3. **MarketGUI.java**
   - Company item materials configurable by type
   - All text and colors from config
   - Dynamic button placement

### Listener Classes
1. **MarketGUIListener.java**
   - Updated to pass `GUIConfigManager` to GUI constructors
   - Uses Adventure API for messages

2. **PortfolioGUIListener.java**
   - Updated to pass `GUIConfigManager` to GUI constructors
   - Uses Adventure API for messages

3. **CompanySettingsGUIListener.java**
   - No changes needed (stateless listener)

### Command Classes
1. **MarketCommand.java**
   - Accepts and passes `GUIConfigManager` to MarketGUI
   - Updated constructor signature

2. **CompanyCommand.java**
   - Accepts and passes `GUIConfigManager` to CompanySettingsGUI
   - Updated constructor signature

### Plugin Initialization
1. **QuickStocksPlugin.java**
   - Initializes `GUIConfigManager` on plugin enable
   - Passes `GUIConfigManager` to all commands and listeners

## Benefits

### For Server Administrators
- **Full customization**: Change any GUI element without code changes
- **Multiple languages**: Easy to translate by editing one file
- **Branding**: Match your server's theme with custom colors
- **Flexibility**: Rearrange GUI items to your preference

### For Developers
- **Maintainability**: Changes don't require recompiling
- **Consistency**: All GUIs follow the same configuration pattern
- **Extensibility**: Easy to add new GUI elements

### For Players
- **Better UX**: Server-specific customizations
- **Accessibility**: Customizable colors for better readability
- **Hex color support**: More vibrant and modern appearance

## Migration Notes

### Breaking Changes
- **Constructor signatures**: All GUI classes now require `GUIConfigManager` parameter
- **Command constructors**: `MarketCommand` and `CompanyCommand` require `GUIConfigManager`
- **Listener constructors**: `MarketGUIListener` and `PortfolioGUIListener` require `GUIConfigManager`

### Backward Compatibility
- Default values in code ensure GUIs work even if config is missing
- Fallback to sensible defaults if invalid materials specified
- Graceful error handling for missing config keys

## Testing

Due to Maven build issues with PaperMC repository connectivity, automated testing was not possible. However, the implementation follows these best practices:

1. **Defensive programming**: All config reads have fallback defaults
2. **Error handling**: Try-catch blocks around GUI setup
3. **Logging**: Warning messages for invalid configurations
4. **Type safety**: Material validation with fallback to defaults

## Future Enhancements

Potential improvements for future versions:
- Per-player GUI customization
- Dynamic GUI layouts based on permissions
- Animated GUI elements
- Custom item models support
- GUI templates for common patterns

## Support

If you encounter issues with GUI configuration:
1. Check server logs for warning messages
2. Verify YAML syntax in `guis.yml`
3. Ensure material names are valid for your Minecraft version
4. Test with default configuration first
5. Check that `guis.yml` exists in the plugin data folder

## Example Configurations

### Minimal Configuration
```yaml
company_settings:
  title: "Company: {company_name}"
  size: 54
```

### Full Customization
```yaml
company_settings:
  title: "&#FFD700&l⚙ Company Settings: &#FFFFFF{company_name}"
  size: 54
  balance_display:
    name: "&#FFD700&l💰 Balance"
    material: GOLD_INGOT
    slot: 0
    lore:
      - "&#00FF00${balance}"
      - ""
      - "&#808080Manage your funds below"
```

## Color Reference

### Legacy Colors
- `&0` - Black
- `&1` - Dark Blue
- `&2` - Dark Green
- `&3` - Dark Aqua
- `&4` - Dark Red
- `&5` - Dark Purple
- `&6` - Gold
- `&7` - Gray
- `&8` - Dark Gray
- `&9` - Blue
- `&a` - Green
- `&b` - Aqua
- `&c` - Red
- `&d` - Light Purple
- `&e` - Yellow
- `&f` - White

### Format Codes
- `&l` - Bold
- `&m` - Strikethrough
- `&n` - Underline
- `&o` - Italic
- `&r` - Reset formatting

### Hex Colors
Use `&#RRGGBB` format for 16 million color possibilities:
- `&#FF0000` - Pure Red
- `&#00FF00` - Pure Green
- `&#0000FF` - Pure Blue
- `&#FFD700` - Gold
- `&#FF1493` - Deep Pink
