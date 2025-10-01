# Company Commands

The `/company` command provides comprehensive company management including creation, employee management, finances, and stock market operations.

## 📋 Command Overview

### Company Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/company` | Show help menu | None |
| `/company create <name> <type>` | Create new company | `quickstocks.company.create` |
| `/company info [name]` | View company details | `quickstocks.company.manage` |
| `/company list [page]` | List all companies | None |
| `/company settings [company]` | Open settings GUI | `quickstocks.company.manage` |

### Employee Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/company invite <company> <player> <job>` | Invite player | Role-based |
| `/company invitations` | View your invitations | None |
| `/company accept <id>` | Accept invitation | None |
| `/company decline <id>` | Decline invitation | None |
| `/company employees <company>` | List employees | None |

### Financial Operations
| Command | Description | Permission |
|---------|-------------|------------|
| `/company deposit <company> <amount>` | Deposit funds | Employee |
| `/company withdraw <company> <amount>` | Withdraw funds | Role-based |

### Job Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/company jobs <company>` | List job titles | None |
| `/company createjob <company> <title> <perms>` | Create job title | Role-based |
| `/company editjob <company> <title> <perms>` | Edit job title | Role-based |
| `/company assignjob <company> <player> <job>` | Assign job | Role-based |

### Market Operations
| Command | Description | Permission |
|---------|-------------|------------|
| `/company setsymbol <company> <symbol>` | Set trading symbol | Owner only |
| `/company market enable <company>` | Go public (IPO) | Owner only |
| `/company market disable <company>` | Delist from market | Owner only |
| `/company market settings <company>` | View/edit market settings | Owner only |
| `/company notifications` | View notifications | None |

**Aliases:** `/corp`, `/corporation`

---

## 🏢 Creating Companies

### `/company create <name> <type>`

Create a new company and become its CEO.

**Permission:** `quickstocks.company.create` (default: everyone)

**Arguments:**
- `<name>` - Company name (unique, no spaces recommended)
- `<type>` - Company type: PRIVATE, PUBLIC, or DAO

**Cost:** $1,000 by default (configurable)

**Examples:**
```
/company create TechCorp PUBLIC
/company create MiningInc PRIVATE
/company create GamersDAO DAO
/corp create MyCompany PUBLIC    # Using alias
```

**Sample Output:**
```
✅ Successfully created company: TechCorp
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Type: PUBLIC | Balance: $0.00
Your Role: CEO (Full permissions)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Deposit funds: /company deposit TechCorp <amount>
💡 Invite employees: /company invite TechCorp <player> <job>
💡 Settings: /company settings TechCorp
```

**Company Types:**

| Type | Description | Can Go Public? |
|------|-------------|----------------|
| **PRIVATE** | Small, closed company | No |
| **PUBLIC** | Can trade on stock market | Yes (requires $10,000) |
| **DAO** | Decentralized organization | Yes (requires $15,000) |

**Automatic Setup:**
- You become the CEO
- Default job titles created (CEO, CFO, EMPLOYEE)
- Company starts with $0 balance
- Default permissions assigned

---

## 📊 Viewing Company Information

### `/company info [name]`

View detailed information about a company.

**Permission:** `quickstocks.company.manage`

**Arguments:**
- `[name]` - Company name (optional - shows your companies if omitted)

**Examples:**
```
/company info                # List your companies
/company info TechCorp       # View specific company
```

**Sample Output:**
```
=== TechCorp ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Type: PUBLIC
Owner: Steve
Balance: $45,678.90
Employees: 5
Created: 2024-01-15

Market Status: 🟢 On Market
Trading Symbol: TECH
Share Price: $128.50
Shareholders: 12

Your Role: CEO
Your Permissions:
  ✓ Manage Company
  ✓ Invite Members
  ✓ Create Job Titles
  ✓ Withdraw Funds
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Settings GUI: /company settings TechCorp
💡 View employees: /company employees TechCorp
💡 Deposit: /company deposit TechCorp <amount>
```

### `/company list [page]`

List all companies on the server.

**Permission:** None (public)

**Arguments:**
- `[page]` - Page number for pagination (optional)

**Example:**
```
/company list
/company list 2     # Page 2
```

