# Configuration Guide

This guide explains all configuration options available in QuickStocks.

## 📁 Configuration Files

QuickStocks uses these configuration files:

```
plugins/QuickStocks/
├── config.yml          # Main configuration
└── Translations.yml    # Language translations
```

**After changes:** Restart server or use `/quickstocks reload`

---

## 🗄️ Database Configuration

Configure which database system to use and connection details.

### Database Provider

```yaml
database:
  provider: sqlite  # sqlite | mysql | postgres
```

**Options:**
- `sqlite` - Embedded database (default, no setup required)
- `mysql` - MySQL/MariaDB server
- `postgres` - PostgreSQL server

---

### SQLite Configuration

```yaml
database:
  provider: sqlite
  sqlite:
    file: plugins/QuickStocks/data.db
```

**Parameters:**
- `file` - Path to database file (relative to server root)

**When to use:**
- Small to medium servers (< 100 players)
- Single server setups
- Easy maintenance

---

### MySQL Configuration

```yaml
database:
  provider: mysql
  mysql:
    host: 127.0.0.1
    port: 3306
    database: quickstocks
    user: root
    password: "your_password"
    useSSL: false
```

**Parameters:**
- `host` - MySQL server address
- `port` - MySQL port (default: 3306)
- `database` - Database name
- `user` - Database username
- `password` - Database password
- `useSSL` - Enable SSL connections

**When to use:**
- Large servers (100+ players)
- Multiple server instances (BungeeCord/Velocity)
- Professional hosting environments

---

### PostgreSQL Configuration

```yaml
database:
  provider: postgres
  postgres:
    host: 127.0.0.1
    port: 5432
    database: quickstocks
    user: postgres
    password: "your_password"
```

**Parameters:**
- `host` - PostgreSQL server address
- `port` - PostgreSQL port (default: 5432)
- `database` - Database name
- `user` - Database username
- `password` - Database password

**When to use:**
- Enterprise deployments
- High-concurrency requirements
- Advanced database features needed

---

## 📊 Features Configuration

General plugin features and behavior.

```yaml
features:
  historyEnabled: true
  topListWindowHours: 24
```

**Parameters:**

### `historyEnabled`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable price history tracking
- **Impact:** Disabling saves database space but loses historical data

### `topListWindowHours`
- **Type:** Number (hours)
- **Default:** `24`
- **Description:** Time window for "top gainers" calculations
- **Range:** 1-168 (1 hour to 7 days)

---

## 📈 Market Configuration

Controls market simulation and price behavior.

### Basic Market Settings

```yaml
market:
  updateInterval: 5
  startOpen: true
  defaultStocks: true
```

**Parameters:**

### `updateInterval`
- **Type:** Number (seconds)
- **Default:** `5`
- **Description:** How often prices update
- **Range:** 1-300 (1 second to 5 minutes)
- **Performance:** Lower values = more CPU usage

### `startOpen`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Whether market starts open when server starts
- **Impact:** If false, admin must open market manually

### `defaultStocks`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Auto-seed Minecraft items as tradeable instruments
- **Impact:** If false, only custom crypto and companies are tradeable

---

### Price Threshold Configuration

Prevents unrealistic price movements.

```yaml
market:
  priceThreshold:
    enabled: true
    maxChangePercent: 0.15
    priceMultiplierThreshold: 5.0
    dampeningFactor: 0.3
    minVolumeThreshold: 100
    volumeSensitivity: 0.5
```

**Parameters:**

### `enabled`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable price dampening system

### `maxChangePercent`
- **Type:** Decimal (0.0-1.0)
- **Default:** `0.15` (15%)
- **Description:** Maximum price change per update before dampening
- **Example:** 0.15 = 15% max change

### `priceMultiplierThreshold`
- **Type:** Decimal
- **Default:** `5.0`
- **Description:** Price must be X times initial price before dampening
- **Example:** 5.0 = 500% of initial price

### `dampeningFactor`
- **Type:** Decimal (0.0-1.0)
- **Default:** `0.3`
- **Description:** Reduces price change impact when threshold exceeded
- **Example:** 0.3 = reduce to 30% of original change

### `minVolumeThreshold`
- **Type:** Number
- **Default:** `100`
- **Description:** Trading volume needed to ignore dampening
- **Purpose:** Active stocks can grow more freely

### `volumeSensitivity`
- **Type:** Decimal
- **Default:** `0.5`
- **Description:** How quickly volume affects dampening
- **Range:** 0.0-1.0 (higher = more sensitive)

---

## 📱 Market Device Configuration

Configure the Market Link Device item.

```yaml
marketDevice:
  recipe:
    enabled: false
    shapedRecipe: true
```

**Parameters:**

### `recipe.enabled`
- **Type:** Boolean
- **Default:** `false`
- **Description:** Allow players to craft Market Link Devices
- **Recommendation:** Keep disabled to maintain rarity

