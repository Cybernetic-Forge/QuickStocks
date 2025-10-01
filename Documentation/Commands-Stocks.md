# Stocks Commands

The `/stocks` command provides access to stock market information, analytics, and database administration tools.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/stocks` | Show top 10 gainers (24h) | None |
| `/stocks <SYMBOL>` | View detailed stock information | None |
| `/stocks <material>` | Find stocks by Minecraft material | None |
| `/stocks audit` | Database integrity check | `quickstocks.admin.audit` |
| `/stocks audit repair` | Repair database issues | `quickstocks.admin.audit` |

**Aliases:** `/stock`, `/quote`

---

## 📈 Viewing Market Overview

### `/stocks`

Displays the top 10 performing instruments over the last 24 hours.

**Permission:** None (public)

**Output:**
- Rank, Symbol, Name, Current Price, 24h Change %
- Beautiful formatted table with color-coded changes
- Suggestions for viewing detailed information

**Example:**
```
/stocks
```

**Sample Output:**
```
📈 TOP 10 GAINERS (24H)
════════════════════════════════════════════════════════════
RANK │ SYMBOL │ NAME                    │ PRICE  │ 24H CHANGE
────────────────────────────────────────────────────────────
1    │ MINE   │ MineCorp Industries     │ $45.67 │ +12.34%
2    │ DIMD   │ Diamond Enterprises     │ $89.12 │ +8.76%
3    │ GOLD   │ GoldCorp                │ $23.45 │ +7.89%
...
────────────────────────────────────────────────────────────
💡 Use /stocks <symbol> for detailed information
```

<!-- [GUI Screenshot Placeholder: Top 10 Gainers Table] -->

---

## 🔍 Viewing Stock Details

### `/stocks <SYMBOL>`

Shows comprehensive information about a specific instrument including:
- Current price and 24h change
- Trading volume
- Market capitalization
- Volatility metrics
- Historical performance (1h, 24h)
- Sector classification

**Permission:** None (public)

**Arguments:**
- `<SYMBOL>` - The trading symbol (e.g., MINE, DIMD, BTC)

**Examples:**
```
/stocks MINE
/stocks BTC
/stocks EMERALD
```

**Sample Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💹 MineCorp Industries (MINE)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💵 Price: $45.67 │ +$5.12 (12.34%) ↗

📊 Statistics:
  • Volume (24h): 1,234 shares
  • Market Cap: $456,789
  • Volatility: 2.45%
  
📈 Performance:
  • 1 Hour: +2.3%
  • 24 Hours: +12.34%
  • 7 Days: +23.4%

🏢 Sector: TECHNOLOGY
⏰ Last Updated: 14:23:45

💡 Trade this stock: /market buy MINE <quantity>
💡 Add to watchlist: /watch add MINE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

<!-- [GUI Screenshot Placeholder: Stock Detail Card] -->

---

## 🔎 Finding Stocks by Material

### `/stocks <material>`

Search for stocks based on Minecraft materials. Useful when you remember the material but not the exact symbol.

**Permission:** None (public)

**Arguments:**
- `<material>` - Minecraft material name (e.g., diamond, gold, iron)

**Examples:**
```
/stocks diamond
/stocks emerald  
/stocks gold_ingot
```

**Sample Output:**
```
💎 Stocks related to 'diamond':

1. DIMD - Diamond Enterprises
   • Price: $89.12 (+8.76%)
   • Material: DIAMOND
   
2. DJEW - Diamond Jewelry Co.
   • Price: $123.45 (+3.21%)
   • Material: DIAMOND_BLOCK