**Sample Output:**
```
=== Companies (Page 1/3) ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. TechCorp (PUBLIC) - Owner: Steve
   💰 $45,678.90 | 👥 5 employees | 📈 On Market

2. MiningCo (PRIVATE) - Owner: Alex
   💰 $12,345.67 | 👥 3 employees

3. DiamondInc (PUBLIC) - Owner: Notch
   💰 $89,012.34 | 👥 8 employees | 📈 On Market
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Next page: /company list 2
💡 View details: /company info <name>
```

---

## 🎨 Company Settings GUI

### `/company settings [company]`

Opens an interactive GUI for managing your company.

**Permission:** `quickstocks.company.manage`

**Arguments:**
- `[company]` - Company name (optional - opens first company if omitted)

**Examples:**
```
/company settings                # Your first company
/company settings TechCorp       # Specific company
```

**GUI Features:**

```
┌─────────────────────────────────────────────┐
│  💰 Balance        👑 TechCorp        📋 CEO  │
│                                               │
│         $45,678.90    Type: PUBLIC           │
│                       Owner: Steve            │
│                       Created: 2024-01-15     │
├─────────────────────────────────────────────┤
│                                               │
│   👥 View        📚 View        💵 Deposit   │
│   Employees      Jobs           Funds        │
│                                               │
│   💸 Withdraw    🎯 Assign      ✉️ Invite    │
│   Funds          Job            Player        │
│                                               │
│                  📝 Create Job                │
│                                               │
├─────────────────────────────────────────────┤
│                                               │
│              🔄 Refresh      ❌ Close        │
└─────────────────────────────────────────────┘
```

**Features:**
- **Permission-aware** - Only shows what you can do
- **Quick actions** - One-click commands
- **Helpful tooltips** - Hover for command hints
- **Real-time data** - Shows current company state

<!-- [GUI Screenshot Placeholder: Company Settings Interface] -->

---

## 👥 Employee Management

### Inviting Players

#### `/company invite <company> <player> <job>`

Invite a player to join your company.

**Permission:** Role-based (`canInvite` permission)

**Arguments:**
- `<company>` - Your company name
- `<player>` - Player name to invite
- `<job>` - Job title to assign (CEO, CFO, EMPLOYEE, or custom)

**Examples:**
```
/company invite TechCorp Steve EMPLOYEE
/company invite TechCorp Alex CFO
/company invite TechCorp Notch Developer    # Custom job
```

**Sample Output:**
```
✅ Invitation sent to Steve
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
Position: EMPLOYEE
Expires: 7 days

Steve will be notified when they log in.
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Cancel: /company decline <invitation-id>
```

### Managing Invitations

#### `/company invitations`

View all pending invitations you've received.

**Permission:** None (public)

**Example:**
```
/company invitations
```

**Sample Output:**
```
=== Your Invitations ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. TechCorp (Invited by Steve)
   • Position: EMPLOYEE
   • Expires: in 6 days
   • /company accept 1

2. MiningCo (Invited by Alex)
   • Position: CFO
   • Expires: in 4 days
   • /company accept 2
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Accept: /company accept <id>
💡 Decline: /company decline <id>
```

#### `/company accept <id>`

Accept a company invitation.

**Permission:** None (public)

**Arguments:**
- `<id>` - Invitation ID from `/company invitations`

**Example:**
```
/company accept 1
```

**Sample Output:**
```
✅ Welcome to TechCorp!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Position: EMPLOYEE
Permissions:
  • Deposit funds
  • View company information

💡 View details: /company info TechCorp
💡 Settings: /company settings TechCorp
```

#### `/company decline <id>`

Decline a company invitation.

**Permission:** None (public)

**Arguments:**
- `<id>` - Invitation ID from `/company invitations`

**Example:**
```
/company decline 2
```

### Viewing Employees

#### `/company employees <company>`

List all employees of a company.

**Permission:** None (public)

**Arguments:**
- `<company>` - Company name

**Example:**
```
/company employees TechCorp
```

