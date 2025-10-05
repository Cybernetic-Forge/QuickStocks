# bStats Integration Documentation

## Overview

QuickStocks now integrates with [bStats](https://bstats.org/), an open-source metrics collection platform for Minecraft plugins. This allows us to collect anonymous usage statistics to better understand how the plugin is used and make informed decisions about future development.

## What is bStats?

bStats is a free, open-source plugin statistics service used by thousands of Minecraft plugins. It collects anonymous data about plugin installations, usage patterns, and server configurations to help developers improve their plugins.

### Key Features of bStats
- ✅ **Completely Anonymous** - No player data or server IPs are collected
- ✅ **Open Source** - The bStats codebase is publicly available
- ✅ **Opt-Out Available** - Server owners can disable it at any time
- ✅ **Industry Standard** - Used by most major Minecraft plugins
- ✅ **Privacy Focused** - Complies with GDPR and privacy regulations

## Implementation Details

### Maven Dependencies

Added to `pom.xml`:
```xml
<repository>
    <id>CodeMC</id>
    <url>https://repo.codemc.org/repository/maven-public/</url>
</repository>

<dependency>
    <groupId>org.bstats</groupId>
    <artifactId>bstats-bukkit</artifactId>
    <version>3.1.0</version>
    <scope>compile</scope>
</dependency>
```

### Maven Shade Plugin

Configured to relocate bStats classes to avoid conflicts:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>org.bstats</pattern>
                        <shadedPattern>net.cyberneticforge.quickstocks.bstats</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### MetricsService Class

Created a new service class at `src/main/java/net/cyberneticforge/quickstocks/core/services/MetricsService.java`:

**Key Components:**
- Plugin ID: `24106` (registered on bStats)
- Initialization in plugin startup
- Custom charts registration
- Graceful shutdown handling

**Constructor Parameters:**
- `JavaPlugin plugin` - The plugin instance
- `DatabaseConfig databaseConfig` - For database provider metrics
- `StockMarketService stockMarketService` - For market statistics
- `CompanyService companyService` - For company count
- `HoldingsService holdingsService` - For active trader count

## Custom Metrics Collected

### 1. Database Provider (Simple Pie Chart)
**Chart ID:** `database_provider`
**Type:** Simple Pie Chart
**Data:** Distribution of database types (SQLite, MySQL, PostgreSQL)

**Purpose:** Understand which database backends are most popular to prioritize optimization and bug fixes.

### 2. Active Instruments (Single Line Chart)
**Chart ID:** `active_instruments`
**Type:** Single Line Chart
**Data:** Total number of tradeable instruments (stocks + cryptos)

**Purpose:** Track market size growth across servers and understand typical market scales.

### 3. Total Companies (Single Line Chart)
**Chart ID:** `total_companies`
**Type:** Single Line Chart
**Data:** Number of companies created on each server

**Purpose:** Monitor adoption of the company feature and economic activity levels.

### 4. Active Traders (Single Line Chart)
**Chart ID:** `active_traders`
**Type:** Single Line Chart
**Data:** Number of unique players with holdings

**Purpose:** Understand player engagement with the trading system.

### 5. Market Status (Simple Pie Chart)
**Chart ID:** `market_status`
**Type:** Simple Pie Chart
**Data:** Distribution of market open vs closed states

**Purpose:** See how servers configure their markets (always open vs scheduled hours).

## Configuration

### Config File (`config.yml`)

Added new configuration section:
```yaml
# bStats Metrics Configuration
metrics:
  enabled: true  # Enable anonymous usage statistics (bStats)
```

**Default:** `enabled: true`

### Disabling Metrics

Server owners can opt-out in three ways:

1. **Plugin-specific (QuickStocks only):**
   ```yaml
   # In plugins/QuickStocks/config.yml
   metrics:
     enabled: false
   ```

2. **Global bStats disable (all plugins):**
   ```yaml
   # In plugins/bStats/config.yml
   enabled: false
   ```

3. **Server-wide firewall:**
   Block outbound connections to `https://bstats.org/` (not recommended)

## Integration Points

### Plugin Startup (`QuickStocksPlugin.onEnable()`)

Added after services initialization:
```java
// Initialize bStats metrics if enabled
if (getConfig().getBoolean("metrics.enabled", true)) {
    metricsService = new MetricsService(
        this,
        config,
        stockMarketService,
        companyService,
        holdingsService
    );
    metricsService.initialize();
}
```

### Plugin Shutdown (`QuickStocksPlugin.onDisable()`)

Added before database shutdown:
```java
// Shutdown metrics
if (metricsService != null) {
    metricsService.shutdown();
}
```

### HoldingsService Extension

Added method to count active traders:
```java
/**
 * Gets the count of unique players with holdings (for metrics).
 * @return Number of players with at least one holding
 */
public int getPlayerCountWithHoldings() throws SQLException {
    String result = database.queryValue(
        "SELECT COUNT(DISTINCT player_uuid) FROM user_holdings WHERE qty > 0"
    );
    return result != null ? Integer.parseInt(result) : 0;
}
```

## Privacy & Data Protection

### What is Collected
- ✅ Server version (Minecraft version, e.g., "1.21.8")
- ✅ Plugin version (e.g., "1.0.0-SNAPSHOT")
- ✅ Server player count (approximate ranges, not exact)
- ✅ Database provider type (SQLite/MySQL/PostgreSQL)
- ✅ Number of instruments, companies, and traders (aggregate counts)
- ✅ Market open/closed status

### What is NOT Collected
- ❌ Server IP address or hostname
- ❌ Player names, UUIDs, or any identifying information
- ❌ Server name or location
- ❌ Chat messages or commands
- ❌ Economy balances or transaction details
- ❌ Company names or details
- ❌ Trade history or holdings
- ❌ Any configuration values

### GDPR Compliance
- All data is anonymous and aggregated
- No personal data is collected or stored
- Server owners can opt-out at any time
- Data retention follows bStats policies
- Public statistics are aggregate across all servers

## Viewing Statistics

### Public Dashboard
**URL:** https://bstats.org/plugin/bukkit/QuickStocks/24106

The dashboard shows:
- Total number of servers using QuickStocks
- Player count distribution
- Plugin version distribution
- Custom chart data (database types, market stats, etc.)
- Historical trends over time

### Access
- Public statistics are viewable by anyone
- No authentication required
- Real-time updates (refreshed periodically)
- Export options available (CSV, JSON)

## Benefits to Development

### For Plugin Developers
1. **Usage Insights** - Understand real-world usage patterns
2. **Feature Prioritization** - Focus on popular features
3. **Database Optimization** - Optimize for most common database types
4. **Market Scale Planning** - Design for typical market sizes
5. **Bug Triage** - Prioritize issues affecting most users

### For Server Administrators
1. **Transparency** - See how other servers use the plugin
2. **Best Practices** - Learn from aggregate statistics
3. **Configuration Guidance** - Understand typical setups
4. **Community Size** - Know the plugin's adoption rate

## Testing & Verification

### Local Testing
When testing locally, metrics will still be sent unless disabled. To test without sending data:

```yaml
metrics:
  enabled: false
```

### Production Deployment
1. Deploy plugin with metrics enabled (default)
2. Check server logs for "bStats metrics initialized successfully"
3. Wait 30-60 minutes for first data submission
4. View dashboard at https://bstats.org/plugin/bukkit/QuickStocks/24106

## Troubleshooting

### Metrics Not Showing Up
1. **Check configuration:** Ensure `metrics.enabled: true` in config.yml
2. **Check logs:** Look for bStats initialization messages
3. **Check firewall:** Ensure outbound HTTPS to bstats.org is allowed
4. **Wait:** First submission can take 30-60 minutes
5. **Global disable:** Check `plugins/bStats/config.yml` isn't disabling globally

### Error Messages
```
Failed to initialize bStats metrics: <error>
```

**Common causes:**
- Network connectivity issues
- Firewall blocking outbound connections
- Plugin initialization failure (check earlier logs)

### Disabling Failed
If metrics remain enabled after configuration:
1. Verify config.yml syntax is correct
2. Restart server after config changes
3. Check for config reload commands
4. Verify file permissions on config.yml

## Future Enhancements

### Potential Additional Metrics
- Trading volume statistics (total trades per time period)
- Order type distribution (market vs limit vs stop orders)
- Company type distribution (PRIVATE vs PUBLIC vs DAO)
- Feature usage (ChestShop integration, watchlists, etc.)
- Performance metrics (update intervals, query times)

### Advanced Charts
- Multi-line charts for trending data
- Advanced pie charts with multiple categories
- Drilldown charts for detailed analysis
- Geographic distribution (country-level, if ethical)

## Resources

### Documentation
- **bStats Documentation:** https://bstats.org/getting-started
- **bStats API:** https://github.com/Bastian/bStats-Metrics
- **Privacy Policy:** https://bstats.org/privacy

### Support
- **bStats Discord:** https://discord.bstats.org/
- **GitHub Issues:** https://github.com/Bastian/bStats-Metrics/issues

## Conclusion

The bStats integration provides valuable insights while respecting user privacy. It's completely anonymous, opt-out is easy, and the data helps us make QuickStocks better for everyone.

**Remember:** If you have any concerns about metrics collection, you can always disable it with:
```yaml
metrics:
  enabled: false
```

No hard feelings—we still appreciate your use of QuickStocks! 🎉
