# Installation Guide

This guide walks you through installing QuickStocks on your Minecraft server.

## 📋 Requirements

### Server Requirements

- **Server Software:** Paper, Spigot, or Bukkit
- **Minecraft Version:** 1.21.8 or higher
- **Java Version:** Java 17 or higher
- **RAM:** Minimum 2GB allocated (4GB+ recommended)
- **Database:** SQLite (included) or MySQL/PostgreSQL (optional)

### Optional Dependencies

- **Vault** - For economy integration (recommended)
- **LuckPerms** - For advanced permission management (recommended)
- **PlaceholderAPI** - For placeholder support (optional)

---

## 📥 Step 1: Download the Plugin

1. **Download** the latest QuickStocks JAR file
2. **Verify** the file is complete (check file size)
3. **Backup** your server before installing

**File Name:** `QuickStocks-1.0.0-SNAPSHOT.jar`

---

## 📁 Step 2: Install the Plugin

### Basic Installation

1. **Stop your server** (if running)
   ```bash
   # In server console
   stop
   ```

2. **Place JAR file** in the plugins folder
   ```bash
   # Copy to plugins directory
   cp QuickStocks-1.0.0-SNAPSHOT.jar /path/to/server/plugins/
   ```

3. **Set permissions** (if on Linux/Mac)
   ```bash
   chmod 644 plugins/QuickStocks-1.0.0-SNAPSHOT.jar
   ```

4. **Start your server**
   ```bash
   # Start server
   ./start.sh
   # or
   java -Xms2G -Xmx4G -jar paper.jar nogui
   ```

5. **Verify installation** in console
   ```
   [QuickStocks] Enabling QuickStocks v1.0.0-SNAPSHOT
   [QuickStocks] Database initialized successfully
   [QuickStocks] Loaded X instruments
   [QuickStocks] Market simulation started
   ```

---

## ⚙️ Step 3: Initial Configuration

### First-Time Setup

After first run, QuickStocks creates these files:

```
plugins/QuickStocks/
├── config.yml              # Main configuration
├── Translations.yml        # Translation strings
├── data.db                 # SQLite database (if using SQLite)
└── migrations/             # Database schema files
```

### Basic Configuration

Edit `plugins/QuickStocks/config.yml`:

```yaml
# Database Configuration
database:
  provider: sqlite         # sqlite | mysql | postgres
  sqlite:
    file: plugins/QuickStocks/data.db

# Market Configuration
market:
  updateInterval: 5        # seconds
  startOpen: true
  defaultStocks: true      # Seed default instruments

# Companies Configuration
companies:
  enabled: true
  creationCost: 1000.0    # Cost to create company
```

**Save and reload:**
```bash
# In server console
reload confirm
# or
quickstocks reload
```

---

## 🗄️ Step 4: Database Setup

### Option A: SQLite (Default)

**Recommended for:** Small to medium servers (< 100 players)

**Advantages:**
- No setup required
- Works out of the box
- Single file database

**Configuration:**
```yaml
database:
  provider: sqlite
  sqlite:
    file: plugins/QuickStocks/data.db
```

**No additional steps needed!**

---

### Option B: MySQL

**Recommended for:** Large servers or multiple server instances

**Advantages:**
- Better performance at scale
- Shared across multiple servers
- Professional backup solutions

**Prerequisites:**
1. MySQL 5.7+ or MariaDB 10.2+ installed
2. Database created
3. User with permissions

**Setup Steps:**

1. **Create database and user**
   ```sql
   CREATE DATABASE quickstocks;
   CREATE USER 'quickstocks'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON quickstocks.* TO 'quickstocks'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. **Configure QuickStocks**
   ```yaml
   database:
     provider: mysql
     mysql:
       host: localhost
       port: 3306
       database: quickstocks
       user: quickstocks
       password: your_password
       useSSL: false
   ```

3. **Restart server** - Tables will be created automatically

---

### Option C: PostgreSQL

**Recommended for:** Enterprise deployments

**Advantages:**
- Advanced features
- Excellent concurrent performance
- Robust data integrity

**Prerequisites:**
1. PostgreSQL 12+ installed
2. Database created
3. User with permissions

**Setup Steps:**

1. **Create database and user**
   ```sql
   CREATE DATABASE quickstocks;
   CREATE USER quickstocks WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE quickstocks TO quickstocks;
   ```

2. **Configure QuickStocks**
   ```yaml
   database:
     provider: postgres
     postgres:
       host: localhost
       port: 5432
       database: quickstocks
       user: quickstocks
       password: your_password
   ```

3. **Restart server** - Tables will be created automatically

---

## 🔐 Step 5: Permission Setup

### Using LuckPerms (Recommended)

1. **Install LuckPerms** if not already installed

2. **Create permission groups**
   ```bash
   # Default permissions (all players)
   lp group default permission set quickstocks.company.create true
   lp group default permission set quickstocks.company.manage true

   # VIP permissions
   lp group vip permission set maksy.stocks.crypto.create true

   # Admin permissions
   lp group admin permission set maksy.stocks.marketdevice.give true
   lp group admin permission set quickstocks.wallet.add true
   lp group admin permission set quickstocks.wallet.set true
   lp group admin permission set quickstocks.admin.audit true
   ```

3. **Verify permissions**
   ```bash
   lp user PlayerName permission info
   ```

### Using PermissionsEx

```yaml
# permissions.yml
groups:
  default:
    permissions:
      - quickstocks.company.create
      - quickstocks.company.manage
  
  vip:
    permissions:
      - maksy.stocks.crypto.create
  
  admin:
    permissions:
      - maksy.stocks.marketdevice.give
      - quickstocks.wallet.add
      - quickstocks.wallet.set
      - quickstocks.admin.audit
