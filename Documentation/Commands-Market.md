# Market Commands

The `/market` command provides a comprehensive trading interface for buying and selling company shares, viewing your portfolio, and managing your investments.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/market` | Open market browser GUI | None |
| `/market browse` | Open market browser GUI | None |
| `/market buy <company> <qty>` | Buy company shares | None |
| `/market sell <company> <qty>` | Sell company shares | None |
| `/market portfolio` | View your holdings | None |
| `/market history` | View trade history | None |
| `/market watchlist` | Watchlist summary | None |
| `/market shareholders <company>` | View company shareholders | None |

**Aliases:** `/trade`, `/trading`

---

## 🏪 Market Browser

### `/market` or `/market browse`

Opens an interactive GUI showing all available instruments and company shares.

**Permission:** None (public)

**Features:**
- Visual browsing of all tradeable instruments
- Company share listings with current prices
- Click-to-trade functionality
- Real-time price updates
- Beautiful material-based icons

**Example:**
```
/market
/market browse
/trade          # Using alias
```

**GUI Features:**
- **Browse Mode** - See all available instruments
- **Quick Buy** - Click items to start purchase
- **Price Display** - Current market prices
- **Company Info** - Hover for details
- **Navigation** - Page through listings

<!-- [GUI Screenshot Placeholder: Market Browser Interface] -->

---

## 💰 Buying Shares

### `/market buy <company> <quantity>`

Purchase shares of a company that's on the market.

**Permission:** None (public)

**Arguments:**
- `<company>` - Company name or trading symbol
- `<quantity>` - Number of shares to buy

**Examples:**
```
/market buy TechCorp 100
/market buy TECH 50        # Using symbol
/trade buy MyCompany 25    # Using alias
```

**Sample Output:**
```
✅ Purchase Successful!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp (TECH)
Shares: 100
Price: $123.45 per share
Total Cost: $12,345.00 + $30.86 fee (0.25%)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 New Balance: $37,624.14
📊 Total Shares Owned: 150

💡 View portfolio: /market portfolio
💡 Track: /watch add TECH
```

**How It Works:**
1. **Checks availability** - Verifies company is on market
2. **Calculates cost** - Share price × quantity + trading fee
3. **Verifies funds** - Ensures you have enough wallet balance
4. **Executes trade** - Transfers shares and deducts cost
5. **Updates records** - Records transaction in history

**Trading Fees:**
- Default: 0.25% of transaction value
- Configurable by server administrators
- Applied to both buy and sell orders

---

## 📤 Selling Shares

### `/market sell <company> <quantity>`

Sell shares you own back to the market.

**Permission:** None (public)

**Arguments:**
- `<company>` - Company name or trading symbol
- `<quantity>` - Number of shares to sell

**Examples:**
```
/market sell TechCorp 50
/market sell TECH 25       # Using symbol
/trade sell MyCompany 10   # Using alias
```

**Sample Output:**
```
✅ Sale Successful!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp (TECH)
Shares Sold: 50
Price: $128.50 per share
Proceeds: $6,425.00 - $16.06 fee (0.25%)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 New Balance: $44,033.08
📊 Remaining Shares: 100

💡 Profit/Loss: +$253.00 (+4.1%)
💡 View portfolio: /market portfolio
```

**Profit/Loss Calculation:**
- Compares sale price to your average cost
- Shows total P&L in dollars and percentage
- Green for profit, red for loss

---

## 📊 Portfolio Management

### `/market portfolio`

View all your current holdings including shares, values, and unrealized gains/losses.

**Permission:** None (public)

**Example:**
```
/market portfolio
/market holdings    # Alternative
```

**Sample Output:**
```
=== Your Portfolio ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💵 Cash Balance: $44,033.08
📊 Portfolio Value: $15,678.00
💰 Total Assets: $59,711.08

Holdings:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
TechCorp (TECH): 100.00 shares @ $123.45 avg
  • Current: $128.50
  • P&L: ▲$505.00 (+4.1%)
  
MiningCo (MINE): 50.00 shares @ $45.20 avg
  • Current: $48.90
  • P&L: ▲$185.00 (+8.2%)

