# Company/Corporation Feature

## Overview
The Company/Corporation system allows players to form organizations, assign roles, pool funds, and operate through designated titles. Companies have shared balances and role-based permissions for managing operations.

## Features

### Company Creation
- Create companies with configurable types (PRIVATE, PUBLIC, DAO)
- Configurable creation cost (default: $1000)
- Automatic creation of default job titles (CEO, CFO, EMPLOYEE)
- Owner automatically assigned as CEO

### Role-Based Permissions
Each job title can have the following permissions:
- **canManageCompany**: Full company management (assign jobs, etc.)
- **canInvite**: Invite new members
- **canCreateJobTitles**: Create custom job titles
- **canWithdraw**: Withdraw funds from company

Default roles:
- **CEO**: All permissions enabled
- **CFO**: Can withdraw funds only
- **EMPLOYEE**: No special permissions

### Invitation System
- Invite players to join your company
- 7-day expiration on invitations
- Invitations can be accepted, declined, or cancelled
- Status tracking (PENDING, ACCEPTED, DECLINED, EXPIRED, CANCELLED)

### Financial Operations
- **Deposit**: Any employee can deposit funds
- **Withdraw**: Requires canWithdraw permission
- Transaction history tracking with timestamps
- Audit trail for all financial operations

### Job Management
- Create custom job titles with specific permissions
- Edit existing job titles to change permissions
- Assign jobs to employees
- View all job titles and their permissions

### Market Operations (NEW)
- **Go Public**: Companies of specific types can sell shares on the market
- **Trading Symbols**: Set unique trading symbols for companies
- **Share Trading**: Players can buy and sell company shares
- **Buyout Protection**: Optional protection against hostile takeovers
- **Shareholder Management**: Track all shareholders and their holdings
- **Market Settings**: Configure market percentage and buyout rules
- **Notifications**: Offline notifications for market events

## Commands

### Basic Operations
```
/company create <name> <type>           - Create a new company
/company info [name]                    - View company details (or list your companies)
/company list [page]                    - List all companies
```

### Member Management
```
/company invite <company> <player> <job>  - Invite a player to join
/company invitations                      - View your pending invitations
/company accept <id>                      - Accept an invitation
/company decline <id>                     - Decline an invitation
/company employees <company>              - List all employees
```

### Financial Operations
```
/company deposit <company> <amount>       - Deposit funds into company
/company withdraw <company> <amount>      - Withdraw funds (requires permission)
```

### Job Management
```
/company jobs <company>                         - List all job titles
/company createjob <company> <title> <perms>    - Create a new job title
/company editjob <company> <title> <perms>      - Edit an existing job title
/company assignjob <company> <player> <job>     - Assign job to employee
```

### Market Operations (NEW)
```
/company setsymbol <company> <symbol>              - Set trading symbol (required before IPO)
/company market enable <company>                   - Enable market trading (IPO)
/company market disable <company>                  - Disable market (delist, pays out shareholders)
/company market settings <company>                 - View market settings
/company market settings <company> percentage <n>  - Set market percentage (1-100)
/company market settings <company> buyout <bool>   - Enable/disable buyout protection
/company buyshares <company> <quantity>            - Buy company shares
/company sellshares <company> <quantity>           - Sell company shares
/company shareholders <company>                    - View all shareholders
/company notifications                             - View unread notifications
```

### Permission Format
When creating jobs, use comma-separated permissions:
- `invite` - Can invite members
- `createjobs` - Can create job titles
- `withdraw` - Can withdraw funds
- `manage` - Full company management

Example: `/company createjob MyCompany Manager invite,withdraw`

## Configuration

Add to `config.yml`:

```yaml
companies:
  enabled: true
  creationCost: 1000.0         # cost to create a new company (0 = free)
  defaultTypes:
    - PRIVATE
    - PUBLIC
    - DAO
  defaultJobTitles:
    - CEO
    - CFO
    - EMPLOYEE
  permissionsByTitle:
    CEO:
      canManageCompany: true
      canInvite: true
      canCreateJobTitles: true
      canWithdraw: true
    CFO:
      canWithdraw: true
      canInvite: false
      canCreateJobTitles: false
    EMPLOYEE:
      canInvite: false
      canWithdraw: false
      canCreateJobTitles: false
  # Market settings (NEW)
  marketableTypes:           # company types that can go on the market
    - PUBLIC
    - DAO
  marketBalanceThresholds:   # minimum balance required to enable market
    PUBLIC: 10000.0
    DAO: 15000.0
  defaultMarketPercentage: 70.0  # default percentage of company on market
```

