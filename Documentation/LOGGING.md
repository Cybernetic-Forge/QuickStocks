# QuickStocks Logging System

## Quick Reference

### Configuration (config.yml)

```yaml
logging:
  debugLevel: 1  # 0=OFF, 1=INFO (default), 2=DEBUG, 3=TRACE
```

### Usage in Code

```java
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

public class MyService {
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    public void myMethod() {
        logger.severe("Critical error");    // Always logged
        logger.warning("Warning message");  // Always logged
        logger.info("Normal operation");    // Logged if debugLevel >= 1
        logger.debug("Debug details");      // Logged if debugLevel >= 2
        logger.trace("Trace information");  // Logged if debugLevel >= 3
    }
}
```

## Debug Levels

| Level | Name  | What Gets Logged | Recommended For |
|-------|-------|------------------|-----------------|
| 0     | OFF   | Errors & Warnings only | Production (minimal logging) |
| 1     | INFO  | + Basic operations | **Default** - Normal production |
| 2     | DEBUG | + Detailed debugging | Troubleshooting issues |
| 3     | TRACE | + Very verbose traces | Development & bug investigation |

## Common Scenarios

### Production Server (Default)
```yaml
logging:
  debugLevel: 1
```
Shows important operational messages without flooding the console.

### Troubleshooting Issues
```yaml
logging:
  debugLevel: 2
```
Enables detailed debug information to diagnose problems.

### Development/Bug Investigation
```yaml
logging:
  debugLevel: 3
```
Maximum verbosity for detailed analysis.

### Quiet Server
```yaml
logging:
  debugLevel: 0
```
Only critical errors and warnings - minimal noise.

## Best Practices

### When to Use Each Level

**SEVERE** - Critical errors that prevent functionality:
```java
logger.severe("Failed to load database: " + ex.getMessage());
```

**WARNING** - Issues that don't break functionality:
```java
logger.warning("Vault plugin not found, using internal economy");
```

**INFO** - Important operational events:
```java
logger.info("Market opened with 50 active instruments");
```

**DEBUG** - Detailed information for troubleshooting:
```java
logger.debug("Player " + uuid + " balance updated to $" + amount);
```

**TRACE** - Very detailed execution flow:
```java
logger.trace("Calculating price with factors: " + factorList);
```

### Performance Optimization

Avoid expensive operations without checking the level:

```java
// ❌ BAD - Always creates the string
logger.debug("Data: " + expensiveToString());

// ✅ GOOD - Only executes if debug is enabled
if (logger.isDebugEnabled()) {
    logger.debug("Data: " + expensiveToString());
}
```

## Migrating from java.util.logging.Logger

Old pattern:
```java
import java.util.logging.Logger;

private static final Logger logger = Logger.getLogger(MyClass.class.getName());

logger.info("Message");
logger.fine("Debug message");
```

New pattern:
```java
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;

private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

logger.info("Message");
logger.debug("Debug message");  // Changed from fine()
```

## For Server Administrators

To adjust logging verbosity:

1. Open `plugins/QuickStocks/config.yml`
2. Find the `logging` section
3. Change `debugLevel` (0-3)
4. Reload the plugin or restart the server

**Tip**: Start with level 1 (default) and only increase if you need more details for troubleshooting.

## For Developers

When adding new logging:
- Use appropriate levels (don't use info() for everything)
- Keep messages concise but informative
- Include relevant context (player names, IDs, amounts, etc.)
- Use level checks for expensive string operations
- Avoid logging in tight loops (consider trace level if necessary)

## Full Documentation

For complete details, see: [LOGGING_SYSTEM.md](Copilot-Changes/LOGGING_SYSTEM.md)
