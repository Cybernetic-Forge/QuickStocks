# Company Management System - Copilot Instructions

## Overview
The company management system allows players to create, manage, and operate companies with employees, roles, finances, plots, salaries, and stock market features.

## Architecture

### Key Components
```
core/services/features/companies/
├── CompanyService.java        # Core company operations
├── InvitationService.java     # Employee invitations
├── CompanyPlotService.java    # Land ownership management
└── SalaryService.java         # Employee salary payments

gui/
├── CompanySettingsGUI.java    # Company management interface
└── PlotEditGUI.java           # Plot management interface
```

### Database Schema
**Tables:**
- `companies` - Company registry (name, type, owner, balance, market_enabled)
- `company_employees` - Employee memberships with job titles
- `company_jobs` - Job role definitions with permissions
- `company_invitations` - Pending invitations (7-day expiration)
- `company_tx` - Transaction history (deposits/withdrawals)
- `company_plots` - Owned chunks/plots with coordinates
- `company_salaries` - Employee salary definitions
- `company_salary_payments` - Salary payment history

## Key Features

### 1. Company Types
Defined in `companies.yml`:

**PRIVATE**
- Private ownership
- No public trading
- Owner retains full control

**PUBLIC**
- Can go public (IPO)
- Shares tradeable on market
- Ownership can transfer via share purchase

**DAO (Decentralized Autonomous Organization)**
- Community-owned
- Voting-based governance (future feature)
- Democratic decision making

### 2. Company Creation

**Cost:** Configurable in `companies.yml` (default: $1000)

**Process:**
```java
Company company = companyService.createCompany(
    companyName,
    CompanyType.PUBLIC,
    ownerUuid
);
```

**Automatic Setup:**
- Deducts creation cost from wallet
- Creates default job titles (CEO, CFO, EMPLOYEE)
- Sets creator as CEO
- Initializes company balance at $0
- Fires `CompanyCreateEvent`

**Command:** `/company create <name> <type>`

### 3. Employee Management

**Invitation System:**
Location: `InvitationService.java`

**Flow:**
1. CEO/authorized member invites player
2. Invitation stored with 7-day expiration
3. Invitee receives notification
4. Invitee can accept/decline
5. On accept: Added to company with specified job
6. Fires `CompanyEmployeeJoinEvent`

**Commands:**
- `/company invite <company> <player> <job>` - Send invitation
- `/company invitations` - View pending invitations
- `/company accept <invitationId>` - Accept invitation
- `/company decline <invitationId>` - Decline invitation
- `/company leave <company>` - Leave company
- `/company kick <company> <player>` - Remove employee

**Permissions Required:**
- `INVITE_EMPLOYEES` permission on job role
- `REMOVE_EMPLOYEES` permission to kick

### 4. Job System

**Default Jobs:**
Automatically created with every company:
- **CEO** - Full permissions
- **CFO** - Financial permissions
- **EMPLOYEE** - Basic permissions

**Job Permissions:**
Defined in `companies.yml`:
```yaml
companies:
  permissionsByTitle:
    CEO:
      - MANAGE_COMPANY
      - MANAGE_FINANCES
      - INVITE_EMPLOYEES
      - REMOVE_EMPLOYEES
      - MANAGE_JOBS
      - MANAGE_PLOTS
      - MANAGE_SALARIES
    CFO:
      - MANAGE_FINANCES
      - VIEW_FINANCES
    EMPLOYEE:
      - VIEW_COMPANY
```

**Custom Jobs:**
Create new job titles with custom permissions:
```bash
/company createjob <company> <title> <permission1,permission2>
```

**Assigning Jobs:**
```bash
/company assignjob <company> <player> <jobtitle>
```

### 5. Financial Management

**Company Balance:**
- Separate from player wallets
- Shared resource for all employees
- Tracked in `companies.balance` column

**Deposits:**
```bash
/company deposit <company> <amount>
```
- Any employee can deposit
- Deducted from player wallet
- Added to company balance
- Recorded in `company_tx`

**Withdrawals:**
```bash
/company withdraw <company> <amount>
```
- Requires `MANAGE_FINANCES` permission
- Deducted from company balance
- Added to player wallet
- Recorded in `company_tx`

**Transaction History:**
All financial operations logged in `company_tx`:
- Transaction type (DEPOSIT, WITHDRAWAL, IPO_PAYOUT, etc.)
- Amount
- Player UUID
- Timestamp
- Description

### 6. Company Plots (Land Ownership)

**Overview:**
Companies can purchase and own Minecraft chunks (16x16 blocks).

**Features:**
- Configurable purchase price per plot
- Rent system (optional)
- Automatic debt management
- Plot seizure for unpaid rent
- WorldGuard integration for protection