DiamondInc (DIMD): 25.00 shares @ $89.00 avg
  • Current: $85.50
  • P&L: ▼$87.50 (-3.9%)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total Unrealized P&L: ▲$602.50 (+4.0%)
```

**Portfolio Metrics:**
- **Cash Balance** - Available wallet funds
- **Portfolio Value** - Current market value of all holdings
- **Total Assets** - Cash + Portfolio Value
- **Average Cost** - Your average purchase price per share
- **Current Price** - Latest market price
- **Unrealized P&L** - Profit/loss if sold now

<!-- [GUI Screenshot Placeholder: Portfolio Display] -->

---

## 📜 Trade History

### `/market history`

View your recent trade history (last 10 transactions).

**Permission:** None (public)

**Example:**
```
/market history
```

**Sample Output:**
```
=== Order History ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BUY TechCorp (TECH): 100.00 @ $123.45
  2024-01-15 14:23:45

SELL TechCorp (TECH): 50.00 @ $128.50
  2024-01-15 16:10:22

BUY MiningCo (MINE): 50.00 @ $45.20
  2024-01-14 09:15:33

BUY DiamondInc (DIMD): 25.00 @ $89.00
  2024-01-13 11:42:18
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Showing last 10 transactions
💡 View portfolio: /market portfolio
```

**History Details:**
- Transaction type (BUY/SELL)
- Company name and symbol
- Quantity and price
- Timestamp

---

## 👥 Shareholder Information

### `/market shareholders <company>`

View all shareholders and their ownership percentages for a company.

**Permission:** None (public)

**Arguments:**
- `<company>` - Company name or symbol

**Examples:**
```
/market shareholders TechCorp
/market shareholders TECH
```

**Sample Output:**
```
=== TechCorp Shareholders ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp (TECH)
Type: PUBLIC
Share Price: $128.50
Total Shares: 10,000

Shareholders:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Steve (Owner)
   • Shares: 3,000 (30.00%)
   • Value: $385,500

2. Alex
   • Shares: 1,500 (15.00%)
   • Value: $192,750

3. Notch
   • Shares: 800 (8.00%)
   • Value: $102,800

4. Jeb
   • Shares: 500 (5.00%)
   • Value: $64,250

... and 12 more shareholders
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Buy shares: /market buy TECH <quantity>
```

---

## 👀 Watchlist Summary

### `/market watchlist`

Quick summary of instruments in your watchlist with current prices.

**Permission:** None (public)

**Example:**
```
/market watchlist
/market watch       # Short form
```

**Sample Output:**
```
=== Watchlist Summary ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Watching 3 instruments:

TECH - TechCorp: $128.50 ▲ +4.1%
MINE - MiningCo: $48.90 ▲ +8.2%
DIMD - DiamondInc: $85.50 ▼ -3.9%
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Full watchlist: /watch
💡 Add instrument: /watch add <symbol>
```

---

## 💡 Trading Tips

### For New Traders

1. **Start small** - Buy 10-20 shares to learn the system
2. **Use watchlist** - Track before buying with `/watch add`
3. **Check portfolio** - Monitor with `/market portfolio`
4. **Understand fees** - 0.25% fee on all transactions
5. **Research first** - Use `/stocks <symbol>` for details

### For Active Traders

1. **Monitor market daily** - Check `/market browse` regularly
2. **Track performance** - Review `/market portfolio` often
3. **Use history** - Learn from past trades with `/market history`
4. **Diversify** - Don't put all funds in one company
5. **Set targets** - Know your buy/sell prices in advance

### For Long-term Investors

1. **Buy and hold** - Focus on company fundamentals
2. **Dollar-cost averaging** - Buy regularly over time
3. **Dividend potential** - Some companies may pay dividends
4. **Monitor shareholders** - Watch for ownership changes
5. **Company news** - Pay attention to company events

---

## 🎯 Common Trading Scenarios

### Scenario 1: First Trade

```bash
# Check your wallet
/wallet
# Balance: $50,000

# Browse the market
/market

# Research a company
/stocks TECH

# Add to watchlist
/watch add TECH

