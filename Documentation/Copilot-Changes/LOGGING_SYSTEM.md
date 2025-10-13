# Centralized Logging System

## Overview

QuickStocks now uses a centralized logging system with configurable debug levels. The new `PluginLogger` class provides better control over what gets logged to the server console.

## Features

- **Configurable Debug Levels**: Control logging verbosity via `config.yml`
- **Consistent API**: All plugin code uses the same logging interface
- **Performance**: Debug/trace messages are only logged when explicitly enabled
- **Thread-Safe**: Safe for use in concurrent environments

## Configuration

### config.yml

```yaml
logging:
  # Debug level controls what messages are logged
  # 0 = OFF - Only errors and warnings
  # 1 = INFO - Basic operational messages (default)
  # 2 = DEBUG - Detailed debug information
  # 3 = TRACE - Very verbose tracing
  debugLevel: 1
```

### Debug Levels

| Level | Name  | Description | Use Case |
|-------|-------|-------------|----------|
| 0     | OFF   | Only severe errors and warnings | Production servers |
| 1     | INFO  | Basic operational messages | Default - normal operation |
| 2     | DEBUG | Detailed debug information | Troubleshooting issues |
| 3     | TRACE | Very verbose tracing | Development/debugging |

## Usage

### Accessing the Logger

```java
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

public class MyService {
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    // ... your code
}
```

### Logging Methods

#### Always Logged (Regardless of Debug Level)

```java
// Log severe errors
logger.severe("Critical error message");
logger.severe("Error with exception", throwable);

// Log warnings
logger.warning("Warning message");
logger.warning("Warning with exception", throwable);
```

#### Conditional Logging (Based on Debug Level)

```java
// INFO level (debugLevel >= 1)
logger.info("Service initialized successfully");

// DEBUG level (debugLevel >= 2)
logger.debug("Processing item: " + item.getName());

// TRACE level (debugLevel >= 3)
logger.trace("Entering method with params: " + params);
```

### Checking Log Levels

Before expensive string operations:

```java
if (logger.isDebugEnabled()) {
    logger.debug("Complex debug info: " + expensiveOperation());
}

if (logger.isTraceEnabled()) {
    logger.trace("Detailed trace: " + veryExpensiveOperation());
}
```

## Migration from java.util.logging.Logger

All code has been migrated from `java.util.logging.Logger` to `PluginLogger`:

### Before

```java
import java.util.logging.Logger;

public class MyService {
    private static final Logger logger = Logger.getLogger(MyService.class.getName());
    
    public void doSomething() {
        logger.info("Doing something");
        logger.fine("Debug info");
    }
}
```

### After

```java
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

public class MyService {
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    public void doSomething() {
        logger.info("Doing something");
        logger.debug("Debug info");
    }
}
```

### Method Mapping

| Old Method | New Method | Notes |
|------------|------------|-------|
| `logger.severe()` | `logger.severe()` | Always logged |
| `logger.warning()` | `logger.warning()` | Always logged |
| `logger.info()` | `logger.info()` | Requires debugLevel >= 1 |
| `logger.fine()` | `logger.debug()` | Requires debugLevel >= 2 |
| `logger.finer()` | `logger.trace()` | Requires debugLevel >= 3 |
| `logger.finest()` | `logger.trace()` | Requires debugLevel >= 3 |

## Implementation Details

### PluginLogger Class

Located in: `net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger`

Key features:
- Wraps Bukkit's logger for consistent behavior
- Debug level is clamped between 0-3
- Thread-safe for concurrent access
- Minimal overhead when debug logging is disabled

### Initialization

The logger is initialized in `QuickStocksPlugin.onEnable()`:

```java
@Override
public void onEnable() {
    // ...
    
    // Initialize centralized logger
    int debugLevel = getConfig().getInt("logging.debugLevel", 1);
    pluginLogger = new PluginLogger(this, debugLevel);
    pluginLogger.info("PluginLogger initialized with debug level: " + debugLevel);
    
    // ...
}
```

## Best Practices

### When to Use Each Level

**SEVERE** - Use for critical errors that prevent functionality:
```java
logger.severe("Failed to connect to database: " + ex.getMessage());
```

**WARNING** - Use for recoverable issues or deprecation notices:
```java
logger.warning("Vault not found, using internal wallet system");
```

**INFO** - Use for important operational events:
```java
logger.info("Market simulation started with 100 instruments");
```

**DEBUG** - Use for detailed diagnostic information:
```java
logger.debug("Set balance for " + uuid + " to $" + amount);
```

**TRACE** - Use for very detailed tracing:
```java
logger.trace("Processing stock: " + stock + " with factors: " + factors);
```

### Performance Considerations

Avoid expensive operations in log messages without checking the level:

```java
// ❌ BAD - String concatenation always executed
logger.debug("State: " + expensiveToString());

// ✅ GOOD - Check level first
if (logger.isDebugEnabled()) {
    logger.debug("State: " + expensiveToString());
}
```

### Configuration Recommendations

**Production Servers:**
```yaml
logging:
  debugLevel: 1  # INFO level - normal operation
```

**Development/Testing:**
```yaml
logging:
  debugLevel: 2  # DEBUG level - more details
```

**Bug Investigation:**
```yaml
logging:
  debugLevel: 3  # TRACE level - maximum verbosity
```

## Troubleshooting

### No Debug Messages Appearing

1. Check `config.yml` - ensure `logging.debugLevel` is >= 2
2. Reload configuration: `/reload` or restart server
3. Verify logger is initialized after config load

### Too Many Log Messages

1. Reduce `debugLevel` in `config.yml`
2. Level 1 (INFO) is recommended for production
3. Level 0 (OFF) disables all debug messages

### Performance Issues

If logging causes performance problems:
1. Set `debugLevel: 0` to disable debug logging
2. Check code for expensive log operations
3. Add level checks before expensive string operations

## Future Enhancements

Potential future improvements:
- Per-class or per-package debug levels
- Log file rotation and management
- Integration with external logging frameworks
- Runtime debug level changes via commands

## Related Files

- `PluginLogger.java` - Main logger implementation
- `QuickStocksPlugin.java` - Logger initialization
- `config.yml` - Configuration
- All service/command/listener classes - Logger usage

## Breaking Changes

### Version 1.2.0

- Replaced `java.util.logging.Logger` with `PluginLogger` in all classes
- Changed `logger.fine()` to `logger.debug()`
- Info messages now require `debugLevel >= 1` to be logged
- New configuration section: `logging.debugLevel`

## Support

For issues or questions:
- GitHub Issues: https://github.com/Cybernetic-Forge/QuickStocks/issues
- Documentation: https://github.com/Cybernetic-Forge/QuickStocks/wiki
