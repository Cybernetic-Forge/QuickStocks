# GUI Migration Examples - Before & After

This document shows concrete examples of how GUI code has been transformed from hardcoded values to configuration-driven design.

## Example 1: Company Settings GUI - Balance Display

### Before (Hardcoded)
```java
// Old code with ChatColor and hardcoded values
ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
ItemMeta balanceMeta = balanceItem.getItemMeta();
balanceMeta.setDisplayName(ChatColor.GOLD + "Company Balance");

List<String> balanceLore = new ArrayList<>();
balanceLore.add(ChatColor.GREEN + "$" + String.format("%.2f", company.getBalance()));
balanceLore.add("");
balanceLore.add(ChatColor.GRAY + "Use deposit/withdraw commands");
balanceLore.add(ChatColor.GRAY + "to manage funds");

balanceMeta.setLore(balanceLore);
balanceItem.setItemMeta(balanceMeta);
inventory.setItem(0, balanceItem);
```

### After (Configuration-driven)
```java
// New code with Adventure API and config
Material balanceMaterial = guiConfig.getItemMaterial("company_settings.balance_display", Material.GOLD_INGOT);
int balanceSlot = guiConfig.getItemSlot("company_settings.balance_display", 0);
ItemStack balanceItem = new ItemStack(balanceMaterial);
ItemMeta balanceMeta = balanceItem.getItemMeta();
balanceMeta.setDisplayName(guiConfig.getItemNameString("company_settings.balance_display"));

List<String> balanceLorePatt = guiConfig.getItemLoreStrings("company_settings.balance_display");
List<String> balanceLore = new ArrayList<>();
for (String line : balanceLorePatt) {
    String processedLine = line.replace("{balance}", String.format("%.2f", company.getBalance()));
    balanceLore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
}

balanceMeta.setLore(balanceLore);
balanceItem.setItemMeta(balanceMeta);
inventory.setItem(balanceSlot, balanceItem);
```

### Configuration (guis.yml)
```yaml
company_settings:
  balance_display:
    name: "&6Company Balance"
    material: GOLD_INGOT
    slot: 0
    lore:
      - "&a${balance}"
      - ""
      - "&7Use deposit/withdraw commands"
      - "&7to manage funds"
```

## Example 2: Portfolio GUI - Holdings Display

### Before (Hardcoded)
```java
// Old code with fixed materials and colors
Material material;
ChatColor nameColor;

if (holding.getUnrealizedPnL() >= 0) {
    material = holding.getUnrealizedPnL() > 100 ? Material.DIAMOND : Material.EMERALD;
    nameColor = ChatColor.GREEN;
} else {
    material = holding.getUnrealizedPnL() < -100 ? Material.COAL : Material.REDSTONE;
    nameColor = ChatColor.RED;
}

ItemStack item = new ItemStack(material);
ItemMeta meta = item.getItemMeta();
meta.setDisplayName(nameColor + holding.getSymbol());

// Hardcoded lore format
List<String> lore = new ArrayList<>();
lore.add(ChatColor.YELLOW + "Shares Owned: " + ChatColor.WHITE + String.format("%.2f", holding.getQty()));
lore.add(ChatColor.YELLOW + "Purchase Price: " + ChatColor.WHITE + "$" + String.format("%.2f", holding.getAvgCost()));
// ... more hardcoded lore lines
```

