# QuickStocks Permissions Reference

This page documents all permissions used by the QuickStocks plugin.

## 📋 Overview

QuickStocks uses a permission system to control access to various features. By default, most player-facing features are available to everyone, while administrative features require operator permissions.

## 🔐 Permission Nodes

### Cryptocurrency Permissions

#### `maksy.stocks.crypto.create`
- **Description:** Allows players to create custom cryptocurrency instruments
- **Default:** `false` (no one has this by default)
- **Usage:** Required for `/crypto create` command
- **Alternative:** `quickstocks.command.crypto.create` (same functionality)

**Example:**
```yaml
# Give permission to a player
luckperms user Steve permission set maksy.stocks.crypto.create true

# Give permission to a group
luckperms group vip permission set maksy.stocks.crypto.create true
```

**Commands Affected:**
- `/crypto create <symbol> <name>` - Create custom cryptocurrencies

---

### Wallet Permissions

#### `quickstocks.wallet.add`
- **Description:** Allows players to add money to their wallet (admin command)
- **Default:** `op` (operators only)
- **Usage:** Required for `/wallet add <amount>`

**Example:**
```
/wallet add 10000
```

#### `quickstocks.wallet.set`
- **Description:** Allows players to set wallet balance to a specific amount (admin command)
- **Default:** `op` (operators only)
- **Usage:** Required for `/wallet set <amount>`

**Example:**
```
/wallet set 50000
```

**Commands Affected:**
- `/wallet add <amount>` - Add money to wallet
- `/wallet set <amount>` - Set wallet balance

---

### Company Permissions

#### `quickstocks.company.create`
- **Description:** Allows players to create companies
- **Default:** `true` (everyone)
- **Usage:** Required for `/company create` command
- **Note:** Creating a company still requires paying the creation cost ($1,000 by default)

**Example:**
```
/company create TechCorp PUBLIC
```

#### `quickstocks.company.manage`
- **Description:** Allows players to manage companies they are part of
- **Default:** `true` (everyone)
- **Usage:** Required for most company management commands
- **Note:** Players must be employees of the company to manage it

**Commands Affected:**
- `/company info [name]` - View company information
- `/company deposit <company> <amount>` - Deposit to company
- `/company withdraw <company> <amount>` - Withdraw from company (requires role permission)
- `/company invite <company> <player> <job>` - Invite players (requires role permission)
- `/company createjob <company> <title> <perms>` - Create job titles (requires role permission)
- `/company assignjob <company> <player> <job>` - Assign jobs (requires role permission)
- `/company editjob <company> <title> <perms>` - Edit jobs (requires role permission)
- `/company settings [company]` - Open settings GUI
- `/company setsymbol <company> <symbol>` - Set trading symbol (owner only)
- `/company market enable|disable <company>` - Control market (owner only)
- `/company market settings <company>` - Configure market settings (owner only)

---

### Market Device Permissions

#### `maksy.stocks.marketdevice.give`
- **Description:** Allows giving Market Link Device items to players
- **Default:** `op` (operators only)
- **Usage:** Required for `/marketdevice give` command

**Example:**
```
/marketdevice give Steve
```

**Commands Affected:**
- `/marketdevice give [player]` - Give Market Link Device to player

---

### Administrative Permissions

#### `quickstocks.admin.audit`
- **Description:** Allows access to database audit and repair commands
- **Default:** `op` (operators only)
- **Usage:** Required for `/stocks audit` command

**Example:**
```
/stocks audit         # Check database integrity
/stocks audit repair  # Repair database issues
```

**Commands Affected:**
- `/stocks audit` - Database integrity check
- `/stocks audit repair` - Repair database

---

## 🎭 Role-Based Company Permissions

Within companies, job titles have their own permission system. These are **not** Minecraft permissions, but company-specific roles configured in `config.yml` or through commands.

### Company Job Permissions

Each job title in a company can have these permissions:

#### `canManageCompany`
- **Description:** Full company management access
- **Default Roles:** CEO
- **Abilities:**
  - Assign jobs to employees
  - Edit job titles
  - Full control over company

#### `canInvite`
- **Description:** Invite new members to the company
- **Default Roles:** CEO
- **Abilities:**
  - Send invitations to players
  - Grow the company team

#### `canCreateJobTitles`
- **Description:** Create new job titles with custom permissions
- **Default Roles:** CEO
- **Abilities:**
  - Create custom roles
  - Define role permissions

