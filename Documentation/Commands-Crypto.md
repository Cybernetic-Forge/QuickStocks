# Crypto Commands

The `/crypto` command allows players to create custom cryptocurrency instruments that can be traded on the market.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/crypto create <symbol> <name>` | Create custom cryptocurrency | `maksy.stocks.crypto.create` |

**Aliases:** None

---

## 🪙 Creating Custom Cryptocurrencies

### `/crypto create <symbol> <name>`

Creates a new custom cryptocurrency instrument that appears on the market.

**Permission:** `maksy.stocks.crypto.create` (restricted by default)

**Arguments:**
- `<symbol>` - Trading symbol (2-6 characters, uppercase recommended)
- `<name>` - Display name (can include spaces, use quotes if multi-word)

**Examples:**
```
/crypto create MYCOIN "My Custom Coin"
/crypto create LUNA "Luna Token"
/crypto create DOGE "Dogecoin Tribute"
/crypto create BTC2 "Bitcoin 2.0"
```

**Sample Output:**
```
🎉 Custom Crypto Created Successfully!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💰 Symbol: MYCOIN
📝 Name: My Custom Coin
🆔 ID: 8f3e9b2a-4c1d-4e5f-9a8b-7c6d5e4f3a2b
👤 Created By: Steve
📊 Type: CUSTOM_CRYPTO
💵 Initial Price: $1.00
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Your crypto is now tradeable on the market!
💡 View it: /stocks MYCOIN
💡 Trade it: /market buy MYCOIN <quantity>
```

<!-- [GUI Screenshot Placeholder: Crypto Creation Success Message] -->

---

## 🎨 Symbol Naming Guidelines

### Valid Symbols

- **Length:** 2-6 characters
- **Characters:** Letters and numbers
- **Case:** Uppercase recommended (MYCOIN, BTC2)
- **Uniqueness:** Must not conflict with existing symbols

**Good Examples:**
- ✅ `MYCOIN` - Clear, descriptive
- ✅ `LUNA` - Short, memorable
- ✅ `BTC2` - Recognizable reference
- ✅ `GOLD` - Simple, clear

**Bad Examples:**
- ❌ `MC` - Too short (minimum 2 chars)
- ❌ `VERYLONGNAME` - Too long (maximum 6 chars)
- ❌ `MY-COIN` - Contains invalid characters
- ❌ `MINE` - Conflicts with existing stock

### Display Name Guidelines

- Can include spaces and special characters
- Use quotes for multi-word names
- Should be descriptive and unique
- Maximum ~50 characters recommended

**Examples:**
```
/crypto create TECH "TechCorp Token"
/crypto create SPACE "Space Mining Coin"
/crypto create FARM "Agricultural DAO Token"
```

---

## 💼 Use Cases

### Community Currencies

Create server-specific cryptocurrencies:

```
/crypto create SERVER "MyServer Official Token"
/crypto create DONOR "Donator Exclusive Coin"
/crypto create EVENT "Summer Event Token"
```

### Role-Playing Economics

Build immersive economic systems:

```
/crypto create MAGIC "Mage Guild Currency"
/crypto create KNIGHT "Knights Order Token"
/crypto create TRADE "Merchant Guild Coin"
```

### Competitive Trading

Create coins for trading competitions:

```
/crypto create COMP1 "Trading Competition Alpha"
/crypto create PRIZE "Prize Pool Token"
```

### Experimental Markets

Test economic theories:

```
/crypto create STABLE "Stable Coin Test"
/crypto create VOLATILE "High Risk Coin"
/crypto create DEFLA "Deflationary Token"
```

---

## 📈 After Creation

Once created, your cryptocurrency:

1. **Appears in market** - Players can find it in `/market browse`
2. **Has initial price** - Starts at $1.00
3. **Follows market dynamics** - Price changes based on trading and market factors
4. **Can be traded** - Anyone can buy/sell using `/market` commands
5. **Appears in watchlists** - Players can track it with `/watch add`
6. **Shows in top gainers** - May appear in `/stocks` if performing well

### Viewing Your Crypto

```bash
# Check current price and stats
/stocks MYCOIN

