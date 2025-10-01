# Wallet Commands

The `/wallet` command manages your in-game wallet balance used for trading on the market.

## 📋 Command Overview

| Command | Description | Permission |
|---------|-------------|------------|
| `/wallet` | Check your current balance | None |
| `/wallet balance` | Check your current balance | None |
| `/wallet add <amount>` | Add money to wallet (admin) | `quickstocks.wallet.add` |
| `/wallet set <amount>` | Set wallet balance (admin) | `quickstocks.wallet.set` |

**Aliases:** `/money`, `/balance`

---

## 💰 Checking Your Balance

### `/wallet` or `/wallet balance`

Displays your current wallet balance.

**Permission:** None (public)

**Example:**
```
/wallet
```

**Sample Output:**
```
💰 Your Wallet Balance
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💵 Balance: $12,345.67
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Alternative Commands:**
```
/money         # Using alias
/balance       # Using alias
/wallet bal    # Short form
```

---

## 🔧 Administrative Commands

### `/wallet add <amount>`

Adds a specified amount to your wallet balance.

**Permission:** `quickstocks.wallet.add` (operators only)

**Arguments:**
- `<amount>` - Amount to add (must be positive)

**Examples:**
```
/wallet add 10000
/wallet add 5000.50
```

**Sample Output:**
```
✅ Added $10,000.00 to your wallet.
💰 New balance: $22,345.67
```

**Notes:**
- Amount must be positive
- Decimal values supported
- Admin/operator only command

---

### `/wallet set <amount>`

Sets your wallet balance to a specific amount.

**Permission:** `quickstocks.wallet.set` (operators only)

**Arguments:**
- `<amount>` - New balance amount (cannot be negative)

**Examples:**
```
/wallet set 50000
/wallet set 100000.00
```

**Sample Output:**
```
✅ Wallet balance set to $50,000.00
```

**Notes:**
- Cannot set negative balances
- Replaces current balance completely
- Admin/operator only command
- Use with caution (affects economy)

---

## 💡 Integration with Vault

QuickStocks automatically detects and integrates with Vault if available:

- **With Vault:** Wallet commands use your Vault economy balance
- **Without Vault:** Wallet uses internal QuickStocks balance system

**No configuration needed** - detection is automatic!

### Checking Integration

When you run `/wallet`, the plugin automatically:
1. Checks if Vault is installed
2. Uses Vault economy if available
3. Falls back to internal wallet if not

---

## 💼 Use Cases

### For Players

**Check balance before trading:**
```bash
# Check wallet
/wallet

# Browse market
/market

# Buy shares if you have enough
/market buy TechCorp 100
```

**Monitor spending:**
```bash
# Before trading
/wallet
# Balance: $10,000

# After several trades
/wallet
# Balance: $8,500
```

### For Administrators

**Give starting funds:**
```bash
# Give new players starting money
/wallet set 5000
```

**Economy events:**
```bash
# Give bonus for event
/wallet add 1000
```

**Economy resets:**
```bash
# Reset player balance
/wallet set 0

# Or give standard amount
/wallet set 10000
```

**Testing:**
```bash
# Give test funds
/wallet add 100000

# Test trading
/market buy MINE 10
```

---

## 🎯 Common Scenarios

### Scenario 1: New Player Setup

```bash
# Check starting balance
/wallet
# Balance: $0.00

# Admin gives starting funds
/wallet set 5000
# New balance: $5,000.00

# Now ready to trade!
/market
```

### Scenario 2: Economy Management

```bash
# Admin checks player balance
/wallet
# Balance: $50,000

# Too much inflation, reset economy
/wallet set 10000
# New balance: $10,000.00
```

### Scenario 3: Event Rewards

```bash
# Player wins event
"Congratulations! You won the trading competition!"

# Admin gives reward
/wallet add 5000
# Added $5,000.00. New balance: $15,000.00
```

### Scenario 4: Testing Features

```bash
# Developer testing
/wallet set 1000000
# Set to $1,000,000 for testing

# Test expensive trades
/market buy TechCorp 1000
```

---

## 💡 Tips and Best Practices

### For Players

1. **Check before trading** - Always verify balance before making purchases
2. **Monitor spending** - Track wallet changes to manage finances
3. **Ask for help** - If balance issues occur, contact administrators
4. **Use aliases** - `/money` and `/balance` work the same way

### For Administrators

1. **Set reasonable starting funds** - Balance economy with starting amounts
2. **Use add for bonuses** - Reward players with `/wallet add`
3. **Use set for resets** - Reset economy with `/wallet set`
4. **Document economy rules** - Tell players how to earn money
5. **Monitor inflation** - Watch for excessive wealth accumulation
6. **Backup before changes** - Save data before major economy changes

### Economy Balance Guidelines

**Starting Funds:**
- **Conservative:** $1,000-$5,000
- **Standard:** $5,000-$10,000
- **Generous:** $10,000-$25,000

**Stock Prices:**
- Adjust based on starting funds
- Most stocks: $10-$100
- Premium stocks: $100-$500
- Company shares: Based on company balance

---

## 🔐 Permission Setup

### For LuckPerms

**Grant to moderators:**
```
luckperms group moderator permission set quickstocks.wallet.add true
luckperms group moderator permission set quickstocks.wallet.set true
```

**Grant to specific player:**
```
luckperms user Steve permission set quickstocks.wallet.add true
```

### For PermissionsEx

```
pex group admin add quickstocks.wallet.add
pex group admin add quickstocks.wallet.set
```

---

## 🆘 Troubleshooting

### "You don't have permission to add money"

**Cause:** Missing `quickstocks.wallet.add` permission

**Solution:**
- This is an admin command
- Request operator status
- Ask server owner for permission

### "Amount must be positive"

**Cause:** Trying to add negative or zero amount

**Solution:**
- Use positive numbers only
- For `/wallet add`, use amounts > 0
- To reduce balance, use `/wallet set`

### "Amount cannot be negative"

**Cause:** Trying to set negative balance

**Solution:**
- Minimum balance is $0.00
- Use `/wallet set 0` to reset to zero

### Vault integration issues

**Cause:** Vault not responding or misconfigured

**Solution:**
- Check if Vault is installed and running
- Verify economy plugin is connected to Vault
- QuickStocks will fall back to internal wallet
- Check server logs for Vault errors

### Balance not updating

**Cause:** Database or sync issue

**Solution:**
- Re-login to server
- Check `/wallet` again
- Report to administrator
- Admin should check database connection

---

## 🔗 Related Commands

- **[`/market`](Commands-Market.md)** - Trade using wallet funds
- **[`/company deposit`](Commands-Company.md)** - Deposit wallet funds to company
- **[`/company withdraw`](Commands-Company.md)** - Withdraw company funds to wallet

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - New player guide
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Market Commands](Commands-Market.md)** - Trading with wallet
- **[Permissions](Permissions.md)** - Permission reference

---

*For server configuration, see [Configuration](Configuration.md)*