## Permissions

- `quickstocks.company.create` (default: true) - Create companies
- `quickstocks.company.manage` (default: true) - Manage companies you're part of

## Database Schema

The feature adds 8 tables:

1. **companies** - Company registry (extended with market fields)
2. **company_jobs** - Job titles with permissions
3. **company_employees** - Employee memberships
4. **company_invitations** - Invitation tracking
5. **company_tx** - Transaction history
6. **company_shareholders** - Shareholder tracking (NEW)
7. **company_share_tx** - Share transaction history (NEW)
8. **player_notifications** - Offline notification system (NEW)

See `src/main/resources/migrations/V7__companies.sql` and `V9__company_market.sql` for details.

## Usage Examples

### Creating a Company
```
/company create TechCorp PUBLIC
```
This creates a public company called "TechCorp" and assigns you as CEO.

### Going on the Market (IPO)
```
/company setsymbol TechCorp TECH
/company deposit TechCorp 15000
/company market enable TechCorp
```
This sets the trading symbol, deposits enough funds to meet the threshold, and enables market trading.

### Configuring Market Settings
```
/company market settings TechCorp percentage 60
/company market settings TechCorp buyout false
```
This sets 60% of the company on the market and enables buyout protection.

### Trading Shares
```
/company buyshares TechCorp 100
/company sellshares TechCorp 50
/company shareholders TechCorp
```
Buy shares, sell shares, and view all shareholders.

### Inviting Members
```
/company invite TechCorp Steve EMPLOYEE
```
Steve will receive an invitation notification.

### Accepting an Invitation
```
/company invitations
/company accept <invitation-id>
```

### Managing Finances
```
/company deposit TechCorp 5000
/company withdraw TechCorp 1000
```

### Creating Custom Jobs
```
/company createjob TechCorp Developer invite,withdraw
/company assignjob TechCorp Alex Developer
```

### Editing Existing Jobs
If you need to change permissions for an existing job:
```
/company editjob TechCorp Developer invite,createjobs,withdraw
```
This updates the "Developer" job to have invite, createjobs, and withdraw permissions.

## Architecture

### Services
- **CompanyService**: Core company operations (create, manage, transactions)
- **InvitationService**: Invitation lifecycle management
- **CompanyMarketService**: Market operations (IPO, trading, buyouts) (NEW)

### Models
- **Company**: Company entity (extended with market fields)
- **CompanyJob**: Job title with permissions
- **CompanyEmployee**: Employee membership
- **CompanyInvitation**: Invitation with status tracking

### Key Features

#### CEO Protection
CEOs cannot demote themselves to prevent accidental loss of company control. To transfer ownership, use the buyout mechanism or manually transfer ownership first.

#### Market Mechanics
- **Share Price**: Calculated as company balance / 10,000 shares
- **Market Percentage**: Controls how much of the company can be traded (default 70%)
- **Buyout Protection**: When disabled, players can buy >50% shares and gain ownership
- **Automatic Ownership Transfer**: When buyout occurs, the new majority shareholder becomes CEO

#### Notification System
All market events trigger notifications that are delivered:
- Immediately to online players
- Stored for offline players to see when they log in
- Accessible via `/company notifications`

### Integration
- Integrates with WalletService for financial operations
- Uses existing database infrastructure
- Follows plugin's IoC pattern

## Market Requirements & Edge Cases

### Requirements to Go on Market
1. Company must be of a marketable type (PUBLIC or DAO by default)
2. Company must have sufficient balance (thresholds: PUBLIC $10k, DAO $15k)
3. Company must have a trading symbol set
4. Only the company owner can enable/disable the market

### Edge Cases Handled
1. **CEO Self-Demotion**: Prevented to avoid loss of control
2. **Market Disable**: All shareholders are automatically paid out at current share price
3. **Buyout Protection**: Optional setting prevents hostile takeovers by limiting ownership to 50%
4. **Insufficient Liquidity**: Company must have enough balance to buy back shares when players sell
5. **Ownership Transfer**: When buyout occurs, new owner is automatically assigned CEO role
6. **Offline Notifications**: Market events are stored for offline players
7. **Symbol Uniqueness**: Trading symbols must be unique across all companies

## Future Enhancements

Potential additions (not implemented):
- Company disband functionality
- Dividend distribution system
- Company-owned stock portfolios
- Company rankings and leaderboards
- Inter-company trading
- Merger and acquisition mechanics
- Stock splits and reverse splits
- Secondary market (player-to-player trading)
- Limit orders for share purchases
