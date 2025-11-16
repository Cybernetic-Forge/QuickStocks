# Plugin Integrations - Copilot Instructions

## Overview
QuickStocks integrates with popular Minecraft plugins to provide enhanced functionality. All integrations are optional soft dependencies that gracefully degrade if not available.

## Supported Integrations

### 1. Vault Economy Integration

**Purpose:** Economy system integration for wallet management

**Status:** ✅ Implemented, Optional

**Hook Location:** `core/services/features/portfolio/WalletService.java`

#### Features
- Automatic detection at runtime
- Reflection-based to avoid compile-time dependency
- Supports any Vault-compatible economy plugin
- Seamless fallback to internal wallet system

#### Compatible Economy Plugins
- Essentials
- CMI (Custom MadeIn Items)
- TheNewEconomy (TNE)
- CraftConomy
- Any plugin that implements Vault economy API

#### Implementation
```java
public class WalletService {
    private final boolean useVault;
    private Object vaultEconomy;  // Using Object to avoid compile-time dependency
    
    public WalletService() {
        this.useVault = setupVaultEconomy();
    }
    
    private boolean setupVaultEconomy() {
        try {
            // Use reflection to check for Vault
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Object pluginManager = /* get plugin manager */;
            Object vaultPlugin = /* get Vault plugin */;
            
            if (vaultPlugin == null) {
                return false;  // Vault not found, use internal
            }
            
            // Get economy service using reflection
            vaultEconomy = /* get economy provider */;
            return true;
        } catch (Exception e) {
            // Not available - use internal wallet
            return false;
        }
    }
}
```

#### Configuration
**Location:** `config.yml`
```yaml
economy:
  useVault: auto  # auto, true, false
  # auto = detect and use if available
  # true = require Vault (fail if not found)
  # false = always use internal wallet
```

#### Testing Vault Integration
1. Install Vault plugin
2. Install economy provider (Essentials, etc.)
3. Restart server
4. Check logs for "Using Vault economy integration"
5. Test `/wallet` commands
6. Verify balance operations use external economy

#### Fallback Behavior
If Vault not available:
- Internal wallet system activates automatically
- Uses `wallets` database table
- Same API, different backend
- No functionality lost

---

### 2. ChestShop Integration

**Purpose:** Company-owned chest shops with revenue tracking

**Status:** ✅ Implemented, Optional

**Hook Location:** `hooks/chestshop/ChestShopHook.java`

#### Features
- Companies can own ChestShop shops
- Revenue automatically credited to company balance
- Employee-based shop management
- Permission inheritance from company roles
- Transaction logging

#### Requirements
- ChestShop plugin installed
- QuickStocks companies feature enabled

#### Setup Process
1. Create company: `/company create ShopCorp PUBLIC`
2. Create ChestShop as normal (place chest, sign)
3. Assign to company using ChestShop admin commands
4. Revenue goes to company balance automatically

#### Hook Implementation
```java
public class ChestShopHook {
    private boolean isEnabled = false;
    
    public ChestShopHook() {
        // Check if ChestShop is available
        Plugin chestShop = Bukkit.getPluginManager().getPlugin("ChestShop");
        if (chestShop != null && chestShop.isEnabled()) {
            this.isEnabled = true;
            registerListeners();
            logger.info("ChestShop integration enabled");
        }
    }
    
    private void registerListeners() {
        // Listen for ChestShop transactions
        Bukkit.getPluginManager().registerEvents(
            new ChestShopTransactionListener(),
            plugin
        );
    }
}
```

#### Event Handling
```java
@EventHandler
public void onChestShopTransaction(ShopTransactionEvent event) {
    Shop shop = event.getShop();
    
    // Check if shop is owned by company
    String companyId = getCompanyOwner(shop);
    if (companyId == null) return;  // Not a company shop
    
    // Credit revenue to company
    double revenue = event.getPrice() * event.getQuantity();
    companyService.addBalance(companyId, revenue);
    
    // Log transaction
    companyService.recordTransaction(
        companyId,
        event.getCustomer().getUniqueId().toString(),
        TransactionType.SHOP_REVENUE,
        revenue,
        "ChestShop: " + event.getItem().getType()
    );
}
```

