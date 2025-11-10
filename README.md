# QuickStocks

> A comprehensive stock market and company management plugin for Minecraft servers

Transform your Minecraft server into a thriving economic ecosystem where players trade stocks, create companies, and build financial empires—all within the game they love.

[![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-blue)](https://github.com/Cybernetic-Forge/QuickStocks)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.8-green)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/java-17%2B-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)]()

---

## 🎮 What is QuickStocks?

QuickStocks brings real-world financial markets to Minecraft. Players can:

- 📊 **Trade on a realistic stock market** with dynamic pricing and 25+ market factors
- 🏢 **Create and manage companies** with employees, roles, and shared finances
- 💹 **Go public with IPOs** and trade company shares on the market
- 🪙 **Create custom cryptocurrencies** with permission-based controls
- 📈 **Build investment portfolios** and track performance in real-time
- 💰 **Manage wallets** integrated with Vault economy
- 👀 **Use watchlists** to monitor favorite instruments
- 📱 **Access markets anywhere** with the Market Link Device

---

## ✨ Key Features

### 📊 Advanced Market Simulation
- **Real-time price updates** every 5 seconds with realistic volatility
- **Market factors** including inflation, interest rates, sector performance, and global events
- **Circuit breakers** to halt trading during extreme movements
- **Trading fees** and slippage for realistic market mechanics
- **Price history** tracking with analytics and trend analysis

### 🏢 Company Management System
- **Create companies** (PRIVATE, PUBLIC, or DAO) with customizable roles
- **Employee management** with job titles and permission-based access
- **Shared finances** with deposit/withdraw capabilities
- **Go public** and sell shares on the stock market
- **Buyout protection** to prevent hostile takeovers
- **Shareholder tracking** with real-time ownership percentages

### 💰 Economy Integration
- **Vault integration** for seamless economy plugin support
- **Wallet system** with built-in balance management
- **Trading fees** (default 0.25%) to prevent market manipulation
- **Configurable costs** for company creation and operations

### 🔌 Plugin Integrations
- **ChestShop integration** - Companies can own and manage chest shops
  - Employee-based shop management with permission system
  - Shop revenues and purchases tied to company balance
  - Automatic balance validation for transactions
  - Fully optional soft-dependency (works without ChestShop)
- **WorldGuard integration** - Region-based permission control
  - Custom flags for plot purchases, trading, and shop placement
  - `quickstocks-plots` flag to control plot purchases in regions
  - Graceful degradation when WorldGuard not installed
  - Fully optional soft-dependency (works without WorldGuard)

### 🎨 Beautiful User Interface
- **Interactive market GUI** for browsing and trading
- **Company settings GUI** with permission-aware buttons
- **Color-coded displays** for profits, losses, and performance
- **Helpful tooltips** and command hints throughout
- **Tab completion** for all commands with context awareness

---

## 🚀 Quick Start

### For Players

```bash
/wallet                    # Check your balance
/market                    # Browse tradeable instruments
/stocks                    # See top 10 gainers
/company create TechCorp PUBLIC    # Start your company
/watch add TECH            # Track your investments
```

### For Server Administrators

1. **Download** the latest QuickStocks JAR
2. **Place** in your server's `plugins/` folder
3. **Restart** your server
4. **Configure** (optional) in `plugins/QuickStocks/config.yml`
5. **Done!** Players can start trading immediately

📖 **[Full Installation Guide →](Documentation/Installation.md)**

---

## 📚 Documentation

### 🎯 For Players
- **[Getting Started](Documentation/Getting-Started.md)** - Learn the basics and start trading
- **[Commands Overview](Documentation/Commands-Overview.md)** - All available commands
- **[Permissions](Documentation/Permissions.md)** - Permission system explained

### 🎮 For Server Administrators
- **[Installation](Documentation/Installation.md)** - Step-by-step setup guide
- **[Configuration](Documentation/Configuration.md)** - Detailed configuration options
- **[Database Management](Documentation/Database.md)** - Database administration
- **[Build Obfuscation](Documentation/BUILD_OBFUSCATION.md)** - Code protection for releases
- **[ChestShop Integration](Documentation/ChestShop-Integration.md)** - Company-owned chest shops
- **[WorldGuard Integration](Documentation/WORLDGUARD_INTEGRATION.md)** - Region-based permissions and custom flags

### 📖 Command References
- **[Stocks Commands](Documentation/Commands-Stocks.md)** - Market data and analysis
- **[Market Commands](Documentation/Commands-Market.md)** - Trading interface
- **[Company Commands](Documentation/Commands-Company.md)** - Company management
- **[Crypto Commands](Documentation/Commands-Crypto.md)** - Cryptocurrency creation
- **[Wallet Commands](Documentation/Commands-Wallet.md)** - Balance management
- **[Watch Commands](Documentation/Commands-Watch.md)** - Watchlist management
- **[Market Device](Documentation/Commands-MarketDevice.md)** - Portable market access

---

## 🎯 Use Cases

### 🏫 Educational Servers
Teach economics, finance, and business management through gameplay

### 🎭 Roleplay Servers
Create immersive economic systems for your roleplay world

### ⚔️ Competition Servers
Host trading competitions and economic challenges

### 🎪 Event Servers
Run special market events and company competitions

---

## 🔧 Technical Specifications

- **Server Software:** Paper 1.21.8+ / Spigot / Bukkit
- **Java Version:** 17 or higher
- **Database:** SQLite (included), MySQL, or PostgreSQL
- **Dependencies:** None required (Vault optional for economy integration)
- **Performance:** Optimized for servers of all sizes with connection pooling
- **Architecture:** Clean architecture with IoC pattern and service layers

---

## 💡 Example Scenarios

### Trading Stocks
```bash
# Check market overview
/stocks                        # Top 10 gainers

# Research an instrument
/stocks MINE                   # MineCorp Industries details

# Add to watchlist
/watch add MINE

# Make a purchase
/market buy MINE 100           # Buy 100 shares

# Check your portfolio
/market portfolio              # View holdings and P&L
```

### Running a Company
```bash
# Create your company
/company create TechCorp PUBLIC

# Deposit funds
/company deposit TechCorp 15000

# Invite employees
/company invite TechCorp Alex CFO

# Set trading symbol and go public
/company setsymbol TechCorp TECH
/company market enable TechCorp

# Monitor shareholders
/market shareholders TECH
```

---

## 🎨 Screenshots

<!-- [GUI Screenshot Placeholder: Market Browser Interface] -->
*Market browser with live prices and trading options*

<!-- [GUI Screenshot Placeholder: Company Settings GUI] -->
*Company settings with permission-aware action buttons*

<!-- [GUI Screenshot Placeholder: Portfolio Display] -->
*Portfolio view with P&L calculations and performance metrics*

---

---

## 🔨 Building from Source

Want to compile QuickStocks yourself?

```bash
# Clone the repository
git clone https://github.com/Cybernetic-Forge/QuickStocks.git
cd QuickStocks

# Development build (no obfuscation) - NOT FOR PRODUCTION
mvn clean package

# Production build (with obfuscation) - REQUIRED FOR RELEASES
mvn clean package -Dobfuscate.enabled=true
```

> **⚠️ IMPORTANT**: For production releases, you **MUST** use `-Dobfuscate.enabled=true` to protect your code from decompilation. Without this flag, the code is NOT obfuscated and can be easily reverse-engineered.

The compiled JAR will be in `target/QuickStocks-1.0.0-SNAPSHOT.jar`

For more details on obfuscation and advanced build options, see the [Build Obfuscation Guide](Documentation/BUILD_OBFUSCATION.md).

---

## 🤝 Contributing

We welcome contributions! Whether it's:
- 🐛 Bug reports
- ✨ Feature requests
- 📖 Documentation improvements
- 💻 Code contributions

Please check our contributing guidelines and open an issue or pull request.

---

## 📜 License

QuickStocks is released under the MIT License. See LICENSE file for details.

---

## 🔗 Links

- **[Full Documentation](Documentation/Getting-Started.md)** - Complete user and admin guides
- **[GitHub Issues](https://github.com/Cybernetic-Forge/QuickStocks/issues)** - Report bugs and request features
- **[bStats Statistics](https://bstats.org/plugin/bukkit/QuickStocks/24106)** - View anonymous usage statistics
- **[Discord](https://discord.gg/your-invite)** - Get support and discuss with community

---

## 📊 Anonymous Statistics

QuickStocks collects anonymous usage statistics through [bStats](https://bstats.org/) to help us understand how the plugin is used and improve it. This data includes server counts, plugin versions, and aggregate usage metrics. No personal or player information is collected.

You can view our public statistics at: https://bstats.org/plugin/bukkit/QuickStocks/24106

To opt-out, set `metrics.enabled: false` in `config.yml` or disable bStats globally in `plugins/bStats/config.yml`.

---

## 🙏 Credits

Developed with ❤️ by the Cybernetic Forge team

- **Architecture:** Clean architecture with IoC patterns
- **Database:** Multi-provider support with automatic migrations
- **UI/UX:** Adventure Components for beautiful formatting
- **Testing:** Comprehensive test suite with MockBukkit (78 test cases)
- **CI/CD:** Automated build pipeline with test validation

---

## 🧪 Testing

QuickStocks includes a comprehensive automated test suite:

- **78 test cases** covering core business logic
- **MockBukkit** integration for Bukkit API mocking
- **JUnit 5** test framework
- **GitHub Actions** CI/CD pipeline
- **Test Coverage**: Fee calculations, wallet operations, trading logic, company management, portfolio operations

See [TEST_SUITE.md](Documentation/TEST_SUITE.md) for detailed documentation.

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=WalletServiceTest
```

---