### `recipe.shapedRecipe`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Use shaped (3x3 grid) recipe vs shapeless
- **Note:** Only applies if crafting is enabled

**Crafting Recipe (if enabled):**
```
[Ender Pearl] [Diamond]     [Ender Pearl]
[Gold Ingot]  [Compass]     [Gold Ingot]
[Ender Pearl] [Redstone Bl] [Ender Pearl]
```

---

## 📊 Analytics Configuration

Configure market analytics and calculations.

```yaml
analytics:
  lambda: 0.94
  defaultWindowsMinutes:
    change: 1440
    volatility: 1440
    correlation: 1440
```

**Parameters:**

### `lambda`
- **Type:** Decimal (0.0-1.0)
- **Default:** `0.94`
- **Description:** Exponential moving average decay factor
- **Purpose:** Smooths price changes over time

### `defaultWindowsMinutes.change`
- **Type:** Number (minutes)
- **Default:** `1440` (24 hours)
- **Description:** Time window for price change calculations

### `defaultWindowsMinutes.volatility`
- **Type:** Number (minutes)
- **Default:** `1440` (24 hours)
- **Description:** Time window for volatility calculations

### `defaultWindowsMinutes.correlation`
- **Type:** Number (minutes)
- **Default:** `1440` (24 hours)
- **Description:** Time window for correlation analysis

---

## 💰 Trading Configuration

Configure trading fees, limits, and safeguards.

### Trading Fees

```yaml
trading:
  fee:
    mode: percent
    percent: 0.25
    flat: 0.0
```

**Parameters:**

### `fee.mode`
- **Type:** String
- **Options:** `percent`, `flat`, `mixed`
- **Default:** `percent`
- **Description:** Fee calculation method

### `fee.percent`
- **Type:** Decimal
- **Default:** `0.25` (0.25%)
- **Description:** Percentage fee on transaction value
- **Example:** 0.25 = $25 fee on $10,000 trade

### `fee.flat`
- **Type:** Decimal
- **Default:** `0.0`
- **Description:** Fixed fee per transaction
- **Example:** 10.0 = $10 per trade

---

### Trading Limits

```yaml
trading:
  limits:
    maxOrderQty: 10000
    maxNotionalPerMinute: 250000
    perPlayerCooldownMs: 750
```

**Parameters:**

### `limits.maxOrderQty`
- **Type:** Number
- **Default:** `10000`
- **Description:** Maximum shares per single order
- **Purpose:** Prevents market manipulation

### `limits.maxNotionalPerMinute`
- **Type:** Number
- **Default:** `250000`
- **Description:** Maximum $ value traded per minute per player
- **Purpose:** Rate limiting

### `limits.perPlayerCooldownMs`
- **Type:** Number (milliseconds)
- **Default:** `750`
- **Description:** Cooldown between trades for same player
- **Purpose:** Prevents spam trading

---

### Circuit Breakers

```yaml
trading:
  circuitBreakers:
    enable: true
    levels: [7, 13, 20]
    haltMinutes: [15, 15, -1]
```

**Parameters:**

### `circuitBreakers.enable`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable trading halts on large movements

### `circuitBreakers.levels`
- **Type:** Array of numbers (percentages)
- **Default:** `[7, 13, 20]`
- **Description:** Price movement thresholds for halts
- **Example:** 7% = first halt, 13% = second halt, 20% = third halt

### `circuitBreakers.haltMinutes`
- **Type:** Array of numbers (minutes)
- **Default:** `[15, 15, -1]`
- **Description:** How long to halt trading at each level
- **Note:** -1 = rest of trading session

---

### Order Types

```yaml
trading:
  orders:
    allowMarket: true
    allowLimit: true
    allowStop: true
```

**Parameters:**

### `orders.allowMarket`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Allow immediate market orders

### `orders.allowLimit`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Allow limit orders (price-specific)

### `orders.allowStop`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Allow stop-loss orders

---

### Slippage Configuration

```yaml
trading:
  slippage:
    mode: linear
    k: 0.0005
```

**Parameters:**

### `slippage.mode`
- **Type:** String
- **Options:** `none`, `linear`, `sqrtImpact`
- **Default:** `linear`
- **Description:** How order size affects price

### `slippage.k`
- **Type:** Decimal
- **Default:** `0.0005`
- **Description:** Impact coefficient for slippage calculation
- **Purpose:** Larger orders have bigger market impact

---

## 🏢 Companies Configuration

Configure company/corporation system.

### Basic Company Settings

```yaml
companies:
  enabled: true
  creationCost: 1000.0
  defaultTypes:
    - PRIVATE
    - PUBLIC
    - DAO
```

**Parameters:**

### `enabled`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable company system
- **Impact:** Disabling removes all company commands

### `creationCost`
- **Type:** Decimal
- **Default:** `1000.0`
- **Description:** Cost in dollars to create a company
- **Note:** Set to 0.0 for free company creation