💡 Use /stocks <SYMBOL> for detailed information
```

<!-- [GUI Screenshot Placeholder: Material Search Results] -->

---

## 🔧 Database Administration

### `/stocks audit`

Performs a database integrity check to identify any inconsistencies in the market data.

**Permission:** `quickstocks.admin.audit` (operators only)

**Usage:** For server administrators only

**What it checks:**
- Missing price history entries
- Orphaned instrument records
- Inconsistent state data
- Data corruption indicators

**Example:**
```
/stocks audit
```

**Sample Output:**
```
🔍 Database Audit Report
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Instruments: 156 records
✅ Price History: 45,678 entries
✅ State Records: 156 current
⚠️  Found 3 orphaned history entries
⚠️  Found 1 missing state record
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Use /stocks audit repair to fix issues
```

### `/stocks audit repair`

Attempts to automatically repair database inconsistencies found during the audit.

**Permission:** `quickstocks.admin.audit` (operators only)

**Usage:** Only use when audit identifies issues

**What it fixes:**
- Creates missing state records
- Removes orphaned entries
- Rebuilds indexes
- Normalizes data

**Example:**
```
/stocks audit repair
```

**Sample Output:**
```
🔧 Database Repair Report
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Removed 3 orphaned entries
✅ Created 1 missing state record
✅ Rebuilt indexes
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Database repair complete!
```

**⚠️ Warning:** Always backup your database before running repair operations!

---

## 💡 Tips and Best Practices

### For Traders

1. **Check top gainers daily** - Run `/stocks` each day to spot opportunities
2. **Monitor specific stocks** - Use `/stocks <SYMBOL>` before making trades
3. **Add to watchlist** - Use `/watch add` to track interesting stocks
4. **Understand volatility** - High volatility means higher risk and reward

### For Company Owners

1. **Track competitors** - Use `/stocks` to see how other public companies perform
2. **Monitor your symbol** - Check your company's stock price regularly
3. **Analyze trends** - Watch 24h performance to understand market sentiment

### For Administrators

1. **Regular audits** - Run `/stocks audit` weekly to ensure data integrity
2. **Backup before repair** - Always backup database before using repair
3. **Monitor performance** - Check if query times are acceptable
4. **Review top gainers** - Ensure market simulation is realistic

---

## 📊 Understanding Market Data

### Price Changes

Colors indicate performance:
- 🟢 **Green/Positive** - Stock is up (gaining value)
- 🔴 **Red/Negative** - Stock is down (losing value)
- ⚪ **Gray/Neutral** - No significant change

### Volatility

Measures how much the price fluctuates:
- **Low (0-2%)** - Stable, predictable
- **Medium (2-5%)** - Moderate risk
- **High (5%+)** - High risk, high reward

### Market Cap

Total value of all shares:
- **Small Cap** - Under $100,000
- **Mid Cap** - $100,000 - $1,000,000
- **Large Cap** - Over $1,000,000

### Trading Volume

Number of shares traded:
- **High Volume** - Popular, liquid stock
- **Low Volume** - Less active trading

---

## 🎯 Common Use Cases

### Scenario 1: Daily Trading Routine

```bash
# Check top gainers
/stocks

# View details of interesting stock
/stocks MINE

# Add to watchlist for monitoring
/watch add MINE

# View stock from watchlist later
/watch info MINE
```

### Scenario 2: Material-Based Trading

```bash
# Find diamond-related stocks
/stocks diamond

# Check specific company
/stocks DIMD

# Make trading decision
/market buy DIMD 100
```

### Scenario 3: Company IPO Monitoring

```bash
# Check your company's stock performance
/stocks TECH

# View detailed analytics
/stocks TECH

# Adjust company strategy based on performance
/company market settings TechCorp
```

### Scenario 4: Admin Maintenance

```bash
# Check database health
/stocks audit

# Fix any issues found
/stocks audit repair

# Verify repair was successful
/stocks audit
```

---

## 🆘 Troubleshooting

### "No stocks found in the market"

**Cause:** No instruments have been seeded yet

**Solution:** 
- Wait for market initialization
- Check if `market.defaultStocks: true` in config.yml
- Ask admin to seed instruments manually

### "Stock not found: SYMBOL"

**Cause:** Symbol doesn't exist or was misspelled

**Solution:**
- Check spelling and capitalization
- Use `/stocks` to see available stocks
- Try searching by material: `/stocks diamond`

### "Database error occurred"

**Cause:** Database connection or corruption issue

**Solution:**
- Report to server administrator
- Admin should run `/stocks audit`
- Check server logs for details

### Permission denied for audit

**Cause:** Not an operator

**Solution:**
- This is an admin-only command
- Request operator status from server owner
- Regular players don't need audit access

---

## 🔗 Related Commands

- **[`/market`](Commands-Market.md)** - Trade stocks through market interface
- **[`/watch`](Commands-Watch.md)** - Track favorite stocks in watchlist
- **[`/company`](Commands-Company.md)** - Manage your company's public stock

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Learn the basics
- **[Commands Overview](Commands-Overview.md)** - All command domains
- **[Market Commands](Commands-Market.md)** - Trading interface
- **[Permissions](Permissions.md)** - Permission reference

---

*For server configuration, see [Configuration](Configuration.md)*