#### Configuration
**Location:** `companies.yml`
```yaml
companies:
  chestShopIntegration:
    enabled: true
    revenueShare: 1.0  # 100% to company, 0% to employee
```

#### Permission Handling
- CEO can manage all company shops
- Employees with `MANAGE_SHOPS` permission can manage
- Revenue always goes to company, not individual

---

### 3. WorldGuard Integration

**Purpose:** Region-based plot protection and management

**Status:** ✅ Implemented, Optional

**Hook Location:** `hooks/worldguard/WorldGuardHook.java`

#### Features
- Automatic region creation for company plots
- Employee-based build permissions
- Custom flags for plot features
- Rent-based region management
- Automatic region cleanup on plot sale

#### Requirements
- WorldGuard 7.0.9+
- WorldEdit (WorldGuard dependency)
- QuickStocks company plots feature enabled

#### Region Creation
When company buys plot:
```java
public void createPlotRegion(CompanyPlot plot, Company company) {
    if (!isWorldGuardAvailable()) return;
    
    // Calculate region bounds (chunk coordinates)
    BlockVector3 min = BlockVector3.at(plot.chunkX() * 16, 0, plot.chunkZ() * 16);
    BlockVector3 max = BlockVector3.at(min.getX() + 15, 255, min.getZ() + 15);
    
    // Create region
    ProtectedCuboidRegion region = new ProtectedCuboidRegion(
        "quickstocks_plot_" + plot.id(),
        min,
        max
    );
    
    // Set owner
    region.getOwners().addPlayer(UUID.fromString(company.ownerUuid()));
    
    // Add employees as members
    for (String employeeUuid : companyService.getEmployees(company.id())) {
        region.getMembers().addPlayer(UUID.fromString(employeeUuid));
    }
    
    // Add to region manager
    RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    RegionManager regions = container.get(world);
    regions.addRegion(region);
}
```

#### Custom Flags
```java
public static final StateFlag QUICKSTOCKS_PLOTS = 
    new StateFlag("quickstocks-plots", true);

// Register flag
FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
registry.register(QUICKSTOCKS_PLOTS);
```

#### Configuration
**Location:** `companies.yml`
```yaml
companies:
  plots:
    worldGuardIntegration:
      enabled: true
      autoCreateRegions: true
      regionPriority: 10
      flags:
        - "pvp=deny"
        - "mob-spawning=deny"
```

#### Permission Synchronization
When employee joins/leaves company:
```java
private void updateRegionPermissions(String companyId, String playerUuid, boolean add) {
    List<CompanyPlot> plots = plotService.getCompanyPlots(companyId);
    
    for (CompanyPlot plot : plots) {
        ProtectedRegion region = getRegion(plot.id());
        if (region == null) continue;
        
        UUID uuid = UUID.fromString(playerUuid);
        if (add) {
            region.getMembers().addPlayer(uuid);
        } else {
            region.getMembers().removePlayer(uuid);
        }
    }
}
```

---

### 4. PlaceholderAPI Integration

**Purpose:** Provide placeholders for other plugins

**Status:** 📋 Planned

**Planned Placeholders:**
- `%quickstocks_balance%` - Player balance
- `%quickstocks_portfolio_value%` - Total portfolio value
- `%quickstocks_holdings_<symbol>%` - Holdings of specific instrument
- `%quickstocks_company_balance%` - Company balance
- `%quickstocks_company_name%` - Player's primary company

---

### 5. DiscordSRV Integration

**Purpose:** Market notifications to Discord

**Status:** 📋 Planned

**Planned Features:**
- Company creation announcements
- IPO notifications
- Major price changes
- Circuit breaker alerts
- Top gainers/losers daily digest

