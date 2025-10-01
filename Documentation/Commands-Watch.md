# Watch Commands

The `/watch` command manages your personal watchlist for tracking favorite stocks, cryptocurrencies, and company shares.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/watch` | View your watchlist | None |
| `/watch add <SYMBOL>` | Add instrument to watchlist | None |
| `/watch remove <SYMBOL>` | Remove from watchlist | None |
| `/watch info <SYMBOL>` | Detailed watchlist item info | None |
| `/watch list` | View your watchlist | None |
| `/watch clear` | Clear entire watchlist | None |

**Aliases:** `/watchlist`, `/wl`

---

## 📊 Viewing Your Watchlist

### `/watch` or `/watch list`

Displays all instruments in your watchlist with current prices and 24h changes.

**Permission:** None (public)

**Example:**
```
/watch
/watch list
/wl         # Using alias
```

**Sample Output:**
```
👀 Your Watchlist
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SYMBOL │ NAME                    │ PRICE     │ 24H
──────────────────────────────────────────────────
MINE   │ MineCorp Industries     │ $45.67    │ +12.3%
BTC    │ Bitcoin                 │ $89.12    │ -3.2%
TECH   │ TechCorp (Company)      │ $123.45   │ +5.7%
DIMD   │ Diamond Enterprises     │ $67.89    │ +8.1%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Tracking 4 instruments
💡 Use /watch info <SYMBOL> for details
```

**Empty Watchlist:**
```
👀 Your Watchlist
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Your watchlist is empty.

💡 Add instruments: /watch add <SYMBOL>
💡 Browse market: /market
💡 View stocks: /stocks
```

<!-- [GUI Screenshot Placeholder: Watchlist Display] -->

---

## ➕ Adding to Watchlist

### `/watch add <SYMBOL>`

Adds an instrument to your watchlist for easy tracking.

**Permission:** None (public)

**Arguments:**
- `<SYMBOL>` - Trading symbol to add (e.g., MINE, BTC, TECH)

**Examples:**
```
/watch add MINE
/watch add BTC
/watch add TECH
/wl add DIMD        # Using alias
```

**Sample Output:**
```
✅ Added MINE (MineCorp Industries) to your watchlist
💡 View your watchlist: /watch
💡 View details: /watch info MINE
```

**Features:**
- Prevents duplicate entries
- Validates symbol exists
- Instant confirmation
- Works with stocks, crypto, and company shares

---

## ➖ Removing from Watchlist

### `/watch remove <SYMBOL>`

Removes an instrument from your watchlist.

**Permission:** None (public)

**Arguments:**
- `<SYMBOL>` - Trading symbol to remove

**Examples:**
```
/watch remove MINE
/watch rem BTC          # Short form
/watch delete TECH      # Alternative
/watch del DIMD         # Alternative short
```

**Sample Output:**
```
✅ Removed MINE from your watchlist
💡 Add it back: /watch add MINE
```

**Alternatives:**
- `/watch rem <SYMBOL>` - Short form
- `/watch delete <SYMBOL>` - Alternative
- `/watch del <SYMBOL>` - Short alternative

---

## 🔍 Viewing Item Details

### `/watch info <SYMBOL>`

Shows detailed information about a specific instrument in your watchlist.

**Permission:** None (public)

**Arguments:**
- `<SYMBOL>` - Trading symbol to view

**Examples:**
```
/watch info MINE
/watch details BTC      # Alternative
```

**Sample Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💹 MineCorp Industries (MINE)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💵 Current Price: $45.67
📊 24h Change: +$5.12 (12.34%) ↗
📈 Trend: Bullish

📊 Performance:
  • 1 Hour: +2.3%
  • 24 Hours: +12.34%
  • 7 Days: +23.4%

📈 Statistics:
  • Volume (24h): 1,234 shares
  • Market Cap: $456,789
  • Volatility: 2.45%

⏰ Added to Watchlist: 2024-01-15 14:23
⏰ Last Updated: 2024-01-15 16:45

💡 Trade: /market buy MINE <quantity>
💡 Details: /stocks MINE
💡 Remove: /watch remove MINE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

<!-- [GUI Screenshot Placeholder: Watchlist Item Details] -->

---

## 🗑️ Clearing Watchlist

### `/watch clear`

Removes all instruments from your watchlist.

**Permission:** None (public)

**Warning:** This action cannot be undone!

**Example:**
```
/watch clear
```

**Sample Output:**
```
⚠️  Are you sure you want to clear your entire watchlist?
This will remove all 4 instruments.

Confirm with: /watch clear confirm

Or cancel by doing nothing.
```

**After Confirmation:**
```
✅ Cleared your watchlist (removed 4 instruments)
💡 Add instruments: /watch add <SYMBOL>
```

---

## 💡 Use Cases

### Active Trader Workflow

```bash
# Morning routine - check watchlist
/watch