**Sample Output:**
```
=== TechCorp Employees ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. Steve - CEO (Owner)
   ✓ All permissions

2. Alex - CFO
   ✓ Withdraw funds

3. Notch - EMPLOYEE
   • Basic access

4. Jeb - Developer (Custom)
   ✓ Invite members
   ✓ Create job titles

5. Dinnerbone - EMPLOYEE
   • Basic access
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total: 5 employees
💡 Assign job: /company assignjob TechCorp <player> <job>
```

---

## 💰 Financial Operations

### Depositing Funds

#### `/company deposit <company> <amount>`

Deposit money from your wallet into the company balance.

**Permission:** Must be an employee

**Arguments:**
- `<company>` - Company name
- `<amount>` - Amount to deposit

**Examples:**
```
/company deposit TechCorp 5000
/company deposit MiningCo 10000.50
```

**Sample Output:**
```
✅ Deposit Successful
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
Amount: $5,000.00
New Company Balance: $50,678.90

Your Wallet: $45,000.00 (-$5,000.00)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Transaction recorded in company history
💡 View balance: /company info TechCorp
```

**Notes:**
- Any employee can deposit
- Funds transfer from your wallet to company
- Transaction is recorded
- If company is public, may affect share price

### Withdrawing Funds

#### `/company withdraw <company> <amount>`

Withdraw money from company balance to your wallet.

**Permission:** Role-based (`canWithdraw` permission)

**Arguments:**
- `<company>` - Company name
- `<amount>` - Amount to withdraw

**Examples:**
```
/company withdraw TechCorp 1000
/company withdraw MiningCo 500.50
```

**Sample Output:**
```
✅ Withdrawal Successful
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
Amount: $1,000.00
New Company Balance: $49,678.90

Your Wallet: $46,000.00 (+$1,000.00)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Transaction recorded in company history
💡 View balance: /wallet
```

**Notes:**
- Requires `canWithdraw` permission (CEO, CFO by default)
- Company must have sufficient balance
- Transaction is recorded
- If company is public, may affect share price

---

## 👔 Job Management

### Viewing Jobs

#### `/company jobs <company>`

List all job titles and their permissions for a company.

**Permission:** None (public)

**Arguments:**
- `<company>` - Company name

**Example:**
```
/company jobs TechCorp
```

**Sample Output:**
```
=== TechCorp Job Titles ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
CEO (Default)
  ✓ Manage Company
  ✓ Invite Members
  ✓ Create Job Titles
  ✓ Withdraw Funds

CFO (Default)
  ✓ Withdraw Funds

EMPLOYEE (Default)
  • Basic access only

Developer (Custom)
  ✓ Invite Members
  ✓ Create Job Titles
  ✓ Withdraw Funds
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Create job: /company createjob TechCorp <title> <perms>
💡 Edit job: /company editjob TechCorp <title> <perms>
```

### Creating Job Titles

#### `/company createjob <company> <title> <perms>`

Create a custom job title with specific permissions.

**Permission:** Role-based (`canCreateJobTitles` permission)

**Arguments:**
- `<company>` - Company name
- `<title>` - New job title name
- `<perms>` - Comma-separated permissions

**Permission Options:**
- `invite` - Can invite members
- `createjobs` - Can create job titles
- `withdraw` - Can withdraw funds
- `manage` - Full company management

**Examples:**
```
/company createjob TechCorp Manager invite,withdraw
/company createjob TechCorp Developer createjobs
/company createjob TechCorp Accountant withdraw
/company createjob TechCorp Admin manage
```

**Sample Output:**
```
✅ Created job title: Manager
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
Permissions:
  ✓ Invite Members
  ✓ Withdraw Funds

💡 Assign to player: /company assignjob TechCorp <player> Manager
💡 View all jobs: /company jobs TechCorp
```

### Editing Job Titles

#### `/company editjob <company> <title> <perms>`

Edit permissions for an existing job title.

**Permission:** Role-based (`canCreateJobTitles` permission)

**Arguments:**
- `<company>` - Company name
- `<title>` - Existing job title
- `<perms>` - New comma-separated permissions

**Examples:**
```
/company editjob TechCorp Manager invite,withdraw,createjobs
/company editjob TechCorp Developer invite
```

**Sample Output:**
```
✅ Updated job title: Manager
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
New Permissions:
  ✓ Invite Members
  ✓ Withdraw Funds
  ✓ Create Job Titles

All employees with this title have been updated.
```

