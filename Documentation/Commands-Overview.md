# Commands Overview

This page provides an overview of all QuickStocks commands organized by domain.

## 📚 Command Domains

QuickStocks commands are organized into seven main domains:

| Domain | Command | Description |
|--------|---------|-------------|
| 📊 **Stocks** | `/stocks` | View stock market information and quotes |
| 🪙 **Crypto** | `/crypto` | Create and manage custom cryptocurrencies |
| 💰 **Wallet** | `/wallet` | Manage your in-game wallet balance |
| 📈 **Market** | `/market` | Browse and trade on the market |
| 👀 **Watch** | `/watch` | Manage your stock watchlist |
| 🏢 **Company** | `/company` | Create and manage companies |
| 📱 **Market Device** | `/marketdevice` | Grant Market Link Device items |

---

## 🎯 Quick Reference

### For Players

**Most used commands:**
```
/wallet                    # Check your balance
/market                    # Open market browser
/stocks <SYMBOL>           # View stock details
/watch add <SYMBOL>        # Track a stock
/company settings          # Manage your company
```

### For Administrators

**Admin commands:**
```
/wallet add <amount>              # Add funds to wallet
/wallet set <amount>              # Set wallet balance
/marketdevice give [player]       # Give Market Device
/stocks audit [repair]            # Database maintenance
```

---

## 📖 Detailed Command Documentation

Each domain has its own detailed documentation page with:
- Complete command syntax
- Usage examples
- Permission requirements
- GUI screenshots (where applicable)
- Tips and best practices

### 📊 [Stocks Commands](Commands-Stocks.md)

View and analyze stock market data.

**Main commands:**
- `/stocks` - Top 10 gainers
- `/stocks <SYMBOL>` - Stock details
- `/stocks <material>` - Find by material
- `/stocks audit` - Database integrity

[→ View Full Stocks Documentation](Commands-Stocks.md)

---

### 🪙 [Crypto Commands](Commands-Crypto.md)

Create custom cryptocurrency instruments.

**Main commands:**
- `/crypto create <symbol> <name>` - Create custom crypto

**Aliases:** None

[→ View Full Crypto Documentation](Commands-Crypto.md)

---

### 💰 [Wallet Commands](Commands-Wallet.md)

Manage your in-game wallet balance.

**Main commands:**
- `/wallet` - Check balance
- `/wallet balance` - Check balance
- `/wallet add <amount>` - Add funds (admin)
- `/wallet set <amount>` - Set balance (admin)

**Aliases:** `/money`, `/balance`

[→ View Full Wallet Documentation](Commands-Wallet.md)

---

### 📈 [Market Commands](Commands-Market.md)

Browse and trade instruments and company shares.

**Main commands:**
- `/market` - Open market browser
- `/market buy <company> <quantity>` - Buy shares
- `/market sell <company> <quantity>` - Sell shares
- `/market portfolio` - View holdings
- `/market history` - Trade history
- `/market watchlist` - Watchlist summary

**Aliases:** `/trade`, `/trading`

[→ View Full Market Documentation](Commands-Market.md)

---

### 👀 [Watch Commands](Commands-Watch.md)

Track your favorite stocks and instruments.

**Main commands:**
- `/watch` - View watchlist
- `/watch add <SYMBOL>` - Add to watchlist
- `/watch remove <SYMBOL>` - Remove from watchlist
- `/watch info <SYMBOL>` - Detailed info
- `/watch clear` - Clear watchlist

**Aliases:** `/watchlist`, `/wl`

[→ View Full Watch Documentation](Commands-Watch.md)

---

### 🏢 [Company Commands](Commands-Company.md)

Create and manage companies with employees and shares.

**Main commands:**
- `/company create <name> <type>` - Create company
- `/company settings` - Settings GUI
- `/company info [name]` - Company details
- `/company invite <company> <player> <job>` - Invite employee
- `/company market enable <company>` - Go public (IPO)

**Aliases:** `/corp`, `/corporation`

[→ View Full Company Documentation](Commands-Company.md)

