# 📈 QuickStocks

> **The Ultimate Stock Market & Company Management Plugin for Minecraft**

Transform your Minecraft server into a sophisticated economic powerhouse. QuickStocks brings **Wall Street to your world** with realistic market simulation, company management, and financial trading—all seamlessly integrated into Minecraft.

[![Version](https://img.shields.io/badge/version-1.0.0--SNAPSHOT-blue)](https://github.com/Cybernetic-Forge/QuickStocks)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.8-green)](https://www.spigotmc.org/)
[![Java](https://img.shields.io/badge/java-21%2B-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey)](https://opensource.org/licenses/MIT)

---

## 🎮 What Makes QuickStocks Special?

QuickStocks isn't just another economy plugin—it's a **fully-featured financial market simulator** that rivals real-world trading platforms. With sophisticated algorithms, realistic market behavior, and enterprise-grade architecture, it offers an unmatched economic experience.

### 💎 For Players
- 📊 **Trade Like a Pro** - Real-time market with dynamic pricing influenced by 25+ economic factors
- 🏢 **Build Business Empires** - Create companies, hire employees, manage finances, and expand with plots
- 💹 **Launch Your IPO** - Take your company public and watch players invest in your vision
- 🪙 **Create Cryptocurrencies** - Mint your own crypto (personal or company-branded)
- 📈 **Master Investing** - Build diversified portfolios, use watchlists, track performance metrics
- 💰 **Seamless Economy** - Integrated wallet system with Vault support
- 📱 **Trade Anywhere** - Market Link Device for on-the-go transactions

### ⚙️ For Server Administrators
- 🚀 **Plug & Play** - Works out of the box with sensible defaults
- 🗄️ **Enterprise Database** - SQLite, MySQL, or PostgreSQL with automatic migrations
- 🔌 **Smart Integrations** - Optional Vault, ChestShop, and WorldGuard support
- 🎨 **Beautiful GUIs** - Professional interfaces with full customization
- 📊 **Performance** - Optimized for servers of all sizes with connection pooling
- 🔧 **Highly Configurable** - Multi-file config system for fine-tuned control

---

## 🌟 Why Choose QuickStocks?

### 🎯 Realistic Market Simulation
Not just random price changes—QuickStocks uses **sophisticated algorithms** that consider:
- Economic indicators (inflation, interest rates, GDP, unemployment)
- Market sentiment (fear/greed index, investor confidence)
- Technical analysis (moving averages, support/resistance)
- Industry trends and sector performance
- Global events and breaking news
- Supply/demand dynamics with slippage
- **Circuit breakers** to prevent market crashes
- **Trading fees** and realistic market mechanics

---

## ✨ Core Features

### 📊 Advanced Market Simulation
The heart of QuickStocks—a **sophisticated trading engine** that behaves like real markets:

- **Real-time Price Updates** - Every 5 seconds with realistic volatility and momentum
- **Smart Price Algorithm** - 25+ market factors including:
  - Economic indicators (inflation, interest rates, GDP, unemployment)
  - Market sentiment (fear/greed index, investor confidence, media buzz)
  - Technical analysis (moving averages, support/resistance, momentum)
  - Industry trends (sector performance, commodity prices, regulations)
  - Global events (geopolitical changes, natural disasters, pandemics)
  - Random events (flash crashes, social media trends, market manipulation)
- **Circuit Breakers** - Automatic trading halts during extreme movements (±10%)
- **Trading Fees & Slippage** - Realistic transaction costs (0.25% default) and price impact on large orders
- **Market Analytics** - Price history, trend analysis, top gainers/losers, sector performance
- **Multiple Instrument Types** - Stocks, cryptocurrencies, Minecraft items, company shares, indices, funds

### 🏢 Comprehensive Company Management
Run your business empire with **professional-grade tools**:

- **Company Types** - PRIVATE, PUBLIC, or DAO with distinct governance models
- **Employee System** - Hire players, assign roles (CEO, CFO, custom titles), manage permissions
- **Financial Operations** - Shared company treasury, deposits, withdrawals, transaction history
- **Plot System** - Buy and manage land plots (chunks) for your company
- **Salary System** - Automated employee payments with configurable schedules
- **IPO Launch** - Go public on the stock market, set share prices, manage shareholders
- **Buyout Protection** - Prevent hostile takeovers with ownership safeguards
- **Invitation System** - 7-day expiring invites with acceptance/decline
- **Company Settings GUI** - Professional interface for all management tasks
- **ChestShop Integration** - Company-owned shops with employee permissions

### 💰 Cryptocurrency System
Create and trade digital assets:

- **Default Cryptocurrencies** - Pre-seeded popular cryptos (Bitcoin, Ethereum, Dogecoin, etc.)
- **Personal Crypto** - Create your own cryptocurrency ($500k requirement)
- **Company Crypto** - Companies can mint branded tokens
- **Full Trading Support** - Cryptos trade like any other instrument
- **Symbol Validation** - Prevents conflicts and ensures uniqueness
- **Market Integration** - Real-time prices, history, analytics

### 💼 Portfolio & Wallet Management
Track your wealth with precision:

- **Vault Integration** - Seamless connection to economy plugins (optional)
- **Built-in Wallet** - Standalone wallet system if Vault unavailable
- **Holdings Tracking** - Real-time portfolio with P&L calculations
- **Performance Metrics** - Total value, unrealized gains/losses, ROI percentages
- **Watchlist System** - Monitor favorite instruments without buying
- **Transaction History** - Complete audit trail of all trades
- **Query Service** - Advanced analytics and reporting

### 🔌 Smart Plugin Integrations
Optional but powerful—all soft dependencies with graceful degradation:

- **Vault Economy** 
  - Automatic detection and integration
  - Falls back to built-in wallet if unavailable
  - Dual-mode operation for maximum compatibility

- **ChestShop Integration**
  - Company-owned chest shops
  - Employee-based management with permission system
  - Shop revenue/costs tied to company balance
  - Automatic transaction validation
  - Works perfectly without ChestShop installed

- **WorldGuard Integration**
  - Region-based permission control
  - Custom flags: `quickstocks-plots` for plot purchases
  - Protection for company-owned land
  - Graceful degradation without WorldGuard

### 🎨 Beautiful User Interfaces
Professional GUIs that make complex operations simple:

- **Market Browser GUI** - Browse instruments, view live prices, execute trades
- **Company Settings GUI** - Permission-aware management interface
- **Plot Management GUI** - Visual plot editor for company lands
- **Color-Coded Displays** - Green for profits, red for losses, intuitive indicators
- **Helpful Tooltips** - Context-sensitive help on every item
- **Tab Completion** - Smart command completion for all commands
- **Pagination Support** - Handle large datasets smoothly
- **Configurable Layouts** - Full customization via `guis.yml`

---

## 🚀 Quick Start

### 🎮 For Players - Start Trading in Seconds

```bash
# Check your wallet balance
/wallet

# Browse the market
/market                          # Interactive GUI
/stocks                          # Top 10 gainers

# Research and track instruments
/stocks DIAMOND                  # View detailed info
/watch add DIAMOND               # Add to watchlist

# Make your first trade
/market buy DIAMOND 100          # Buy 100 shares

# View your portfolio
/market portfolio                # Holdings and P&L

# Create a company
/company create MyCorp PUBLIC    # Start your business

# Go public and trade shares
/company setsymbol MyCorp MCORP
/company market enable MyCorp

# Create cryptocurrency (requires $500k)
/crypto create MYCOIN "My Coin"
```a

### ⚙️ For Server Administrators - 5-Minute Setup

1. **Download** the latest QuickStocks JAR from [Releases](https://github.com/Cybernetic-Forge/QuickStocks/releases)
2. **Place** in your server's `plugins/` folder
3. **Start/Restart** your server
4. **Configure** (optional) in `plugins/QuickStocks/`
   - `config.yml` - Database, logging, feature toggles
   - `market.yml` - Trading economy, circuit breakers, analytics
   - `companies.yml` - Company types, costs, permissions
   - `guis.yml` - GUI layouts and appearance
5. **Add Dependencies** (optional):
   - Vault (for economy integration)
   - ChestShop (for company shops)
   - WorldGuard (for plot protection)
6. **Done!** Players can start trading immediately with sensible defaults

📖 **[Full Installation Guide →](Documentation/Installation.md)**

#### Default Configuration Highlights
- ✅ SQLite database (no setup required)
- ✅ Market updates every 5 seconds
- ✅ 0.25% trading fee
- ✅ $1,000 company creation cost
- ✅ Circuit breakers at ±10% price movement
- ✅ All features enabled by default

---

## 🎯 Use Cases & Server Types

### 🏫 Educational Servers
Perfect for teaching economics and finance:
- Real-world market simulation for hands-on learning
- Company management teaches business principles
- Safe environment to learn trading without real money
- Great for economics courses and financial literacy programs

### 🎭 Roleplay Servers
Enhance immersion with realistic economy:
- Players can roleplay as CEOs, traders, employees
- Company system supports business roleplay scenarios
- Realistic financial transactions and investments
- Plot system for company headquarters and offices

### ⚔️ Competition & Event Servers
Host exciting economic challenges:
- Trading competitions with leaderboards
- Company growth competitions
- "Rags to riches" challenges
- Stock market prediction games
- Economic warfare between factions

### 🏰 Survival & Towny Servers
Add economic depth to gameplay:
- Trade Minecraft items on the market
- Town economies integrated with companies
- Player-driven market prices
- Corporate alliances and competitions

### 🎪 Mini-Game & Hub Servers
Create unique economy-focused games:
- Stock trading mini-games
- Company building challenges
- Economic simulation games
- Financial literacy challenges

---

## 💡 Real-World Examples

### 📈 Trading Stocks & Items
```bash
# Morning routine: Check the markets
/stocks                          # View top movers
/watch list                      # Check watchlist

# Research a specific item
/stocks DIAMOND                  # DIAMOND: $450.25 (+5.3%)
                                 # Volume: 15,234 | 24h Change: +$22.75
                                 # Market Cap: $68.5M

# Technical analysis
/stocks DIAMOND                  # Shows moving averages, support/resistance

# Execute trades
/market buy DIAMOND 100 450      # Buy 100 @ max $450
/market sell DIAMOND 50          # Sell 50 at market price

# Review portfolio
/market portfolio                # Total Value: $125,432.50
                                 # Unrealized P&L: +$5,234.00 (+4.35%)

# Check transaction history
/market history                  # Last 10 trades with P&L
```

### 🏢 Running a Company
```bash
# Start your business empire
/company create TechCorp PUBLIC  # Cost: $1,000

# Build your team
/company invite TechCorp Alice CFO
/company invite TechCorp Bob DEVELOPER

# Alice accepts the invitation
/company invitations             # View pending
/company accept 1                # Accept invitation #1

# Manage company finances
/company deposit TechCorp 15000  # Invest in the business
/company info TechCorp           # Balance: $15,000
                                 # Employees: 3 (CEO, CFO, DEVELOPER)

# Expand with real estate
/company plot buy TechCorp       # Buy current chunk
/company plot list TechCorp      # View owned plots

# Create custom roles
/company createjob TechCorp "Lead Engineer" MANAGE_PLOTS,DEPOSIT

# Go public on the market
/company setsymbol TechCorp TECH
/company market enable TechCorp  # IPO Launch!

# Monitor your stock
/market shareholders TECH        # See who owns shares
/stocks TECH                     # Current price and performance

# Company profits can be distributed
# Shareholders can trade TECH like any stock
/market buy TECH 500             # Buy 500 shares of TechCorp
```

### 💰 Creating Cryptocurrency
```bash
# Personal cryptocurrency (requires $500,000 balance)
/wallet                          # Check balance: $500,000+
/crypto create MYCOIN "MyCustomCoin"
                                 # MYCOIN created successfully!
                                 # Initial supply: 1,000,000 MYCOIN

# Company cryptocurrency (requires company balance)
/company info TechCorp           # Balance: $500,000+
/crypto company TechCorp TECHCOIN "TechCorp Token"
                                 # TECHCOIN created for TechCorp!

# Trade your crypto
/stocks MYCOIN                   # View market data
/market buy MYCOIN 1000          # Buy 1000 coins
/market sell MYCOIN 500          # Sell 500 coins

# Watch it grow
/watch add MYCOIN                # Track performance
/watch add TECHCOIN

# Crypto behaves like any tradeable instrument
# Subject to market forces, fees, and slippage
```

### 🔍 Advanced Features
```bash
# Set up watchlist for portfolio tracking
/watch add DIAMOND
/watch add EMERALD
/watch add TECH
/watch list                      # Quick overview of all tracked instruments

# Use Market Link Device
/marketdevice                    # Get portable market access
# Right-click device anywhere    → Opens market GUI

# Admin operations (requires permissions)
/wallet add PlayerName 10000     # Add balance
/wallet set PlayerName 50000     # Set balance
/wallet remove PlayerName 1000   # Remove balance

# Company administration
/company employees TechCorp      # List all employees
/company jobs TechCorp           # View job titles and permissions
/company kick TechCorp Bob       # Remove employee
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

## 🤝 Contributing & Support

### 🐛 Found a Bug?
[Open an issue](https://github.com/Cybernetic-Forge/QuickStocks/issues) with:
- Clear description of the problem
- Steps to reproduce
- Expected vs actual behavior
- Server version, Java version, plugin version
- Any error messages or logs

### ✨ Have a Feature Request?
We love new ideas! [Submit a feature request](https://github.com/Cybernetic-Forge/QuickStocks/issues) with:
- Clear description of the feature
- Use case and benefits
- How it fits with existing features

### 💻 Want to Contribute Code?
Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes following our code standards
4. Write tests for new functionality
5. Submit a pull request

### 💬 Need Help?
- **[GitHub Issues](https://github.com/Cybernetic-Forge/QuickStocks/issues)** - Bug reports and feature requests
- **[Discord](https://discord.gg/Sek4PRBf)** - Real-time chat and support
- **[Documentation](Documentation/Getting-Started.md)** - Comprehensive guides

---

## 🔗 Quick Links

### Documentation
- 📖 **[Getting Started Guide](Documentation/Getting-Started.md)** - Complete player tutorial
- 🎮 **[Commands Overview](Documentation/Commands-Overview.md)** - All available commands
- ⚙️ **[Installation Guide](Documentation/Installation.md)** - Server setup instructions
- 🔧 **[Configuration Guide](Documentation/Configuration.md)** - Detailed settings reference
- 📊 **[Database Guide](Documentation/Database.md)** - Database administration
- 🧪 **[Test Suite](Documentation/TEST_SUITE.md)** - Testing documentation

### Community & Support
- 🐛 **[Issue Tracker](https://github.com/Cybernetic-Forge/QuickStocks/issues)** - Bug reports and features
- 📊 **[bStats Dashboard](https://bstats.org/plugin/bukkit/QuickStocks/24106)** - Usage statistics
- 💭 **[Discord Server](https://discord.gg/your-invite)** - Real-time chat support

### Developer Resources
- 🔌 **[API Documentation](src/main/java/net/cyberneticforge/quickstocks/api/README.md)** - Event system and managers
- 📋 **[Contributing Guide](Documentation/CONTRIBUTING_TESTS.md)** - How to contribute
- 🏗️ **[Architecture Overview](.github/copilot/features/README.md)** - System design docs
- 📝 **[Code Quality Analysis](Documentation/CODE_QUALITY_ANALYSIS.md)** - Quality metrics

---

<div align="center">

**⭐ Star this repository if you find QuickStocks useful!**

Made with ❤️ for the Minecraft community

</div>