#### `canWithdraw`
- **Description:** Withdraw funds from company balance
- **Default Roles:** CEO, CFO
- **Abilities:**
  - Withdraw money from company
  - Manage finances

**Creating Custom Jobs:**
```
/company createjob TechCorp Manager invite,withdraw
```

**Editing Jobs:**
```
/company editjob TechCorp Manager invite,withdraw,manage
```

Permission strings for job creation:
- `invite` - Can invite members
- `withdraw` - Can withdraw funds
- `createjobs` - Can create job titles
- `manage` - Full company management

---

## 🔧 Permission Setup Examples

### Using LuckPerms

**Grant crypto creation to VIP players:**
```
luckperms group vip permission set maksy.stocks.crypto.create true
```

**Grant wallet commands to moderators:**
```
luckperms group moderator permission set quickstocks.wallet.add true
luckperms group moderator permission set quickstocks.wallet.set true
```

**Grant market device to admins:**
```
luckperms group admin permission set maksy.stocks.marketdevice.give true
```

**Grant audit commands to admins:**
```
luckperms group admin permission set quickstocks.admin.audit true
```

### Using PermissionsEx

**Grant crypto creation to a player:**
```
pex user Steve add maksy.stocks.crypto.create
```

**Grant wallet permissions to a group:**
```
pex group moderator add quickstocks.wallet.add
pex group moderator add quickstocks.wallet.set
```

### Using GroupManager

**Grant permissions to a group:**
```yaml
groups:
  vip:
    permissions:
      - maksy.stocks.crypto.create
  moderator:
    permissions:
      - quickstocks.wallet.add
      - quickstocks.wallet.set
      - maksy.stocks.marketdevice.give
```

---

## 🎯 Default Permission Matrix

| Permission Node | Default | Description |
|-----------------|---------|-------------|
| `maksy.stocks.crypto.create` | `false` | Create custom cryptocurrencies |
| `quickstocks.command.crypto.create` | `false` | Create custom cryptocurrencies (alternate) |
| `quickstocks.wallet.add` | `op` | Add money to wallet |
| `quickstocks.wallet.set` | `op` | Set wallet balance |
| `quickstocks.company.create` | `true` | Create companies |
| `quickstocks.company.manage` | `true` | Manage companies as employee |
| `maksy.stocks.marketdevice.give` | `op` | Give Market Link Device |
| `quickstocks.admin.audit` | `op` | Database audit commands |

---

## 📝 Permission Best Practices

### For Server Owners

1. **Keep crypto creation restricted** - Only give to trusted players or make it a donator perk
2. **Limit wallet commands** - Only admins should manipulate wallets
3. **Allow company features** - Most players should be able to create and manage companies
4. **Market devices are optional** - Decide if this should be a special item or craftable
5. **Audit is admin-only** - Only operators should access database repair tools

### For Players

1. **Understand your permissions** - Use `/crypto create` to check if you can create crypto
2. **Request permissions** - Ask admins if you need access to specific features
3. **Company roles matter** - Your job title determines what you can do in a company
4. **No permission for trading** - Anyone can trade on the market without special permissions

---

## 🐛 Troubleshooting

### "You don't have permission..."

**For crypto creation:**
- Verify you have `maksy.stocks.crypto.create` permission
- Ask your server administrator to grant this permission

**For wallet commands:**
- These are admin-only commands
- You need `quickstocks.wallet.add` or `quickstocks.wallet.set` permission
- Contact a server operator

**For company management:**
- Check you're actually an employee of the company
- Verify your job title has the required permissions (`/company jobs <company>`)
- Company-specific permissions are separate from Minecraft permissions

### Permission not working?

1. **Check your permission plugin** - Ensure LuckPerms/PermissionsEx/GroupManager is installed
2. **Verify permission syntax** - Copy exact permission nodes from this page
3. **Restart or reload** - Try `/lp reloadconfig` or restart the server
4. **Check inheritance** - Make sure permission groups are inheriting correctly
5. **Test as op** - Temporarily op yourself to verify the feature works

---

## 🔗 Related Documentation

- **[Getting Started](Getting-Started.md)** - Introduction to QuickStocks
- **[Commands Overview](Commands-Overview.md)** - All available commands
- **[Configuration](Configuration.md)** - Server configuration guide

---

*Need help? Contact your server administrators or check the plugin documentation.*
