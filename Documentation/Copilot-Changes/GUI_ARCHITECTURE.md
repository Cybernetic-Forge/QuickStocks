# GUI Configuration Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     QuickStocks Plugin                           │
│                                                                   │
│  ┌────────────────┐          ┌─────────────────┐                │
│  │ QuickStocks    │          │ GUIConfig       │                │
│  │ Plugin.java    │─────────>│ Manager.java    │                │
│  │                │ creates  │                 │                │
│  └────────────────┘          └────────┬────────┘                │
│         │                              │ loads                   │
│         │ passes to                    │                         │
│         ▼                              ▼                         │
│  ┌────────────────┐          ┌─────────────────┐                │
│  │ Commands       │          │   guis.yml      │                │
│  │ - Market       │          │                 │                │
│  │ - Company      │          │ Configuration   │                │
│  └────────┬───────┘          │ File            │                │
│           │                  └─────────────────┘                │
│           │ instantiates                                         │
│           ▼                                                      │
│  ┌────────────────┐                                              │
│  │ GUI Classes    │                                              │
│  │ - Market       │<─────┐                                      │
│  │ - Portfolio    │      │ uses config                          │
│  │ - Company      │      │                                      │
│  └────────┬───────┘      │                                      │
│           │              │                                      │
│           │ renders      │                                      │
│           ▼              │                                      │
│  ┌────────────────┐     │                                      │
│  │ GUI Listeners  │─────┘                                      │
│  │ - Market       │                                              │
│  │ - Portfolio    │                                              │
│  │ - Company      │                                              │
│  └────────────────┘                                              │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Component Flow

### 1. Initialization Phase

```
Server Start
    │
    ├─> QuickStocksPlugin.onEnable()
    │   │
    │   ├─> Initialize TranslationManager
    │   │
    │   ├─> Initialize GUIConfigManager ◄────┐
    │   │   │                                 │
    │   │   └─> Load guis.yml ───────────────┘
    │   │
    │   ├─> Initialize Services
    │   │
    │   └─> Register Commands (with GUIConfigManager)
    │       │
    │       ├─> MarketCommand
    │       ├─> CompanyCommand
    │       └─> ...
    │
    └─> Register Listeners (with GUIConfigManager)
        │
        ├─> MarketGUIListener
        ├─> PortfolioGUIListener
        └─> CompanySettingsGUIListener
```

### 2. Runtime GUI Creation

```
Player Command
    │
    ├─> /market (MarketCommand)
    │   │
    │   ├─> showMarketOverview(player)
    │   │   │
    │   │   └─> new MarketGUI(
    │   │           player,
    │   │           services...,
    │   │           guiConfigManager ◄───────────┐
    │   │       )                                 │
    │   │       │                                 │
    │   │       ├─> Read guis.yml ───────────────┘
    │   │       │   │
    │   │       │   ├─> market.title
    │   │       │   ├─> market.wallet
    │   │       │   ├─> market.company_item
    │   │       │   └─> ...
    │   │       │
    │   │       ├─> Create Inventory
    │   │       │   │
    │   │       │   └─> Apply ChatUT.hexComp()
    │   │       │
    │   │       └─> Setup Items
    │   │           │
    │   │           ├─> Portfolio Button
    │   │           ├─> Wallet Button
    │   │           ├─> Company Items
    │   │           └─> Navigation Buttons
    │   │
    │   └─> gui.open()
    │
    └─> Player sees GUI with configured elements
```

### 3. Configuration Loading Flow

```
GUIConfigManager
    │
    ├─> Constructor
    │   │
    │   └─> loadGUIConfig()
    │       │
    │       ├─> Check if guis.yml exists
    │       │   │
    │       │   ├─> No  ─> saveResource("guis.yml")
    │       │   └─> Yes ─> Continue
    │       │
    │       ├─> YamlConfiguration.loadConfiguration()
    │       │
    │       ├─> Load defaults from JAR
    │       │
    │       └─> Store in guisConfig
    │
    └─> Getter Methods
        │
        ├─> getTitle(path) ──────────> Component
        ├─> getItemName(path) ───────> Component
        ├─> getItemLore(path) ───────> List<Component>
        ├─> getItemMaterial(path) ───> Material
        ├─> getItemSlot(path) ───────> int
        └─> getString(path) ─────────> String
```