**Purchase:**
```bash
/company buyplot <company>
```
- Must be standing in desired chunk
- Deducts cost from company balance
- Registers plot ownership
- Creates WorldGuard region (if enabled)

**Configuration:** `companies.yml`
```yaml
companies:
  plots:
    enabled: true
    pricePerPlot: 10000.0
    rentEnabled: true
    rentPerPlot: 100.0
    rentIntervalHours: 168  # Weekly
    autoPayEnabled: true
    gracePeriodHours: 24
```

**Rent System:**
- Optional automatic rent charges
- Configurable intervals (default: weekly)
- Grace period before seizure
- Auto-payment from company balance
- Debt tracking and warnings

**Commands:**
- `/company buyplot <company>` - Purchase current chunk
- `/company sellplot <company>` - Sell current chunk
- `/company plots <company>` - List owned plots
- `/company plottp <company> <plotId>` - Teleport to plot
- `/company plotrent <company>` - View rent status

**Plot Visualization:**
- `/company plotshow <company> <plotId>` - Show plot borders with particles

**Integration:**
Location: `hooks/worldguard/WorldGuardHook.java`
- Automatic region creation
- Custom flag: `quickstocks-plots`
- Employee-based permissions

### 7. Salary System

**Overview:**
Companies can pay employees regular salaries.

**Setup:**
```bash
/company setsalary <company> <player> <amount> <interval>
```

**Intervals:**
- HOURLY
- DAILY
- WEEKLY
- MONTHLY

**Process:**
1. Define salary for employee
2. Automatic payment at intervals
3. Deducted from company balance
4. Paid to employee wallet
5. Recorded in payment history

**Commands:**
- `/company setsalary <company> <player> <amount> <interval>`
- `/company removesalary <company> <player>`
- `/company salaries <company>` - View all salaries
- `/company paystatus <company>` - Check payment status

**Configuration:** `companies.yml`
```yaml
companies:
  salaries:
    enabled: true
    minAmount: 1.0
    maxAmount: 100000.0
```

### 8. Stock Market Integration

**Going Public (IPO):**
```bash
/company ipo <company> <sharePrice> <totalShares>
```

**Requirements:**
- Company type must be PUBLIC
- CEO permission required
- Sets initial share price
- Creates tradeable instrument
- Distributes shares to owner/employees

**Share Trading:**
After IPO, company shares trade like any other instrument:
- Buy: `/market buy <SYMBOL> <quantity>`
- Sell: `/market sell <SYMBOL> <quantity>`
- View shareholders: `/market shareholders <SYMBOL>`

**Ownership Transfer:**
When player buys >50% of shares, they automatically become company owner.

**Going Private:**
```bash
/company private <company>
```
- Removes from market
- Buys back all shares
- Returns to private ownership

### 9. Company Settings GUI

**Command:** `/company settings [company]`

**Features:**
- Visual company overview
- Balance display
- Your job title and permissions
- Quick action buttons (permission-aware)
- Click to execute commands
- Refresh data in real-time

**Layout:**
```
┌─────────────────────────────────────┐
│  💰 Balance   👑 Company   📋 Role  │
│                                     │
│  👥 Employees   📚 Jobs   💵 Deposit│
│  💸 Withdraw   🎯 Assign   ✉️ Invite│
│               📝 Create Job         │
│                                     │
│          🔄 Refresh   ❌ Close      │
└─────────────────────────────────────┘
```

### 10. ChestShop Integration

**Overview:**
Companies can own ChestShop shops, with revenue going to company balance.

**Setup:**
1. Create ChestShop as normal
2. Assign to company using ChestShop admin commands
3. Revenue automatically goes to company
4. Employees with permissions can manage shop

**Integration:** `hooks/chestshop/ChestShopHook.java`

## Configuration

### Primary Config: `companies.yml`
```yaml
companies:
  enabled: true
  creationCost: 1000.0
  
  defaultTypes:
    - PRIVATE
    - PUBLIC
    - DAO
  
  defaultJobTitles:
    - CEO
    - CFO
    - EMPLOYEE
  
  permissionsByTitle:
    CEO: [MANAGE_COMPANY, MANAGE_FINANCES, ...]
    CFO: [MANAGE_FINANCES, VIEW_FINANCES]
    EMPLOYEE: [VIEW_COMPANY]
  
  plots:
    enabled: true
    pricePerPlot: 10000.0
    rentEnabled: true
    rentPerPlot: 100.0
    rentIntervalHours: 168
  
  salaries:
    enabled: true
    minAmount: 1.0
    maxAmount: 100000.0
```

## Commands

### Company Command (`/company`)
Handler: `commands/CompanyCommand.java`

**Management:**
- `/company create <name> <type>` - Create company
- `/company info [company]` - View company details
- `/company list` - List all companies
- `/company settings [company]` - Open settings GUI
- `/company delete <company>` - Delete company (owner only)

