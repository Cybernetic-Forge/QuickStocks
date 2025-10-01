# Getting Started with QuickStocks

Welcome to **QuickStocks** - a comprehensive stock market and company management plugin for Minecraft! This guide will help you get started with the plugin's features.

## 📖 What is QuickStocks?

QuickStocks transforms your Minecraft server into an immersive financial ecosystem where players can:

- 📊 **Trade stocks and cryptocurrencies** based on Minecraft materials
- 💼 **Create and manage companies** with employees and role-based permissions
- 💰 **Build portfolios** and watch your investments grow
- 📈 **Monitor markets** in real-time with a powerful trading interface
- 🏢 **Go public** and trade company shares with other players

## 🎮 Quick Start for Players

### Step 1: Check Your Wallet

Your wallet holds your in-game currency for trading. Check your balance with:

```
/wallet
```

If you need funds, ask a server administrator to add money to your wallet.

### Step 2: Explore the Market

Open the interactive market browser to see available instruments and companies:

```
/market
```

This will open a beautiful GUI where you can browse stocks, cryptocurrencies, and company shares.

### Step 3: View Stock Information

To see detailed information about any stock or crypto:

```
/stocks <SYMBOL>
```

For example:
- `/stocks MINE` - View MineCorp Industries stock
- `/stocks diamond` - Find stocks related to diamonds
- `/stocks` - See top 10 gainers

### Step 4: Create Your Watchlist

Keep track of your favorite instruments:

```
/watch add <SYMBOL>     # Add to watchlist
/watch list             # View your watchlist
/watch info <SYMBOL>    # Detailed watchlist info
/watch remove <SYMBOL>  # Remove from watchlist
```

### Step 5: Trade on the Market

Buy and sell company shares through the market:

```
/market buy <company> <quantity>
/market sell <company> <quantity>
/market portfolio                # View your holdings
/market history                  # View trade history
```

## 🏢 Starting a Company

### Creating Your First Company

Anyone can create a company (with enough funds):

```
/company create <name> <type>
```

**Company Types:**
- **PRIVATE** - Small, closed company
- **PUBLIC** - Can go on the stock market
- **DAO** - Decentralized autonomous organization

**Example:**
```
/company create TechCorp PUBLIC
```

This costs $1,000 by default (configurable by admins).

### Managing Your Company

Use the convenient settings GUI:

```
/company settings
```

Or specific commands:
```
/company info [name]              # View company details
/company deposit <company> <amount>   # Add funds
/company employees <company>      # View team
/company jobs <company>           # View job titles
```

### Inviting Employees

Grow your team by inviting other players:

```
/company invite <company> <player> <job>
```

**Example:**
```
/company invite TechCorp Steve EMPLOYEE
```

Steve will receive an invitation they can accept or decline:
```
/company invitations    # View pending invitations
/company accept <id>    # Accept an invitation
/company decline <id>   # Decline an invitation
```

## 📈 Going Public (IPO)

If you have a PUBLIC or DAO company with sufficient funds, you can sell shares on the market:

### Step 1: Set a Trading Symbol

```
/company setsymbol <company> <SYMBOL>
```

**Example:**
```
/company setsymbol TechCorp TECH
```

### Step 2: Ensure Minimum Balance

Your company needs:
- **PUBLIC**: $10,000 minimum balance
- **DAO**: $15,000 minimum balance

Deposit funds if needed:
```
/company deposit TechCorp 15000
```

### Step 3: Enable Market Trading

```
/company market enable <company>
```

Your company is now on the market! Players can buy shares with:
```
/market buy TechCorp 100
```

### Managing Market Settings

Configure how much of your company is tradeable:

```
/company market settings <company> percentage <1-100>
/company market settings <company> buyout <true|false>
```

**Buyout Protection:**
- When **enabled**: No one can own more than 50% of shares
- When **disabled**: Majority shareholder (>50%) becomes the new owner

## 💡 Tips and Tricks

### For Traders
- 📊 Use `/watch add` to track stocks you're interested in
- 💹 Check `/stocks` regularly to see top performers
- 📈 View your portfolio with `/market portfolio`
- 🔔 Use the Market Link Device for portable access

### For Company Owners
- 👥 Build a diverse team with different job titles
- 💰 Maintain a healthy company balance
- 📊 Monitor your shareholders with `/company market settings`
- 🎯 Set clear permissions for each job role

### For Employees
- 🏢 Check your company's status with `/company settings`
- 💵 Deposit funds to help your company grow
- 👔 Understand your job permissions
- 🤝 Collaborate with teammates

## 🎯 What's Next?

Now that you understand the basics, explore these topics:

- **[Commands Overview](Commands-Overview.md)** - Complete command reference
- **[Permissions](Permissions.md)** - Understanding permission system
- **[Configuration](Configuration.md)** - Server setup guide (for admins)

## 🆘 Need Help?

- Type `/company` or `/market` without arguments to see command help
- Hover over items in GUIs for helpful tooltips
- Ask server administrators for assistance
- Check the complete [Commands](Commands-Overview.md) documentation

---

**Ready to dive in?** Start by checking your wallet with `/wallet` and exploring the market with `/market`!
