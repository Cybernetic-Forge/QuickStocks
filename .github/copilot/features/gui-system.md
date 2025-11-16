# GUI System - Copilot Instructions

## Overview
The GUI system provides interactive inventory-based interfaces for browsing the market, managing companies, and configuring settings. All GUIs are configurable via `guis.yml`.

## Architecture

### Key Components
```
gui/
├── MarketGUI.java              # Market browser interface
├── CompanySettingsGUI.java     # Company management interface
├── PlotEditGUI.java            # Plot management interface
└── listeners/
    └── CompanySettingsGUIListener.java  # Event handler for company GUI
```

### Configuration
**Location:** `src/main/resources/guis.yml`

All GUI layouts, item types, names, and lore are configurable without code changes.

## Key Features

### 1. Market GUI

**Command:** `/market` or `/market browse`

**Purpose:**
Browse and trade instruments (stocks, crypto, company shares).

**Layout:**
```
┌─────────────────────────────────────┐
│  🔍 Filter Options                  │
│  ├─ All Instruments                 │
│  ├─ Stocks (ITEM/EQUITY)           │
│  ├─ Cryptocurrencies                │
│  └─ Company Shares                  │
│                                     │
│  📊 Instrument List                 │
│  ├─ [SYMBOL] Name - $XX.XX ↑X.X%  │
│  ├─ [SYMBOL] Name - $XX.XX ↓X.X%  │
│  └─ ...                             │
│                                     │
│  ⬅️ Previous   🔄 Refresh   Next ➡️  │
└─────────────────────────────────────┘
```

**Features:**
- Paginated instrument list
- Real-time price updates (on refresh)
- Color-coded price changes (green up, red down)
- Click instrument to view details
- Click to buy/sell
- Filter by instrument type
- Sort by price/volume/change

**Configuration:** `guis.yml`
```yaml
market:
  title: "§6§lStock Market"
  size: 54  # 6 rows
  
  items:
    instrument:
      material: PAPER
      nameFormat: "§e%symbol% §7- §f%name%"
      loreFormat:
        - "§7Price: §a$%price%"
        - "§7Change (24h): %change%"
        - "§7Volume: %volume%"
        - ""
        - "§eLeft-click to buy"
        - "§cRight-click to sell"
    
    filter:
      material: HOPPER
      slot: 0
      name: "§6Filter: %filter%"
    
    refresh:
      material: COMPASS
      slot: 49
      name: "§aRefresh Data"
    
    previousPage:
      material: ARROW
      slot: 45
      name: "§7Previous Page"
    
    nextPage:
      material: ARROW
      slot: 53
      name: "§7Next Page"
```

**Code Pattern:**
```java
public class MarketGUI {
    private Player player;
    private Inventory inventory;
    private int currentPage;
    private InstrumentType filter;
    
    public MarketGUI(Player player) {
        this.player = player;
        this.currentPage = 0;
        this.filter = InstrumentType.ALL;
        
        // Load configuration from guis.yml
        this.inventory = createInventory();
        populateItems();
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public void refresh() {
        populateItems();
        player.updateInventory();
    }
}
```

### 2. Company Settings GUI

**Command:** `/company settings [company]`

**Purpose:**
Manage company settings, employees, finances, and operations.

**Layout:**
```
┌─────────────────────────────────────┐
│  💰 Balance   👑 Info   📋 Your Job │
│                                     │
│  👥 View        📚 View    💵 Deposit│
│  Employees      Jobs       Funds    │
│                                     │
│  💸 Withdraw   🎯 Assign   ✉️ Invite │
│  Funds         Job        Player    │
│                                     │
│              📝 Create Job          │
│                                     │
│          🔄 Refresh   ❌ Close      │
└─────────────────────────────────────┘
```

**Features:**
- Permission-aware button display
- Real-time company data
- Click actions (view/execute commands)
- Visual permission indicators
- Hover tooltips with command hints
- Refresh without closing GUI

**Configuration:** `guis.yml`
```yaml
companySettings:
  title: "§6§lCompany Settings: §e%company%"
  size: 54
  
  items:
    balance:
      material: GOLD_INGOT
      slot: 0
      nameFormat: "§6💰 Balance: §a$%balance%"
      loreFormat:
        - "§7Company funds available"
        - "§7for operations"
    
    companyInfo:
      material: BOOK
      slot: 4
      nameFormat: "§e%name%"
      loreFormat:
        - "§7Type: §f%type%"
        - "§7Owner: §f%owner%"
        - "§7Created: §f%date%"
    
    yourJob:
      material: NAME_TAG
      slot: 8
      nameFormat: "§aYour Job: §f%job%"
      loreFormat:
        - "§7Permissions:"
        - "%permissions%"
    
    viewEmployees:
      material: PLAYER_HEAD
      slot: 19
      name: "§a👥 View Employees"
      requirePermission: false
      action: "command"
      command: "/company employees %company%"
    
    # ... more buttons ...
```

**Code Pattern:**
```java
public class CompanySettingsGUI {
    public void createButton(int slot, Material material, String name, 
                           List<String> lore, boolean requirePermission) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Check permission if required
        if (requirePermission && !hasPermission(player, permission)) {
            meta.setDisplayName("§c" + name + " §7(No Permission)");
            material = Material.BARRIER;
        } else {
            meta.setDisplayName(name);
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }
}
```