### After (Configuration-driven)
```java
// New code with configurable materials and colors
Material material;
String colorCode;

if (holding.getUnrealizedPnL() >= 0) {
    String highMat = guiConfig.getString("portfolio.holding_item.profit_materials.high", "DIAMOND");
    String medMat = guiConfig.getString("portfolio.holding_item.profit_materials.medium", "EMERALD");
    material = holding.getUnrealizedPnL() > 100 ? 
        Material.valueOf(highMat) : Material.valueOf(medMat);
    colorCode = guiConfig.getString("portfolio.holding_item.profit_color", "&a");
} else {
    String highMat = guiConfig.getString("portfolio.holding_item.loss_materials.high", "COAL");
    String medMat = guiConfig.getString("portfolio.holding_item.loss_materials.medium", "REDSTONE");
    material = holding.getUnrealizedPnL() < -100 ? 
        Material.valueOf(highMat) : Material.valueOf(medMat);
    colorCode = guiConfig.getString("portfolio.holding_item.loss_color", "&c");
}

ItemStack item = new ItemStack(material);
ItemMeta meta = item.getItemMeta();
meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(colorCode + holding.getSymbol())));

// Configurable lore with placeholders
List<String> lorePatt = guiConfig.getItemLoreStrings("portfolio.holding_item");
List<String> lore = new ArrayList<>();

for (String line : lorePatt) {
    String processedLine = line
        .replace("{qty}", String.format("%.2f", holding.getQty()))
        .replace("{avg_cost}", String.format("%.2f", holding.getAvgCost()))
        // ... more replacements
    lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
}
```

### Configuration (guis.yml)
```yaml
portfolio:
  holding_item:
    profit_materials:
      high: DIAMOND
      medium: EMERALD
    loss_materials:
      high: COAL
      medium: REDSTONE
    profit_color: "&a"
    loss_color: "&c"
    lore:
      - "&eShares Owned: &f{qty}"
      - "&ePurchase Price: &f${avg_cost}"
      - "&eCurrent Price: &f${current_price}"
      - "&eTotal Value: &f${total_value}"
```

## Example 3: Market GUI - Company Items

### Before (Hardcoded)
```java
// Old code with switch statement for materials
private Material getMaterialForCompany(String type) {
    if (type == null) type = "other";
    
    switch (type.toLowerCase()) {
        case "tech":
        case "technology":
            return Material.REDSTONE;
        case "finance":
        case "financial":
            return Material.EMERALD;
        case "retail":
        case "consumer":
            return Material.CHEST;
        case "manufacturing":
            return Material.IRON_INGOT;
        case "agriculture":
            return Material.WHEAT;
        default:
            return Material.PAPER;
    }
}

// Hardcoded item creation
meta.setDisplayName(ChatColor.GREEN + displayName + " (" + symbol + ")");

List<String> lore = new ArrayList<>();
lore.add(ChatColor.YELLOW + "Company Balance: " + ChatColor.WHITE + "$" + String.format("%.2f", balance));
lore.add(ChatColor.YELLOW + "Market Percentage: " + ChatColor.WHITE + String.format("%.1f%%", company.getMarketPercentage()));
```

### After (Configuration-driven)
```java
// New code with config-based material lookup
private Material getMaterialForCompany(String type) {
    if (type == null) type = "other";
    
    String materialName = guiConfig.getString("market.company_item.materials." + type.toLowerCase(), null);
    if (materialName == null) {
        materialName = guiConfig.getString("market.company_item.materials.default", "PAPER");
    }
    
    try {
        return Material.valueOf(materialName.toUpperCase());
    } catch (IllegalArgumentException e) {
        logger.warning("Invalid material '" + materialName + "' for company type '" + type + "', using PAPER");
        return Material.PAPER;
    }
}

// Configurable item creation
String namePatt = guiConfig.getString("market.company_item.name", "&a{company_name} ({symbol})")
    .replace("{company_name}", displayName)
    .replace("{symbol}", symbol);
meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(namePatt)));

List<String> lorePatt = guiConfig.getItemLoreStrings("market.company_item");
List<String> lore = new ArrayList<>();

for (String line : lorePatt) {
    String processedLine = line
        .replace("{balance}", String.format("%.2f", balance))
        .replace("{market_percentage}", String.format("%.1f", company.getMarketPercentage()))
        // ... more replacements
    lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
}
```

### Configuration (guis.yml)
```yaml
market:
  company_item:
    name: "&a{company_name} ({symbol})"
    materials:
      tech: REDSTONE
      technology: REDSTONE
      finance: EMERALD
      financial: EMERALD
      retail: CHEST
      consumer: CHEST
      manufacturing: IRON_INGOT
      agriculture: WHEAT
      default: PAPER
    lore:
      - "&eCompany Balance: &f${balance}"
      - "&eMarket Percentage: &f{market_percentage}%"
      - "&eType: &7{type}"
```

