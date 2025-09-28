# QuickStocks Plugin - Copilot Development Guidelines

## Code Standards and Rules

### 1. Internationalization (I18n) Requirements

**RULE: NO NEW HARDCODED STRINGS**
- All player-facing messages MUST use the I18n system
- Never use hardcoded strings in `Component.text()` or `sendMessage()` calls
- Always use `I18n.tr()` or `I18n.component()` for user messages

#### I18n Usage Examples

```java
// ❌ BAD - Hardcoded string
sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));

// ✅ GOOD - Using I18n
sender.sendMessage(I18n.component("general.player_not_found"));

// ✅ GOOD - With placeholders
Map<String, Object> placeholders = Map.of("player", playerName);
sender.sendMessage(I18n.component("general.player_not_found_with_name", placeholders));
```

#### Adding New Translation Keys

1. Add the key to `src/main/resources/Translations.yml`:
```yaml
general:
  new_message: "&cYour new message with {placeholder}"
```

2. Add fallback to `I18n.java` in the `initializeFallbacks()` method:
```java
fallbacks.put("general.new_message", "&cYour new message with {placeholder}");
```

### 2. Translation Key Naming Convention

- Use dot notation: `category.specific_message`
- Categories: `general`, `stocks`, `crypto`, `market`, `plugin`, `quickstocks`
- Use lowercase with underscores: `player_not_found`, `invalid_symbol`
- Be descriptive: `crypto.symbol_already_exists` not `crypto.error1`

### 3. Placeholder Guidelines

- Use descriptive placeholder names: `{playerName}`, `{symbol}`, `{price}`
- Always provide placeholders as Map<String, Object>
- Common placeholders:
  - `{symbol}` - Stock/crypto symbol
  - `{price}` - Price values
  - `{error}` - Error messages
  - `{player}` - Player names
  - `{count}` - Numeric counts

### 4. Color Code Standards

- Use `&` color codes in translations, NOT in Java code
- Common colors:
  - `&c` - Red (errors)
  - `&a` - Green (success)
  - `&e` - Yellow (info/labels)
  - `&7` - Gray (secondary text)
  - `&b` - Aqua (values/data)
  - `&6` - Gold (headers)

### 5. I18n System Features

#### Hot Reload
- Use `/quickstocks reload` to reload translations without restart
- Requires `quickstocks.admin.reload` permission

#### Fallback System
- Missing translation keys automatically fall back to English defaults
- System logs warnings for missing keys
- Never returns null - always returns a usable string

#### Component Integration
- `I18n.component(key)` returns Adventure Components with parsed colors
- `I18n.tr(key)` returns raw strings
- Use `I18n.component()` for `sendMessage()` calls

### 6. Command Development Guidelines

#### Command Structure
```java
public class MyCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (sender instanceof Player player && !player.hasPermission("my.permission")) {
            sender.sendMessage(I18n.component("general.no_permission"));
            return true;
        }
        
        // Logic here
        
        return true;
    }
}
```

#### Error Handling
```java
try {
    // Database operations
} catch (SQLException e) {
    Map<String, Object> placeholders = Map.of("error", e.getMessage());
    sender.sendMessage(I18n.component("general.database_error", placeholders));
}
```

### 7. Testing I18n Changes

1. Test with default English translations
2. Test with missing keys (should fall back gracefully)
3. Test placeholder substitution
4. Test color code parsing
5. Test reload functionality

### 8. File Structure

```
src/main/java/com/example/quickstocks/
├── I18n.java                     # Translation utility
├── commands/
│   ├── StocksCommand.java        # Uses I18n throughout
│   ├── CryptoCommand.java        # Uses I18n throughout
│   └── QuickStocksCommand.java   # Admin commands with I18n
└── QuickStocksPlugin.java        # I18n initialization

src/main/resources/
└── Translations.yml              # All translation keys
```

### 9. Migration from Existing Code

When updating existing code:

1. Identify all hardcoded strings
2. Create appropriate translation keys
3. Add fallbacks to `I18n.java`
4. Replace hardcoded strings with `I18n.tr()` or `I18n.component()`
5. Test thoroughly

### 10. Best Practices

- Keep translation keys organized by feature/category
- Use consistent naming across similar messages
- Provide meaningful placeholders
- Test edge cases (missing keys, invalid placeholders)
- Document any complex translation requirements
- Use `I18n.component()` for colored messages, `I18n.tr()` for plain text

## Example Complete Command with I18n

```java
package com.example.quickstocks.commands;

import com.example.quickstocks.I18n;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class ExampleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Permission check
        if (sender instanceof Player player && !player.hasPermission("example.use")) {
            sender.sendMessage(I18n.component("general.no_permission"));
            return true;
        }
        
        // Validation
        if (args.length == 0) {
            sender.sendMessage(I18n.component("example.usage"));
            return true;
        }
        
        // Success with placeholder
        Map<String, Object> placeholders = Map.of("value", args[0]);
        sender.sendMessage(I18n.component("example.success", placeholders));
        
        return true;
    }
}
```

## Remember: NO HARDCODED STRINGS!

Every user-facing message must go through the I18n system. This ensures:
- Consistent messaging
- Future localization support
- Easy message maintenance
- Hot-reload capability
- Fallback reliability