# Add new opportunities found
/stocks                    # See top gainers
/watch add NEWSTOCK       # Add interesting stock

# Monitor throughout day
/watch                    # Quick price check
/watch info MINE          # Detailed analysis

# Trade based on watchlist
/market buy MINE 100      # Buy from watchlist
```

### Portfolio Monitoring

```bash
# Add your holdings to watchlist
/market portfolio         # See what you own
/watch add MINE          # Track MINE
/watch add BTC           # Track BTC
/watch add TECH          # Track TECH

# Daily monitoring
/watch                   # See all at once
```

### Research and Analysis

```bash
# Find interesting stocks
/stocks                  # Top gainers

# Add to watchlist for research
/watch add MINE
/watch add DIMD

# Analyze over time
/watch info MINE         # Check performance
/watch info DIMD         # Compare

# Make decision
/market buy MINE 50      # Buy winner
/watch remove DIMD       # Remove loser
```

### Company Monitoring

```bash
# Track competitors
/watch add TECHCORP
/watch add RIVALCO

# Monitor your company
/watch add MYCOMPANY

# Daily check
/watch                   # See all company stocks
```

---

## 🎯 Common Scenarios

### Scenario 1: New Trader Setup

```bash
# Find interesting stocks
/stocks

# Add top performers to watchlist
/watch add MINE
/watch add DIMD
/watch add BTC

# View your watchlist
/watch

# Research before buying
/watch info MINE
```

### Scenario 2: Portfolio Tracking

```bash
# Buy some stocks
/market buy MINE 100
/market buy BTC 50

# Add to watchlist for monitoring
/watch add MINE
/watch add BTC

# Daily check
/watch
# See price changes at a glance
```

### Scenario 3: Market Research

```bash
# Looking for opportunities
/watch add MINE          # Potential buy
/watch add DIMD          # Researching
/watch add BTC           # Watching trend

# Monitor over several days
/watch                   # Daily check
/watch info MINE         # Detailed view

# Make decision
/market buy MINE 200     # Buy
/watch remove DIMD       # Not interested
```

### Scenario 4: Spring Cleaning

```bash
# Watchlist getting cluttered
/watch
# Showing 15 instruments...

# Clear old items
/watch remove OLD1
/watch remove OLD2
/watch remove OLD3

# Or clear everything
/watch clear confirm

# Start fresh
/watch add MINE
/watch add BTC
```

---

## 💡 Tips and Best Practices

### For Traders

1. **Keep it focused** - Don't track too many (5-10 is ideal)
2. **Review regularly** - Check `/watch` daily to spot trends
3. **Use info for analysis** - `/watch info` shows detailed metrics
4. **Track your holdings** - Add stocks you own for easy monitoring
5. **Clean periodically** - Remove instruments you're no longer interested in

### For Investors

1. **Long-term tracking** - Add company shares for long-term monitoring
2. **Compare performance** - Use watchlist to compare multiple companies
3. **Set price targets** - Remember target prices, check watchlist regularly
4. **Diversification** - Track instruments across different sectors

### For Company Owners

1. **Monitor competition** - Add competitor company stocks
2. **Track your IPO** - Add your own company after going public
3. **Industry analysis** - Add all companies in your sector

### Organizational Tips

**Categorize mentally:**
- Short-term trades
- Long-term holds
- Research targets
- Company monitoring

**Regular maintenance:**
- Weekly review of watchlist
- Remove non-performers
- Add new opportunities
- Keep list under 10 items

---

## 🆘 Troubleshooting

### "Instrument not found"

**Cause:** Symbol doesn't exist

**Solution:**
- Check spelling and capitalization
- Use `/stocks` to see available symbols
- Try `/stocks diamond` to search by material

### "Already in watchlist"

**Cause:** Trying to add duplicate

**Solution:**
- Item is already tracked
- Use `/watch` to see current watchlist
- Use `/watch info SYMBOL` to view it

### "Not in watchlist"

**Cause:** Trying to remove item that's not tracked

**Solution:**
- Check `/watch` to see what's tracked
- Verify symbol spelling
- Item may have been removed already

### Watchlist not updating

**Cause:** Cache or database issue

**Solution:**
- Run `/watch` again
- Re-login to server
- Report to administrator if persists

---

## 🔗 Related Commands

- **[`/stocks`](Commands-Stocks.md)** - View detailed stock information
- **[`/market`](Commands-Market.md)** - Trade instruments from watchlist
- **[`/company`](Commands-Company.md)** - Manage company stocks

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Plugin basics
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Stocks Commands](Commands-Stocks.md)** - Stock information
- **[Market Commands](Commands-Market.md)** - Trading guide

---

*For server configuration, see [Configuration](Configuration.md)*
