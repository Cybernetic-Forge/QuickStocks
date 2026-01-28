# Minecraft Plugin Development - QuickStocks Skill

**Domain**: Minecraft Plugin Development (Paper/Spigot/Bukkit)
**Last Updated**: 2026-01-28

## Overview

QuickStocks is a Paper plugin for Minecraft 1.21.8 that requires specific knowledge of the Bukkit/Spigot/Paper API and Minecraft plugin development patterns.

## Key Technologies

- **Paper API** - Modern Minecraft server software
- **Bukkit/Spigot API** - Core plugin framework
- **Adventure Components** - Modern text component system for rich formatting
- **plugin.yml** - Plugin descriptor and metadata

## Core Patterns

### 1. Plugin Initialization

```java
public class QuickStocksPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Initialize services, commands, listeners
        // Load configuration
        // Setup database connections
    }
    
    @Override
    public void onDisable() {
        // Clean shutdown
        // Close database connections
        // Save data
    }
}
```

### 2. Command Registration

Commands are registered in `plugin.yml` and implemented via `CommandExecutor`:

```java
public class MyCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Handle player vs console
        if (!(sender instanceof Player player)) {
            // Send console error message
            return true;
        }
        
        // Process command
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Return tab completion suggestions
        return List.of();
    }
}
```

### 3. Event Handling

```java
public class MyListener implements Listener {
    @EventHandler
    public void onEvent(SomeEvent event) {
        // Handle event
    }
}
```

### 4. Adventure Components for Rich Text

QuickStocks uses Adventure API for modern text formatting:

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

Component message = Component.text("Hello, ")
    .color(NamedTextColor.GREEN)
    .append(Component.text("world!").color(NamedTextColor.GOLD));
    
player.sendMessage(message);
```

## QuickStocks-Specific Patterns

### Permission Checks

```java
if (!player.hasPermission("quickstocks.command.name")) {
    Translation.NoPermission.sendMessage(player);
    return true;
}
```

### Translation System

QuickStocks uses a centralized translation system:

```java
Translation.MessageKey.sendMessage(player);
Translation.MessageKey.sendMessage(player, placeholder1, placeholder2);
```

### Service Layer Access

Always access business logic through service layer:

```java
private final WalletService walletService;
private final MarketService marketService;

// In constructor
this.walletService = plugin.getWalletService();
this.marketService = plugin.getMarketService();
```

## Best Practices

1. **Thread Safety**: Bukkit's main thread must handle most operations
2. **Async Operations**: Use `Bukkit.getScheduler().runTaskAsynchronously()` for database operations
3. **Sync Operations**: Use `Bukkit.getScheduler().runTask()` to return to main thread
4. **Error Handling**: Always handle exceptions gracefully and inform users
5. **Permission Checks**: Validate permissions before allowing operations
6. **Player vs Console**: Always check if sender is a Player when needed
7. **Resource Cleanup**: Close resources in `onDisable()`

## Common Pitfalls

- ❌ Modifying world state from async threads
- ❌ Blocking main thread with long operations
- ❌ Not closing database connections
- ❌ Forgetting to register commands in plugin.yml
- ❌ Not checking permissions before operations
- ❌ Using deprecated Bukkit methods

## Testing with MockBukkit

QuickStocks uses MockBukkit for unit testing:

```java
@BeforeEach
void setUp() {
    this.server = MockBukkit.mock();
    this.plugin = MockBukkit.load(QuickStocksPlugin.class);
}

@AfterEach
void tearDown() {
    MockBukkit.unmock();
}
```

## Resources

- [Paper API Documentation](https://docs.papermc.io/)
- [Spigot/Bukkit API Javadocs](https://hub.spigotmc.org/javadocs/spigot/)
- [Adventure API Documentation](https://docs.advntr.dev/)
- [MockBukkit Documentation](https://github.com/MockBukkit/MockBukkit)