## Data Flow Diagram

### Reading Configuration

```
┌─────────────┐
│  guis.yml   │
│             │
│ market:     │
│   title: X  │
│   wallet:   │
│     name: Y │
│     slot: 8 │
└──────┬──────┘
       │ YamlConfiguration.load()
       ▼
┌─────────────────┐
│ GUIConfigManager│
│                 │
│ guisConfig:     │
│   ConfigSection │
└──────┬──────────┘
       │ getTitle("market")
       │ getItemName("market.wallet")
       │ getItemSlot("market.wallet", 8)
       ▼
┌─────────────┐
│  MarketGUI  │
│             │
│ Creates:    │
│ - Inventory │
│ - Items     │
└─────────────┘
```

### Processing Placeholders

```
Configuration String
"&6Balance: &a${balance}"
       │
       ├─> Replace placeholders
       │   "{balance}" → "123.45"
       │
       ├─> Result: "&6Balance: &a$123.45"
       │
       ├─> ChatUT.hexComp()
       │   │
       │   ├─> Parse hex colors (&#RRGGBB)
       │   ├─> Parse legacy colors (&a, &6)
       │   ├─> Convert to MiniMessage
       │   └─> Build Adventure Component
       │
       ├─> Component (rich text)
       │
       └─> ChatUT.serialize()
           │
           └─> Legacy string for ItemMeta
```

## Class Relationships

### GUI Hierarchy

```
                    InventoryHolder
                          △
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    MarketGUI      PortfolioGUI    CompanySettingsGUI
         │                │                │
         │                │                │
    Uses Config      Uses Config      Uses Config
         │                │                │
         └────────────────┴────────────────┘
                          │
                   GUIConfigManager
                          │
                      guis.yml
```

### Service Injection

```
QuickStocksPlugin
    │
    ├─> Creates Services
    │   ├─> QueryService
    │   ├─> TradingService
    │   ├─> CompanyService
    │   └─> ...
    │
    ├─> Creates Managers
    │   ├─> TranslationManager
    │   └─> GUIConfigManager ◄── Important!
    │
    └─> Injects into Commands
        │
        ├─> MarketCommand(services..., guiConfigManager)
        │   │
        │   └─> Creates MarketGUI(player, services, guiConfigManager)
        │
        └─> CompanyCommand(services..., guiConfigManager)
            │
            └─> Creates CompanySettingsGUI(player, service, company, guiConfigManager)
```

## Configuration Structure

### Hierarchical Organization

```
guis.yml
├─ company_settings:
│  ├─ title: String
│  ├─ size: Integer
│  ├─ balance_display:
│  │  ├─ name: String
│  │  ├─ material: Material
│  │  ├─ slot: Integer
│  │  └─ lore: List<String>
│  ├─ company_info:
│  │  ├─ name: String
│  │  ├─ material: Material
│  │  ├─ slot: Integer
│  │  └─ lore: List<String>
│  └─ ... (more items)
│
├─ portfolio:
│  ├─ title: String
│  ├─ size: Integer
│  ├─ wallet:
│  │  └─ ...
│  ├─ holding_item:
│  │  ├─ profit_materials:
│  │  │  ├─ high: Material
│  │  │  └─ medium: Material
│  │  ├─ loss_materials:
│  │  │  ├─ high: Material
│  │  │  └─ medium: Material
│  │  ├─ profit_color: String
│  │  ├─ loss_color: String
│  │  └─ lore: List<String>
│  └─ ... (more items)
│
└─ market:
   ├─ title: String
   ├─ size: Integer
   ├─ company_item:
   │  ├─ name: String
   │  ├─ materials:
   │  │  ├─ tech: Material
   │  │  ├─ finance: Material
   │  │  ├─ retail: Material
   │  │  └─ default: Material
   │  └─ lore: List<String>
   └─ ... (more items)
```

## Adventure API Integration

### Color Processing Pipeline