---

## Integration Development Guidelines

### Adding New Integration
1. Create hook class in `hooks/<plugin>/`
2. Implement detection logic
3. Use reflection for compile-time independence
4. Provide graceful degradation
5. Add configuration options
6. Document in this file
7. Add to `plugin.yml` as soft dependency

**Template:**
```java
public class YourPluginHook {
    private boolean enabled = false;
    
    public YourPluginHook() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("YourPlugin");
        if (plugin != null && plugin.isEnabled()) {
            try {
                setupIntegration();
                this.enabled = true;
                logger.info("YourPlugin integration enabled");
            } catch (Exception e) {
                logger.warning("Failed to setup YourPlugin integration: " + e.getMessage());
            }
        }
    }
    
    private void setupIntegration() {
        // Setup using reflection if possible
        // Register listeners if needed
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
```

### Soft Dependency Declaration
**Location:** `src/main/resources/plugin.yml`
```yaml
name: QuickStocks
main: net.cyberneticforge.quickstocks.QuickStocksPlugin
version: 1.0.0

softdepend:
  - Vault
  - ChestShop
  - WorldGuard
  - PlaceholderAPI
  - DiscordSRV
```

### Testing Integrations
1. Test without integration plugin (should work)
2. Test with integration plugin (should enhance)
3. Test integration plugin loading after QuickStocks
4. Test integration plugin loading before QuickStocks
5. Test integration plugin unloading

### Error Handling
```java
try {
    // Integration code using reflection
    Class<?> pluginClass = Class.forName("com.plugin.MainClass");
    // ... more reflection code ...
} catch (ClassNotFoundException e) {
    // Plugin not available - graceful degradation
    logger.debug("Plugin not found, feature disabled");
} catch (Exception e) {
    // Other errors - log and continue
    logger.warning("Integration error: " + e.getMessage());
}
```

## Configuration

### Hook Manager
**Location:** `hooks/HookType.java`
```java
public enum HookType {
    VAULT("Vault"),
    CHESTSHOP("ChestShop"),
    WORLDGUARD("WorldGuard"),
    PLACEHOLDERAPI("PlaceholderAPI"),
    DISCORDSRV("DiscordSRV");
    
    private final String pluginName;
    
    HookType(String pluginName) {
        this.pluginName = pluginName;
    }
}
```

### Hook Initialization
**Location:** `QuickStocksPlugin.onEnable()`
```java
private void setupIntegrations() {
    // Vault (automatic in WalletService)
    // ChestShop
    if (getServer().getPluginManager().getPlugin("ChestShop") != null) {
        chestShopHook = new ChestShopHook();
    }
    
    // WorldGuard
    if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
        worldGuardHook = new WorldGuardHook();
    }
}
```

## Common Patterns

### Check if Integration Available
```java
if (QuickStocksPlugin.getChestShopHook() != null 
    && QuickStocksPlugin.getChestShopHook().isEnabled()) {
    // Use ChestShop integration
} else {
    // Fallback behavior
}
```

### Conditional Feature Activation
```java
if (hasWorldGuard()) {
    // Enable region protection
    createRegionForPlot(plot);
} else {
    // Just track ownership without protection
    logger.info("WorldGuard not available, plot protection disabled");
}
```

## Troubleshooting

### Integration Not Detected
1. Check plugin is installed and enabled
2. Verify version compatibility
3. Check load order in server logs
4. Review reflection errors in logs

### Integration Conflicts
1. Check for conflicting event priorities
2. Verify API version compatibility
3. Test with latest versions
4. Check for known incompatibilities

### Permission Issues
1. Verify permission plugins are compatible
2. Check permission inheritance
3. Review permission node conflicts
4. Test with different permission managers

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Company management: `.github/copilot/features/company-management.md`
- Portfolio wallet: `.github/copilot/features/portfolio-wallet.md`