```

---

## 🎮 Step 6: Test the Installation

### Basic Functionality Test

1. **Join your server** as a player

2. **Test wallet**
   ```
   /wallet
   ```
   Should show your balance

3. **Test market**
   ```
   /market
   ```
   Should open market GUI

4. **Test stocks**
   ```
   /stocks
   ```
   Should show top gainers

5. **Test company creation**
   ```
   /company create TestCorp PUBLIC
   ```
   Should create a company (if you have funds)

### Admin Testing

1. **Test admin commands** (as operator)
   ```
   /wallet set 100000
   /stocks audit
   /marketdevice give
   ```

2. **Check database**
   ```
   /stocks audit
   ```
   Should show database status

3. **Check logs** for errors
   ```bash
   tail -f logs/latest.log | grep QuickStocks
   ```

---

## 🎨 Step 7: Customization

### Economy Integration

If using Vault:
1. Install Vault plugin
2. Install economy plugin (e.g., EssentialsX)
3. QuickStocks will automatically detect and use it

### Language Customization

Edit `plugins/QuickStocks/Translations.yml`:

```yaml
# Example translations
commands:
  wallet:
    balance: "&a💰 Your Wallet Balance: &f$%balance%"
  market:
    no_funds: "&cInsufficient funds!"
  company:
    created: "&a✅ Company created: &f%name%"
```

### Market Configuration

Fine-tune market behavior:

```yaml
market:
  updateInterval: 5              # Price update frequency
  
  priceThreshold:
    enabled: true
    maxChangePercent: 0.15       # 15% max change per update
    dampeningFactor: 0.3         # Dampen large changes
  
trading:
  fee:
    mode: percent                # Fee structure
    percent: 0.25                # 0.25% trading fee
  
  limits:
    maxOrderQty: 10000           # Max shares per order
    perPlayerCooldownMs: 750     # Cooldown between trades
```

---

## ✅ Post-Installation Checklist

- [ ] Plugin loads without errors
- [ ] Database initializes successfully
- [ ] Players can use `/wallet` command
- [ ] Players can open `/market` GUI
- [ ] `/stocks` shows instrument data
- [ ] Companies can be created
- [ ] Permissions work correctly
- [ ] No console errors
- [ ] Database is being updated
- [ ] Market simulation is running

---

## 🔄 Updating QuickStocks

### Update Process

1. **Backup your database**
   ```bash
   # For SQLite
   cp plugins/QuickStocks/data.db plugins/QuickStocks/data.db.backup
   
   # For MySQL
   mysqldump -u quickstocks -p quickstocks > quickstocks_backup.sql
   ```

2. **Stop the server**
   ```
   stop
   ```

3. **Replace the JAR file**
   ```bash
   rm plugins/QuickStocks-*.jar
   cp QuickStocks-NEW-VERSION.jar plugins/
   ```

4. **Start the server**
   ```bash
   ./start.sh
   ```

5. **Check for migrations**
   - Plugin will automatically run database migrations
   - Check console for migration messages
   - Verify with `/stocks audit`

---

## 🆘 Troubleshooting Installation

### Plugin Not Loading

**Error:** `Could not load 'plugins/QuickStocks.jar'`

**Solutions:**
- Check Java version (needs 17+)
- Verify file isn't corrupted
- Check file permissions
- Ensure server software is compatible

### Database Connection Errors

**Error:** `Failed to connect to database`

**Solutions:**
- Verify database credentials
- Check database server is running
- Ensure firewall allows connections
- Test database login manually

### Permission Errors

**Error:** `You don't have permission...`

**Solutions:**
- Check permission plugin is installed
- Verify permission nodes are correct
- Give player OP temporarily to test
- Check group inheritance

### Market Not Updating

**Error:** No prices changing

**Solutions:**
- Check `market.updateInterval` in config
- Verify no errors in console
- Run `/stocks audit` to check data
- Restart server

---

## 🔗 Next Steps

After installation, check out:

- **[Configuration Guide](Configuration.md)** - Detailed configuration options
- **[Getting Started](Getting-Started.md)** - Learn to use the plugin
- **[Commands Overview](Commands-Overview.md)** - All available commands
- **[Database Management](Database.md)** - Database administration

---

## 📚 Additional Resources

### Server Console Commands

```bash
# Reload configuration
quickstocks reload

# Check version
quickstocks version

# Database audit
quickstocks audit
```

### Useful Links

- **GitHub Issues:** Report bugs and request features
- **Discord:** Get community support
- **Wiki:** Full documentation

---

*For detailed configuration options, see [Configuration Guide](Configuration.md)*
