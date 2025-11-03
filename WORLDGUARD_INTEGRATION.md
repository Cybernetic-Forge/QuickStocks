# WorldGuard Integration

## Overview
QuickStocks integrates with WorldGuard to provide fine-grained control over plugin features within WorldGuard regions. This allows server administrators to restrict or allow QuickStocks functionality based on region flags.

## Features

### Custom WorldGuard Flags
QuickStocks registers three custom StateFlags when WorldGuard is installed:

1. **`quickstocks-plots`** - Controls plot purchases
2. **`quickstocks-trading`** - Controls stock trading (future use)
3. **`quickstocks-chestshops`** - Controls ChestShop creation (future use)

## Installation

### Requirements
- WorldGuard 7.0.9 or higher
- QuickStocks plugin installed

### Setup
1. Install WorldGuard plugin on your server
2. Install QuickStocks plugin
3. Restart the server
4. Custom flags are automatically registered

## Usage

### Setting Region Flags

#### Allow plot purchases in a region (default):
```
/rg flag <region> quickstocks-plots allow
```

#### Deny plot purchases in a region:
```
/rg flag <region> quickstocks-plots deny
```

#### Remove flag (defaults to allow):
```
/rg flag <region> quickstocks-plots -
```

### Flag Behavior

| Flag State | Result |
|-----------|--------|
| `allow` | Players can buy plots in the region |
| `deny` | Plot purchases are blocked with error message |
| Not set (null) | Defaults to `allow` |

### Example Scenarios

#### Protect spawn area from plot purchases:
```
/rg define spawn
/rg flag spawn quickstocks-plots deny
```

#### Create a trading district where plots can be purchased:
```
/rg define trading-district
/rg flag trading-district quickstocks-plots allow
```

#### Wilderness area with no restrictions (default):
```
# No flag needed - defaults to allow
```

## Technical Details

### Flag Registration
- Flags are registered during plugin initialization (before WorldGuard finishes loading)
- If a flag already exists, QuickStocks will attempt to use it
- Flag conflicts are logged as warnings

### Permission Checks
When a player attempts to buy a plot:
1. Check if WorldGuard is installed
2. If yes, query the `quickstocks-plots` flag at the location
3. If flag is `deny`, block the action with error message
4. If flag is `allow` or not set, allow the action

### Graceful Degradation
- Plugin works normally without WorldGuard
- If WorldGuard is not installed, all actions are allowed by default
- If flag check fails, defaults to allowing the action (with warning logged)

## Error Messages

### "You cannot buy plots in this WorldGuard region"
This message appears when:
- WorldGuard is installed
- The player is in a WorldGuard region
- The `quickstocks-plots` flag is set to `deny`

**Solution:** Ask a server admin to change the flag or move to a different region.

## Future Enhancements

### `quickstocks-trading` Flag
When implemented, this flag will control whether players can:
- Trade stocks via `/market` command
- Use market devices
- Buy/sell holdings

### `quickstocks-chestshops` Flag
When implemented, this flag will control whether:
- Companies can create ChestShops
- ChestShops can be placed in the region

## API Usage

For plugin developers integrating with QuickStocks:

```java
// Check if WorldGuard hook is available
if (QuickStocksPlugin.getHookManager().isHooked(HookType.WorldGuard)) {
    WorldGuardHook wgHook = QuickStocksPlugin.getWorldGuardHook();
    
    // Check if player can buy plot at location
    boolean canBuy = wgHook.canBuyPlot(player, location);
    
    // Check if player can trade at location
    boolean canTrade = wgHook.canTrade(player, location);
    
    // Check if player can create ChestShop at location
    boolean canCreateShop = wgHook.canCreateChestShop(player, location);
}
```

## Troubleshooting

### Flags not working
1. Verify WorldGuard is installed: `/plugins`
2. Check flag is set correctly: `/rg info <region>`
3. Check server console for WorldGuard integration messages
4. Ensure player is within the region: `/rg info`

### Flags not registered
Check server console for messages:
- `WorldGuard detected, registering custom flags...`
- `Registered WorldGuard flag: quickstocks-plots`

If missing:
1. Ensure WorldGuard loads before QuickStocks
2. Check `plugin.yml` has `softdepend: [WorldGuard]`
3. Verify WorldGuard version is 7.0.9+

### Flag conflicts
If you see warnings about flag conflicts:
- Another plugin may be using the same flag name
- Flags are already registered from a previous server start
- QuickStocks will attempt to use the existing flag

## Configuration

No additional configuration is needed. The integration is automatic when WorldGuard is detected.

### Debug Logging
To see detailed WorldGuard integration logs, set debug level in `config.yml`:
```yaml
logging:
  debugLevel: 2  # or 3 for very verbose
```

## Performance Impact

The WorldGuard integration has minimal performance impact:
- Flag checks are only performed when actions occur (plot purchases)
- No continuous polling or background tasks
- Cached region lookups via WorldGuard's native API

## Compatibility

### Tested With
- WorldGuard 7.0.9
- Paper 1.21.8
- Java 21

### Known Issues
None currently.

## Credits

WorldGuard integration implemented as part of QuickStocks company management system.

## See Also
- [Company Plots Feature](COMPANY_PLOTS_FEATURE.md) - Plot management documentation
- [WorldGuard Documentation](https://worldguard.enginehub.org/en/latest/) - Official WorldGuard docs
- [Custom Flags](https://worldguard.enginehub.org/en/latest/regions/flags/) - WorldGuard flag system