### Assigning Jobs

#### `/company assignjob <company> <player> <job>`

Assign a job title to an employee.

**Permission:** Role-based (`canManageCompany` permission)

**Arguments:**
- `<company>` - Company name
- `<player>` - Employee name
- `<job>` - Job title to assign

**Examples:**
```
/company assignjob TechCorp Alex Manager
/company assignjob TechCorp Notch Developer
```

**Sample Output:**
```
✅ Job assigned successfully
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Player: Alex
Company: TechCorp
New Role: Manager

Permissions:
  ✓ Invite Members
  ✓ Withdraw Funds

Alex has been notified of their new role.
```

**Notes:**
- Player must be an employee
- Cannot demote CEO (protection)
- Player is notified of role change

---

## 📈 Market Operations (IPO)

### Setting Trading Symbol

#### `/company setsymbol <company> <symbol>`

Set the trading symbol for your company (required before going public).

**Permission:** Owner only

**Arguments:**
- `<company>` - Company name
- `<symbol>` - Trading symbol (2-6 characters)

**Examples:**
```
/company setsymbol TechCorp TECH
/company setsymbol MiningCo MINE
```

**Sample Output:**
```
✅ Trading symbol set successfully
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Company: TechCorp
Symbol: TECH

💡 Next steps:
1. Ensure balance ≥ $10,000
2. Enable market: /company market enable TechCorp
```

**Requirements:**
- 2-6 characters
- Letters and numbers only
- Must be unique
- Cannot change once set

### Enabling Market (Going Public)

#### `/company market enable <company>`

List your company on the stock market (IPO).

**Permission:** Owner only

**Requirements:**
- PUBLIC or DAO company type
- Trading symbol set
- Minimum balance:
  - PUBLIC: $10,000
  - DAO: $15,000

**Example:**
```
/company market enable TechCorp
```

**Sample Output:**
```
🎉 TechCorp is now on the market!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Trading Symbol: TECH
Share Price: $128.50
Total Shares: 10,000
Market Percentage: 70% (7,000 shares tradeable)

Players can now buy shares:
/market buy TECH <quantity>

💡 Configure settings: /company market settings TechCorp
💡 Monitor: /market shareholders TECH
```

### Disabling Market (Delisting)

#### `/company market disable <company>`

Remove your company from the stock market.

**Permission:** Owner only

**Warning:** All shareholders will be paid out at current price!

**Example:**
```
/company market disable TechCorp
```

**Sample Output:**
```
⚠️  WARNING: Delisting TechCorp from market
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Current Share Price: $128.50
Shareholders: 12 players
Total Payout: $89,950.00

This will:
✓ Pay all shareholders at current price
✓ Remove company from market
✓ Reset market status

Type again to confirm: /company market disable TechCorp confirm
```

### Market Settings

#### `/company market settings <company>`

View and configure market settings.

**Permission:** Owner only

**Example:**
```
/company market settings TechCorp
```

**Sample Output:**
```
=== TechCorp Market Settings ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Trading Symbol: TECH
Share Price: $128.50
Total Shares: 10,000

Market Percentage: 70%
  • Tradeable Shares: 7,000
  • Owner Retained: 3,000

Buyout Protection: ✓ ENABLED
  • Max Ownership: 50%
  • Prevents hostile takeovers

Shareholders: 12
Active Trades (24h): 145
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Change percentage: /company market settings TechCorp percentage <1-100>
💡 Toggle buyout: /company market settings TechCorp buyout <true|false>
💡 View shareholders: /market shareholders TECH
```

**Configure Percentage:**
```
/company market settings TechCorp percentage 60
```

**Toggle Buyout Protection:**
```
/company market settings TechCorp buyout true
/company market settings TechCorp buyout false
```

---

## 🔔 Notifications

### `/company notifications`

View offline notifications about company events.

**Permission:** None (public)

**Example:**
```
/company notifications
```