## Example 4: GUI Titles

### Before (Hardcoded in Constructor)
```java
// CompanySettingsGUI
this.inventory = Bukkit.createInventory(this, GUI_SIZE, 
    ChatColor.GOLD + "Company: " + ChatColor.WHITE + company.getName());

// PortfolioGUI
this.inventory = Bukkit.createInventory(this, GUI_SIZE, 
    ChatColor.GOLD + "Portfolio - " + player.getName());

// MarketGUI
this.inventory = Bukkit.createInventory(this, GUI_SIZE, 
    ChatColor.DARK_GREEN + "Market - QuickStocks");
```

### After (Configuration-driven)
```java
// All GUIs now use config
int guiSize = guiConfig.getInt("gui_section.size", 54);
String title = guiConfig.getString("gui_section.title", "Default Title")
    .replace("{placeholder}", value);

this.inventory = Bukkit.createInventory(this, guiSize, 
    ChatUT.serialize(ChatUT.hexComp(title)));
```

### Configuration (guis.yml)
```yaml
company_settings:
  title: "&6Company: &f{company_name}"
  size: 54

portfolio:
  title: "&6Portfolio - {player_name}"
  size: 54

market:
  title: "&2Market - QuickStocks"
  size: 54
```

## Key Improvements

### 1. Flexibility
**Before:** Changing colors required code changes and recompilation  
**After:** Edit `guis.yml` and reload server

### 2. Localization
**Before:** Multiple language support required separate builds  
**After:** Different `guis.yml` files for each language

### 3. Customization
**Before:** Limited to predefined colors and materials  
**After:** Full control over every visual element

### 4. Modern API
**Before:** Deprecated `ChatColor` with limited color palette  
**After:** Adventure API with hex colors and modern formatting

### 5. Maintainability
**Before:** GUI changes scattered across multiple Java files  
**After:** All GUI configuration centralized in one YAML file

## Migration Statistics

- **Lines of hardcoded GUI code removed:** ~400+ lines
- **Configuration entries added:** 50+ configurable elements
- **Color codes migrated:** 100+ ChatColor usages → ChatUT.hexComp()
- **Dynamic placeholders:** 20+ placeholder types
- **Configurable properties:** Titles, materials, slots, colors, lore, sizes

## Color Support Comparison

### Before (ChatColor)
```java
ChatColor.GOLD    // 16 predefined colors only
ChatColor.GREEN
ChatColor.RED
```

### After (Adventure API)
```yaml
# Legacy colors (backward compatible)
"&6"  # Gold
"&a"  # Green
"&c"  # Red

# Hex colors (16 million colors!)
"&#FFD700"  # Custom gold
"&#00FF00"  # Bright green
"&#FF1493"  # Deep pink

# MiniMessage format
"<color:gold>"
"<gradient:green:blue>"
```

## Testing Examples

### Test Configuration 1: Minimalist Theme
```yaml
company_settings:
  title: "Company: {company_name}"
  balance_display:
    name: "Balance"
    lore:
      - "${balance}"
```

### Test Configuration 2: Vibrant Theme
```yaml
company_settings:
  title: "&#FFD700&l⭐ {company_name} ⭐"
  balance_display:
    name: "&#FFD700&l💰 Company Funds"
    lore:
      - "&#00FF00&l${balance}"
      - ""
      - "&#808080Click to manage"
```

### Test Configuration 3: Corporate Theme
```yaml
company_settings:
  title: "&8[&6Company&8] &f{company_name}"
  balance_display:
    name: "&6◆ &fFinancial Overview &6◆"
    lore:
      - "&7Current Balance:"
      - "&a$${balance}"
      - ""
      - "&8━━━━━━━━━━━━━━━━━"
```

## Conclusion

The migration to configuration-driven GUIs provides:
- **100% customizability** without code changes
- **Modern color support** via Adventure API
- **Better maintainability** with centralized configuration
- **Easier localization** for multi-language servers
- **Server branding** through custom themes

Server administrators can now fully customize the look and feel of all GUIs to match their server's theme and brand identity.