---

### 📱 [Market Device Commands](Commands-MarketDevice.md)

Grant special Market Link Device items to players.

**Main commands:**
- `/marketdevice give [player]` - Give device

**Aliases:** `/mdevice`

[→ View Full Market Device Documentation](Commands-MarketDevice.md)

---

## 🎨 Command Syntax Convention

Throughout this documentation, we use these conventions:

| Syntax | Meaning | Example |
|--------|---------|---------|
| `<required>` | Required parameter | `/stocks <SYMBOL>` |
| `[optional]` | Optional parameter | `/wallet [balance]` |
| `choice1\|choice2` | Choose one option | `/market [browse\|buy\|sell]` |
| `...` | Variable arguments | `/crypto create <symbol> <name...>` |

---

## 🎭 Tab Completion

All QuickStocks commands support **intelligent tab completion**:

- Press `TAB` after typing a command to see available options
- Context-aware suggestions based on your permissions
- Company names, player names, and symbols auto-complete
- Permission-based filtering (only shows what you can access)

**Examples:**
```
/company cr<TAB>           → /company create
/company invite <TAB>      → Shows your company names
/company invite Tech<TAB>  → Shows online players
/watch add <TAB>           → Shows available symbols
```

---

## 🔐 Permission Summary

Most commands are available to all players by default. See the [Permissions](Permissions.md) page for complete details.

**Public Commands (no permission needed):**
- All `/stocks` commands (except audit)
- All `/market` commands (trading)
- All `/watch` commands
- Most `/company` commands (if you're an employee)
- `/wallet` (balance check only)

**Restricted Commands (permission required):**
- `/crypto create` - Requires `maksy.stocks.crypto.create`
- `/wallet add/set` - Requires `quickstocks.wallet.add` or `quickstocks.wallet.set`
- `/marketdevice give` - Requires `maksy.stocks.marketdevice.give`
- `/stocks audit` - Requires `quickstocks.admin.audit`

---

## 💡 Tips for Using Commands

### For New Players

1. **Start with help commands** - Most commands show help when run without arguments
2. **Use tab completion** - Press TAB to see available options
3. **Check permissions** - Commands will tell you if you lack permissions
4. **Explore GUIs** - Many commands open helpful interfaces

### For Experienced Players

1. **Use aliases** - Many commands have shorter aliases (e.g., `/wl` for `/watch`)
2. **Chain operations** - Use the Market GUI for faster trading
3. **Leverage watchlists** - Track multiple instruments at once
4. **Master company management** - Use `/company settings` GUI for efficiency

### For Administrators

1. **Use audit tools** - Regularly check `/stocks audit` for database health
2. **Monitor wallets** - Use `/wallet set` for economy management
3. **Configure permissions** - Fine-tune access with permission plugins
4. **Read config.yml** - Many features are configurable

---

## 🆘 Getting Help

**In-game help:**
- Run any command without arguments to see usage
- Hover over GUI items for tooltips
- Use `/company` or `/market` for command lists

**Documentation:**
- [Getting Started](Getting-Started.md) - New player guide
- [Permissions](Permissions.md) - Permission reference
- Individual command pages (linked above)

**Troubleshooting:**
- Check you have required permissions
- Verify you're using correct syntax
- Ensure you have sufficient funds
- Ask server administrators for help

---

## 🔗 Next Steps

Choose a command domain to learn more:

- **[📊 Stocks Commands](Commands-Stocks.md)** - Market data and analysis
- **[🪙 Crypto Commands](Commands-Crypto.md)** - Custom cryptocurrencies
- **[💰 Wallet Commands](Commands-Wallet.md)** - Balance management
- **[📈 Market Commands](Commands-Market.md)** - Trading interface
- **[👀 Watch Commands](Commands-Watch.md)** - Watchlist management
- **[🏢 Company Commands](Commands-Company.md)** - Company operations
- **[📱 Market Device Commands](Commands-MarketDevice.md)** - Special items

---

*For server configuration, see [Configuration](Configuration.md)*