**Employees:**
- `/company invite <company> <player> <job>` - Invite player
- `/company invitations` - View pending invitations
- `/company accept <id>` - Accept invitation
- `/company decline <id>` - Decline invitation
- `/company leave <company>` - Leave company
- `/company kick <company> <player>` - Remove employee
- `/company employees <company>` - List employees

**Jobs:**
- `/company jobs <company>` - List job titles
- `/company createjob <company> <title> <perms>` - Create job
- `/company assignjob <company> <player> <job>` - Assign job

**Finance:**
- `/company deposit <company> <amount>` - Deposit funds
- `/company withdraw <company> <amount>` - Withdraw funds
- `/company balance <company>` - View balance
- `/company transactions <company>` - View history

**Plots:**
- `/company buyplot <company>` - Buy current chunk
- `/company sellplot <company>` - Sell current chunk
- `/company plots <company>` - List plots
- `/company plottp <company> <id>` - Teleport to plot
- `/company plotshow <company> <id>` - Show borders
- `/company plotrent <company>` - View rent status

**Salaries:**
- `/company setsalary <company> <player> <amount> <interval>`
- `/company removesalary <company> <player>`
- `/company salaries <company>` - View all salaries
- `/company paystatus <company>` - Payment status

**Market:**
- `/company ipo <company> <price> <shares>` - Go public
- `/company private <company>` - Go private
- `/company symbol <company> <symbol>` - Set trading symbol

## Events

```java
// Company creation
CompanyCreateEvent event = new CompanyCreateEvent(company, creator);
Bukkit.getPluginManager().callEvent(event);

// Employee joined
CompanyEmployeeJoinEvent event = new CompanyEmployeeJoinEvent(company, employee);
Bukkit.getPluginManager().callEvent(event);

// Employee left
CompanyEmployeeLeaveEvent event = new CompanyEmployeeLeaveEvent(company, employee);
Bukkit.getPluginManager().callEvent(event);

// Company IPO
CompanyIPOEvent event = new CompanyIPOEvent(company, sharePrice, totalShares);
Bukkit.getPluginManager().callEvent(event);
```

## Development Guidelines

### Adding New Job Permissions
1. Define in `companies.yml` under `permissionsByTitle`
2. Check permission in command handler:
```java
if (!companyService.hasPermission(playerUuid, companyId, CompanyPermission.PERMISSION_NAME)) {
    Translation.Company_NoPermission.sendMessage(player);
    return;
}
```

### Adding New Company Features
1. Add configuration to `companies.yml`
2. Update `CompanyService` with new methods
3. Add database migration if schema changes needed
4. Update command handler
5. Add to CompanySettingsGUI if applicable
6. Fire events for state changes

### Testing Company Operations
See `test/java/.../core/services/CompanyServiceTest.java`:
- Test company creation with cost deduction
- Test employee management
- Test permission checks
- Test financial operations
- Test job assignment

## Common Patterns

### Getting Company by Name
```java
CompanyService companyService = QuickStocksPlugin.getCompanyService();
Optional<Company> company = companyService.getCompanyByName("TechCorp");
```

### Checking Permission
```java
boolean hasPermission = companyService.hasPermission(
    playerUuid,
    companyId,
    CompanyPermission.MANAGE_FINANCES
);
```

### Recording Transaction
```java
companyService.recordTransaction(
    companyId,
    playerUuid,
    TransactionType.WITHDRAWAL,
    amount,
    "Withdrawal by CEO"
);
```

### Getting Employee Job
```java
Optional<String> jobTitle = companyService.getEmployeeJob(playerUuid, companyId);
```

## Performance Considerations

### Employee Lookup
- Index on `company_employees.player_uuid`
- Cache company memberships per session
- Batch queries for multiple companies

### Plot Operations
- Chunk coordinates indexed
- Plot visualization uses async particle spawning
- Rent checks batched for all companies

### Transaction History
- Paginated queries for large transaction logs
- Archive old transactions periodically

## Troubleshooting

### Company Creation Fails
- Check wallet balance >= creation cost
- Verify company name is unique
- Check database connection
- Look for SQL errors in logs

### Permission Denied
- Verify player is company employee
- Check job title has required permission
- Verify permission is defined in companies.yml

### Plot Purchase Fails
- Check company balance >= plot price
- Verify plot not already owned
- Check chunk coordinates are valid
- Verify WorldGuard integration if enabled

### Salary Not Paid
- Check company balance >= salary amount
- Verify salary interval has elapsed
- Check salary system is enabled
- Look for payment failures in logs

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Market trading: `.github/copilot/features/market-trading.md`
- Database layer: `.github/copilot/features/database-persistence.md`
- GUI system: `.github/copilot/features/gui-system.md`
- Plugin integrations: `.github/copilot/features/plugin-integrations.md`