### `defaultTypes`
- **Type:** Array of strings
- **Default:** `[PRIVATE, PUBLIC, DAO]`
- **Description:** Available company types
- **Note:** Remove types to disable them

---

### Default Job Titles

```yaml
companies:
  defaultJobTitles:
    - CEO
    - CFO
    - EMPLOYEE
```

**Parameters:**

### `defaultJobTitles`
- **Type:** Array of strings
- **Default:** `[CEO, CFO, EMPLOYEE]`
- **Description:** Job titles created automatically with company
- **Customization:** Add your own default titles

---

### Job Permissions

```yaml
companies:
  permissionsByTitle:
    CEO:
      canManageCompany: true
      canInvite: true
      canCreateJobTitles: true
      canWithdraw: true
    CFO:
      canWithdraw: true
      canInvite: false
      canCreateJobTitles: false
    EMPLOYEE:
      canInvite: false
      canWithdraw: false
      canCreateJobTitles: false
```

**Available Permissions:**

### `canManageCompany`
- **Description:** Full company management access
- **Allows:** Assign jobs, edit company, all operations

### `canInvite`
- **Description:** Invite new members to company
- **Allows:** Send invitations to players

### `canCreateJobTitles`
- **Description:** Create and edit custom job titles
- **Allows:** Define new roles with permissions

### `canWithdraw`
- **Description:** Withdraw funds from company balance
- **Allows:** Transfer money to personal wallet

**Example Custom Role:**
```yaml
Manager:
  canManageCompany: false
  canInvite: true
  canCreateJobTitles: false
  canWithdraw: true
```

---

## 🎨 Example Configurations

### Relaxed Trading (Casual Server)

```yaml
trading:
  fee:
    percent: 0.10  # Lower fees
  limits:
    perPlayerCooldownMs: 500  # Faster trading
  circuitBreakers:
    enable: false  # No trading halts
```

### Strict Trading (Competitive Server)

```yaml
trading:
  fee:
    percent: 0.50  # Higher fees
  limits:
    maxOrderQty: 1000  # Smaller orders
    perPlayerCooldownMs: 2000  # Slower trading
  circuitBreakers:
    enable: true
    levels: [5, 10, 15]  # Stricter halts
```

### Free Economy (Event Server)

```yaml
companies:
  creationCost: 0.0  # Free companies
trading:
  fee:
    percent: 0.0  # No fees
  limits:
    maxOrderQty: 100000  # Large orders allowed
```

### Realistic Market (RP Server)

```yaml
market:
  updateInterval: 30  # Slower updates
  priceThreshold:
    maxChangePercent: 0.05  # 5% max change
    dampeningFactor: 0.5  # Strong dampening
trading:
  fee:
    percent: 0.25
  circuitBreakers:
    enable: true
    levels: [7, 13, 20]
```

---

## 🔄 Reloading Configuration

After editing config.yml:

**Option 1: Restart Server**
```bash
# Server console
stop
# Then restart
```

**Option 2: Reload Plugin**
```bash
# If supported
/quickstocks reload
# or
/reload confirm
```

**⚠️ Warning:** Some changes require full restart:
- Database configuration
- Feature enable/disable
- Major structural changes

---

## ✅ Configuration Best Practices

### Performance

1. **Update Interval:** Higher values (10-30s) reduce CPU usage
2. **Database:** Use MySQL/PostgreSQL for large servers
3. **History:** Disable if not needed to save space

### Balance

1. **Fees:** 0.1-0.5% is realistic
2. **Creation Cost:** $1000-$5000 prevents spam
3. **Order Limits:** Adjust based on economy size

### Security

1. **Circuit Breakers:** Always enable for stability
2. **Cooldowns:** Prevent trade spam
3. **Price Dampening:** Prevents unrealistic pumps

### User Experience

1. **Default Stocks:** Enable for new servers
2. **Market Device:** Keep recipe disabled (make it special)
3. **Company Types:** Offer all three types

---

## 🆘 Troubleshooting

### Configuration Not Loading

**Problem:** Changes not applied

**Solutions:**
- Check YAML syntax (use validator)
- Restart server completely
- Check logs for errors
- Verify file encoding (UTF-8)

### Invalid Values

**Problem:** Plugin uses defaults

**Solutions:**
- Check value types (number vs string)
- Verify ranges (e.g., 0.0-1.0)
- Check boolean values (true/false, not yes/no)
- Remove comments from values

### Database Connection Fails

**Problem:** Can't connect to MySQL/PostgreSQL

**Solutions:**
- Verify credentials
- Check server is running
- Test connection manually
- Check firewall rules

---

## 🔗 Related Documentation

- **[Installation Guide](Installation.md)** - Initial setup
- **[Database Management](Database.md)** - Database operations
- **[Getting Started](Getting-Started.md)** - Player guide
- **[Commands Overview](Commands-Overview.md)** - All commands

---

*Need help? Check the troubleshooting section or contact support*