```
Input String
"&#FF5555Hello &aWorld"
       │
       ▼
ChatUT.hexComp()
       │
       ├─> parseHexColorCodes()
       │   "&#FF5555Hello &aWorld"
       │   → "<color:#FF5555>Hello &aWorld"
       │
       ├─> parseNativeColorCodes()
       │   "<color:#FF5555>Hello &aWorld"
       │   → "<color:#FF5555>Hello <color:green>World"
       │
       ├─> Add MiniMessage wrapper
       │   "<!i><color:#FF5555>Hello <color:green>World"
       │
       └─> MiniMessage.deserialize()
           │
           └─> Adventure Component
```

### Component to String Conversion

```
Adventure Component
       │
       ├─> ChatUT.serialize()
       │   │
       │   └─> LegacyComponentSerializer
       │       │
       │       └─> "§cHello §aWorld"
       │           (for Bukkit ItemMeta)
       │
       └─> ChatUT.extractText()
           │
           └─> PlainTextComponentSerializer
               │
               └─> "Hello World"
                   (for string extraction)
```

## Extension Points

### Adding New GUI Elements

```
1. Add to guis.yml
   ─────────────────
   new_gui:
     new_item:
       name: "..."
       material: STONE
       slot: 10
       lore: [...]

2. Add to GUI Class
   ─────────────────
   private void addNewItem() {
       String path = "new_gui.new_item";
       Material mat = guiConfig.getItemMaterial(path, Material.STONE);
       int slot = guiConfig.getItemSlot(path, 10);
       // ... create item
   }

3. Call in setupGUI()
   ─────────────────
   addNewItem();
```

### Supporting New Placeholders

```
1. Document placeholder
   ─────────────────
   // In guis.yml comment:
   # {new_value} - Description

2. Replace in code
   ─────────────────
   processedLine = line.replace("{new_value}", actualValue);
```

## Performance Considerations

### Caching Strategy

```
GUIConfigManager
    │
    ├─> Loaded once on startup
    │   └─> Stored in memory
    │
    └─> GUI Creation
        │
        ├─> Fast lookups (HashMap)
        ├─> No file I/O per GUI
        └─> Reusable configuration
```

### Memory Footprint

```
Per GUI Instance:
├─ Inventory (54 slots)      ~1-2 KB
├─ ItemStacks with meta       ~5-10 KB
└─ GUIConfigManager ref       ~0 KB (shared)
                              ─────────
                              ~6-12 KB per open GUI
```

## Error Handling

### Configuration Errors

```
Invalid Material
    │
    ├─> Try Material.valueOf()
    │   │
    │   ├─> Success ──> Use material
    │   │
    │   └─> Exception ──> Log warning
    │                     Use fallback material
    │
Missing Config Key
    │
    ├─> getItemMaterial(path, DEFAULT)
    │   │
    │   └─> Return DEFAULT if not found
    │
Invalid YAML Syntax
    │
    ├─> YamlConfiguration.load()
    │   │
    │   └─> Exception ──> Log error
    │                     Use empty config
```

## Best Practices

### For Developers

```
✓ Always provide default values
✓ Use try-catch around GUI setup
✓ Log warnings for invalid configs
✓ Test with minimal configuration
✓ Document all placeholders
```

### For Server Admins

```
✓ Backup guis.yml before editing
✓ Test changes on test server first
✓ Validate YAML syntax (use online validator)
✓ Check material names for your MC version
✓ Use consistent color scheme
```

## Migration Path

### From Hardcoded to Config

```
Before:
    lore.add(ChatColor.YELLOW + "Balance: " + balance);

After:
    String line = config.getString("path.lore.0", "Default");
    line = line.replace("{balance}", balance);
    lore.add(ChatUT.serialize(ChatUT.hexComp(line)));

Config:
    path:
      lore:
        - "&eBalance: {balance}"
```

## Summary

The GUI configuration architecture provides:

1. **Separation of Concerns**: Logic vs Presentation
2. **Flexibility**: Easy customization without code changes
3. **Maintainability**: Centralized configuration
4. **Extensibility**: Simple to add new elements
5. **Modern API**: Adventure Components with rich formatting
6. **Performance**: Efficient caching and lookups
7. **Robustness**: Comprehensive error handling

This architecture follows industry best practices and provides a solid foundation for future enhancements.