# Add to your watchlist
/watch add MYCOIN

# Trade on market
/market buy MYCOIN 100
```

---

## 🔐 Permission Setup

This feature is restricted by default. Server administrators must grant permission.

### Using LuckPerms

**Grant to a player:**
```
luckperms user Steve permission set maksy.stocks.crypto.create true
```

**Grant to a group:**
```
luckperms group vip permission set maksy.stocks.crypto.create true
```

### Using PermissionsEx

```
pex user Steve add maksy.stocks.crypto.create
pex group donator add maksy.stocks.crypto.create
```

### Configuration Option

Admins can also set this as a donator/VIP perk by assigning it to specific permission groups.

---

## 💡 Tips and Best Practices

### For Players

1. **Choose unique symbols** - Avoid conflicts with existing instruments
2. **Descriptive names** - Help others understand what your crypto represents
3. **Monitor performance** - Use `/stocks SYMBOL` to track your creation
4. **Promote your crypto** - Tell other players about it to increase trading volume

### For Server Owners

1. **Control access** - Only give this permission to trusted players
2. **Consider cost** - You might want to charge for crypto creation
3. **Monitor creations** - Watch for inappropriate names or spam
4. **Moderate symbols** - Reserve common symbols (BTC, ETH) for official use
5. **Create official coins** - Make server-specific cryptocurrencies

### For Economy Managers

1. **Strategic creation** - Plan cryptocurrencies that serve specific purposes
2. **Balance supply** - Don't create too many competing coins
3. **Set expectations** - Explain to players how crypto works
4. **Track performance** - Monitor which cryptos gain traction

---

## 🎯 Common Scenarios

### Scenario 1: Creating a Server Currency

```bash
# Create official server cryptocurrency
/crypto create SVRCOIN "Official Server Token"

# Check it was created
/stocks SVRCOIN

# Announce to players
"Trade our official SVRCOIN on /market!"
```

### Scenario 2: Donator Perk Crypto

```bash
# Create VIP-exclusive cryptocurrency
/crypto create VIPCOIN "VIP Member Token"

# Monitor its value
/watch add VIPCOIN

# View performance
/watch info VIPCOIN
```

### Scenario 3: Event-Based Token

```bash
# Create limited-time event crypto
/crypto create SUMMER "Summer Festival Token"

# Players can trade during event
"Collect SUMMER tokens during the event!"

# Monitor trading
/stocks SUMMER
```

---

## 🆘 Troubleshooting

### "You don't have permission to create custom crypto"

**Cause:** Missing `maksy.stocks.crypto.create` permission

**Solution:**
- Ask server administrator for permission
- Check if this is a donator/VIP perk on your server
- Verify with `/crypto create` to see permission status

### "Symbol already exists"

**Cause:** Another instrument already uses that symbol

**Solution:**
- Choose a different symbol
- Use `/stocks` to see existing symbols
- Try adding numbers: BTC → BTC2

### "Invalid symbol format"

**Cause:** Symbol doesn't meet requirements

**Solution:**
- Use 2-6 characters only
- Only letters and numbers
- No spaces or special characters
- Try: MYCOIN, LUNA, COIN1

### "Name is too long"

**Cause:** Display name exceeds character limit

**Solution:**
- Shorten the name
- Keep it under 50 characters
- Use abbreviations if needed

---

## 🔗 Related Commands

- **[`/stocks`](Commands-Stocks.md)** - View cryptocurrency details
- **[`/market`](Commands-Market.md)** - Trade cryptocurrencies
- **[`/watch`](Commands-Watch.md)** - Track cryptocurrency performance

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Plugin basics
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Permissions](Permissions.md)** - Permission details
- **[Market Commands](Commands-Market.md)** - Trading guide

---

*For server configuration, see [Configuration](Configuration.md)*
