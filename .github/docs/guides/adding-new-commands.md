# How to Add New Commands to QuickStocks

**AI-Generated**: 2026-01-28  
**Purpose**: Guide for implementing new Minecraft commands  
**Related Skills**: [Minecraft Plugin Development](.github/skills/minecraft-plugin-development.md)

## Overview

This guide demonstrates the standard pattern for adding new commands to QuickStocks, following clean architecture principles.

## Step-by-Step Process

### 1. Define Command in plugin.yml

Add your command definition to `src/main/resources/plugin.yml`:

```yaml
commands:
  mycommand:
    description: Description of what the command does
    usage: /mycommand <arg1> [arg2]
    aliases: [mc, mycmd]
    permission: quickstocks.command.mycommand
```

### 2. Create Command Class

Create a new command class in `src/main/java/net/cyberneticforge/quickstocks/commands/`:

```java
package net.cyberneticforge.quickstocks.commands;

import net.cyberneticforge.quickstocks.core.services.MyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MyCommand implements CommandExecutor, TabCompleter {
    private final MyService myService;
    
    public MyCommand(MyService myService) {
        this.myService = myService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Check if sender is a player (if needed)
        if (!(sender instanceof Player player)) {
            Translation.NoConsoleSender.sendMessage(sender);
            return true;
        }
        
        // 2. Check permissions
        if (!player.hasPermission("quickstocks.command.mycommand")) {
            Translation.NoPermission.sendMessage(player);
            return true;
        }
        
        // 3. Validate arguments
        if (args.length < 1) {
            player.sendMessage("Usage: /mycommand <arg1> [arg2]");
            return true;
        }
        
        // 4. Parse arguments
        String arg1 = args[0];
        
        // 5. Delegate to service layer
        try {
            myService.doSomething(player.getUniqueId(), arg1);
            player.sendMessage("Success!");
        } catch (SomeException e) {
            player.sendMessage("Error: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument completions
            completions.add("option1");
            completions.add("option2");
        }
        
        return completions;
    }
}
```

### 3. Register Command in Plugin

In `QuickStocksPlugin.java`, register your command in the `onEnable()` method:

```java
@Override
public void onEnable() {
    // ... existing initialization ...
    
    // Create service
    MyService myService = new MyService(databaseManager);
    
    // Register command
    PluginCommand myCommand = getCommand("mycommand");
    if (myCommand != null) {
        MyCommand executor = new MyCommand(myService);
        myCommand.setExecutor(executor);
        myCommand.setTabCompleter(executor);
    }
}
```

### 4. Add Permission to plugin.yml

Add permission definition:

```yaml
permissions:
  quickstocks.command.mycommand:
    description: Allows use of /mycommand
    default: true
```

### 5. Add Translation Keys (Optional)

If using the translation system, add entries to your translation files.

### 6. Write Tests

Create tests in `src/test/java/net/cyberneticforge/quickstocks/commands/`:

```java
package net.cyberneticforge.quickstocks.commands;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class MyCommandTest {
    private ServerMock server;
    private QuickStocksPlugin plugin;
    private PlayerMock player;
    
    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(QuickStocksPlugin.class);
        player = server.addPlayer();
    }
    
    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }
    
    @Test
    void testCommandExecution() {
        // Test your command
        boolean result = player.performCommand("mycommand arg1");
        assertTrue(result);
    }
    
    @Test
    void testPermissionCheck() {
        // Test permission handling
        player.addAttachment(plugin, "quickstocks.command.mycommand", false);
        player.performCommand("mycommand arg1");
        // Assert appropriate error message
    }
}
```

## Best Practices

1. **Validation First**: Always validate input before processing
2. **Permission Checks**: Check permissions early in the command
3. **Thin Layer**: Commands should be thin - delegate to services
4. **Error Handling**: Catch exceptions and provide user-friendly messages
5. **Tab Completion**: Implement tab completion for better UX
6. **Testing**: Write tests for command logic

## Common Patterns

### Numeric Argument Parsing

```java
try {
    int amount = Integer.parseInt(args[1]);
    if (amount <= 0) {
        player.sendMessage("Amount must be positive!");
        return true;
    }
} catch (NumberFormatException e) {
    player.sendMessage("Invalid number format!");
    return true;
}
```

### Subcommand Handling

```java
if (args.length < 1) {
    showUsage(player);
    return true;
}

String subcommand = args[0].toLowerCase();
String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

switch (subcommand) {
    case "create" -> handleCreate(player, subArgs);
    case "delete" -> handleDelete(player, subArgs);
    case "list" -> handleList(player, subArgs);
    default -> showUsage(player);
}
```

## Related Resources

- [Minecraft Plugin Development Skill](.github/skills/minecraft-plugin-development.md)
- [Clean Architecture Patterns](.github/skills/clean-architecture-patterns.md)
- Existing commands: `src/main/java/net/cyberneticforge/quickstocks/commands/`