# Buy shares
/market buy TECH 100
# Spent $12,376.06 (including fees)

# Check portfolio
/market portfolio
```

### Scenario 2: Portfolio Management

```bash
# Morning routine - check portfolio
/market portfolio
# See current values and P&L

# Check market for opportunities
/market

# Rebalance if needed
/market sell LOSING 50    # Sell losers
/market buy WINNING 100   # Buy winners

# Review end of day
/market history
```

### Scenario 3: Company Research

```bash
# Find interesting company
/company list

# Check if on market
/stocks TECH

# View shareholders
/market shareholders TECH

# Add to watchlist
/watch add TECH

# Monitor for a while
/watch info TECH

# Make purchase decision
/market buy TECH 50
```

### Scenario 4: Profit Taking

```bash
# Check portfolio
/market portfolio
# TechCorp: +15% profit!

# Review performance
/watch info TECH

# Take some profits
/market sell TECH 50
# Sold 50 shares at profit

# Keep the rest
/market portfolio
# Still holding 50 shares
```

---

## 📊 Understanding Share Prices

### Price Calculation

Company share prices are calculated as:
```
Share Price = Company Balance / 10,000 shares
```

**Example:**
- Company balance: $1,280,000
- Total shares: 10,000
- Share price: $128.00

### Price Factors

Share prices change based on:
1. **Company deposits** - Increases balance, raises price
2. **Company withdrawals** - Decreases balance, lowers price
3. **Company activities** - Business operations affect balance
4. **Market sentiment** - Supply and demand dynamics

### Market Percentage

Companies can configure what percentage is tradeable:
- **Default:** 70% available to market
- **Range:** 1-100%
- **Owner retention:** Remaining percentage held by owner

---

## 🔧 Trading Fees

### Fee Structure

**Default Configuration:**
- **Mode:** Percentage-based
- **Rate:** 0.25% of transaction value
- **Applied to:** Both buy and sell orders

**Fee Calculation:**
```
Buy Order:  Total Cost = (Shares × Price) × (1 + Fee%)
Sell Order: Proceeds = (Shares × Price) × (1 - Fee%)
```

**Example:**
- Buying 100 shares @ $128.50
- Cost: $12,850.00
- Fee: $32.13 (0.25%)
- Total: $12,882.13

### Fee Modes

Servers may configure different fee structures:
1. **Percent** - Percentage of transaction value
2. **Flat** - Fixed amount per trade
3. **Mixed** - Combination of both

*Check with your server administrators for specific fee configuration*

---

## 🆘 Troubleshooting

### "Company not found"

**Cause:** Company doesn't exist or isn't on market

**Solution:**
- Check spelling and capitalization
- Use `/company list` to see all companies
- Use `/market browse` to see tradeable companies
- Company must enable market with `/company market enable`

### "Insufficient funds"

**Cause:** Not enough wallet balance for purchase

**Solution:**
- Check balance: `/wallet`
- Remember trading fees (0.25%)
- Buy fewer shares
- Deposit more funds

### "You don't own that many shares"

**Cause:** Trying to sell more than you own

**Solution:**
- Check portfolio: `/market portfolio`
- Sell only what you own
- Verify company symbol is correct

### "Company has insufficient balance"

**Cause:** Company can't buy back shares

**Solution:**
- Company needs more funds
- Try selling fewer shares
- Contact company owner
- Wait for company to deposit funds

### "Share limit reached"

**Cause:** Hit maximum ownership percentage

**Solution:**
- Some companies have buyout protection
- Maximum ownership may be 50%
- Sell some shares to stay under limit

---

## 🔗 Related Commands

- **[`/company`](Commands-Company.md)** - Manage companies and IPOs
- **[`/wallet`](Commands-Wallet.md)** - Check and manage funds
- **[`/watch`](Commands-Watch.md)** - Track instruments
- **[`/stocks`](Commands-Stocks.md)** - Research instruments

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Trading basics
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Company Commands](Commands-Company.md)** - Company management
- **[Configuration](Configuration.md)** - Trading configuration

---

*For server configuration, see [Configuration](Configuration.md)*
