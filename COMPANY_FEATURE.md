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
- Assign jobs to employees
- View all job titles and their permissions

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
/company assignjob <company> <player> <job>     - Assign job to employee
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
```

## Permissions

- `quickstocks.company.create` (default: true) - Create companies
- `quickstocks.company.manage` (default: true) - Manage companies you're part of

## Database Schema

The feature adds 5 new tables:

1. **companies** - Company registry
2. **company_jobs** - Job titles with permissions
3. **company_employees** - Employee memberships
4. **company_invitations** - Invitation tracking
5. **company_tx** - Transaction history

See `src/main/resources/migrations/V7__companies.sql` for details.

## Usage Examples

### Creating a Company
```
/company create TechCorp PRIVATE
```
This creates a private company called "TechCorp" and assigns you as CEO.

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

## Architecture

### Services
- **CompanyService**: Core company operations (create, manage, transactions)
- **InvitationService**: Invitation lifecycle management

### Models
- **Company**: Company entity
- **CompanyJob**: Job title with permissions
- **CompanyEmployee**: Employee membership
- **CompanyInvitation**: Invitation with status tracking

### Integration
- Integrates with WalletService for financial operations
- Uses existing database infrastructure
- Follows plugin's IoC pattern

## Future Enhancements

Potential additions (not implemented):
- Company disband functionality
- Dividend distribution system
- Company-owned stock portfolios
- Company rankings and leaderboards
- Inter-company trading
- Merger and acquisition mechanics