**Event Handling:**
```java
public class CompanySettingsGUIListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if this is our GUI
        if (!event.getView().getTitle().contains("Company Settings")) return;
        
        event.setCancelled(true);  // Prevent item movement
        
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        // Handle button click
        int slot = event.getSlot();
        switch (slot) {
            case 19 -> executeViewEmployees(player);
            case 20 -> executeViewJobs(player);
            case 21 -> showDepositHint(player);
            // ... more cases ...
        }
    }
}
```

### 3. Plot Edit GUI

**Command:** Opened from company settings when managing plots

**Purpose:**
Visualize and manage company-owned plots.

**Features:**
- Map view of owned plots
- Click to teleport
- Click to show borders
- Sell plot option
- Rent status display

### 4. GUI Configuration System

**Overview:**
All GUIs load configuration from `guis.yml` at runtime.

**Benefits:**
- No code changes needed for layout adjustments
- Server owners can customize appearance
- Easy localization
- Runtime reloading possible

**Loading Configuration:**
```java
public class GuiConfig {
    private FileConfiguration config;
    
    public GuiConfig(File dataFolder) {
        File configFile = new File(dataFolder, "guis.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public String getTitle(String guiName) {
        return config.getString(guiName + ".title");
    }
    
    public int getSize(String guiName) {
        return config.getInt(guiName + ".size", 54);
    }
    
    public ItemConfig getItem(String guiName, String itemName) {
        ConfigurationSection section = config.getConfigurationSection(
            guiName + ".items." + itemName
        );
        return ItemConfig.fromSection(section);
    }
}
```

## Development Guidelines

### Creating New GUI
1. Create GUI class extending base pattern
2. Define layout in `guis.yml`
3. Implement item population
4. Create event listener for clicks
5. Register listener in plugin
6. Add command to open GUI
7. Document in appropriate feature file

**Template:**
```java
public class YourGUI {
    private final Player player;
    private final Inventory inventory;
    private final GuiConfig guiConfig;
    
    public YourGUI(Player player, GuiConfig guiConfig) {
        this.player = player;
        this.guiConfig = guiConfig;
        
        String title = guiConfig.getTitle("yourGui");
        int size = guiConfig.getSize("yourGui");
        this.inventory = Bukkit.createInventory(null, size, title);
        
        populateItems();
    }
    
    private void populateItems() {
        // Load items from config and populate inventory
    }
    
    public void open() {
        player.openInventory(inventory);
    }
    
    public void refresh() {
        populateItems();
        player.updateInventory();
    }
}
```

### Adding GUI Items
1. Define in `guis.yml` under appropriate section
2. Set material, slot, name, lore
3. Configure click action (command, close, navigate)
4. Add permission requirement if needed
5. Implement click handler in listener

### GUI Best Practices
1. **Cancel Events:** Always cancel click events to prevent item movement
2. **Permission Checks:** Show disabled items for no-permission users
3. **Async Loading:** Load data async, update GUI on main thread
4. **Pagination:** Implement for lists > 45 items
5. **Refresh Option:** Allow players to update data without closing
6. **Close Handling:** Cleanup resources when GUI closes

### Handling Dynamic Content
```java
// Load instruments async
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    List<Instrument> instruments = instrumentService.getAllInstruments();
    
    // Update GUI on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        displayInstruments(instruments);
        player.updateInventory();
    });
});
```

## Common Patterns

### Pagination
```java
private int currentPage = 0;
private int itemsPerPage = 45;

public void nextPage() {
    if ((currentPage + 1) * itemsPerPage < totalItems) {
        currentPage++;
        populateItems();
    }
}

public void previousPage() {
    if (currentPage > 0) {
        currentPage--;
        populateItems();
    }
}

private void populateItems() {
    int start = currentPage * itemsPerPage;
    int end = Math.min(start + itemsPerPage, totalItems);
    
    for (int i = start; i < end; i++) {
        // Add item to inventory
    }
}
```

### Click Actions
```java
@EventHandler
public void onClick(InventoryClickEvent event) {
    if (!isOurGUI(event.getView())) return;
    
    event.setCancelled(true);
    
    Player player = (Player) event.getWhoClicked();
    int slot = event.getSlot();
    
    // Map slot to action
    ClickAction action = getActionForSlot(slot);
    if (action == null) return;
    
    switch (action.type()) {
        case COMMAND -> player.performCommand(action.command());
        case CLOSE -> player.closeInventory();
        case NAVIGATE -> openOtherGUI(player, action.target());
        case CUSTOM -> executeCustomAction(player, action.data());
    }
}
```

### Item Builder Pattern
```java
public class ItemBuilder {
    private ItemStack item;
    
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }
    
    public ItemBuilder name(String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return this;
    }
    
    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return this;
    }
    
    public ItemBuilder glow() {
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return this;
    }
    
    public ItemStack build() {
        return item;
    }
}
```

## Performance Considerations

### Async Loading
- Load data async to avoid blocking server thread
- Update GUI on main thread
- Cache frequently accessed data

### Memory Management
- Clear inventory references when closed
- Don't keep large datasets in memory
- Use weak references for temporary GUIs

### Update Frequency
- Don't update every tick
- Implement manual refresh buttons
- Batch updates when possible

## Troubleshooting

### Items Not Clickable
- Check if event is cancelled
- Verify listener is registered
- Check for conflicting plugins

### Data Not Updating
- Verify refresh() is called
- Check async/sync thread usage
- Look for caching issues

### Permission Issues
- Check permission nodes
- Verify permission check logic
- Test with different permission levels

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Market trading: `.github/copilot/features/market-trading.md`
- Company management: `.github/copilot/features/company-management.md`