**Sample Output:**
```
=== Your Notifications ===
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔔 New notification (2 hours ago)
   Alex bought 100 shares of TechCorp

🔔 New notification (5 hours ago)
   Company balance updated: +$5,000.00

🔔 New notification (1 day ago)
   You were assigned as Manager in TechCorp
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Notification Types:**
- Share purchases/sales
- Role assignments
- Company balance changes
- Invitation responses
- Market events

---

## 💡 Tips and Best Practices

### For Company Owners

1. **Plan ahead** - Decide if you want to go public before creating
2. **Set clear roles** - Define job titles with appropriate permissions
3. **Build balance first** - Reach $10,000+ before IPO
4. **Choose symbol wisely** - Can't change after setting
5. **Monitor shareholders** - Watch for buyout attempts
6. **Use buyout protection** - Enable if you want to keep control

### For Employees

1. **Understand your role** - Check `/company jobs` for your permissions
2. **Help company grow** - Deposit funds when possible
3. **Respect permissions** - Don't try to exceed your role
4. **Communicate** - Work with team on company goals
5. **Track performance** - Monitor company balance and market performance

### For Investors

1. **Research companies** - Use `/company info` before buying shares
2. **Check shareholders** - See who else owns shares
3. **Monitor balance** - Company balance affects share price
4. **Understand buyout** - Some companies prevent majority ownership
5. **Watch for IPOs** - New companies going public can be opportunities

---

## 🎯 Common Scenarios

### Scenario 1: Starting a Company

```bash
# Create company
/company create TechCorp PUBLIC

# Deposit starting funds
/company deposit TechCorp 5000

# Invite team
/company invite TechCorp Alex CFO
/company invite TechCorp Notch EMPLOYEE

# Grow balance
/company deposit TechCorp 6000
# Now at $11,000

# Set symbol and go public
/company setsymbol TechCorp TECH
/company market enable TechCorp
```

### Scenario 2: Managing Employees

```bash
# Create custom role
/company createjob TechCorp Manager invite,withdraw

# Invite with custom role
/company invite TechCorp Jeb Manager

# View team
/company employees TechCorp

# Reassign role if needed
/company assignjob TechCorp Jeb Developer
```

### Scenario 3: IPO Process

```bash
# Check requirements
/company info TechCorp
# Need: $10k balance, symbol

# Set symbol
/company setsymbol TechCorp TECH

# Ensure balance
/company deposit TechCorp 15000

# Go public
/company market enable TechCorp

# Configure market
/company market settings TechCorp percentage 70
/company market settings TechCorp buyout true

# Monitor
/market shareholders TECH
```

### Scenario 4: Company Growth

```bash
# Check status
/company settings TechCorp

# Team contributes
/company deposit TechCorp 2000  # You
# Other employees also deposit

# Track growth
/company info TechCorp
# Balance growing!

# Share price increases
/stocks TECH
# Share price: $145.00 (was $128.50)
```

---

## 🆘 Troubleshooting

### "You don't have permission to create companies"

**Solution:**
- Need `quickstocks.company.create` permission
- Ask server administrator
- Check if feature is enabled

### "Company name already exists"

**Solution:**
- Choose a unique name
- Use `/company list` to see existing companies
- Try variations or abbreviations

### "Insufficient funds to create company"

**Solution:**
- Costs $1,000 by default
- Check wallet: `/wallet`
- Earn more money first

### "You don't have permission for this action"

**Solution:**
- Check your role: `/company info`
- View permissions: `/company jobs`
- Ask company owner for different role

### "Company balance too low for market"

**Solution:**
- PUBLIC needs $10,000
- DAO needs $15,000
- Deposit more: `/company deposit`

### "Trading symbol already in use"

**Solution:**
- Choose different symbol
- Check `/stocks` for existing symbols
- Try variations: TECH, TECH1, TECHCO

---

## 🔗 Related Commands

- **[`/market`](Commands-Market.md)** - Trade company shares
- **[`/wallet`](Commands-Wallet.md)** - Manage funds for deposits
- **[`/stocks`](Commands-Stocks.md)** - View company stock prices

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Company basics
- **[Commands Overview](Commands-Overview.md)** - All commands
- **[Permissions](Permissions.md)** - Permission details
- **[Configuration](Configuration.md)** - Server setup

---

*For server configuration, see [Configuration](Configuration.md)*
