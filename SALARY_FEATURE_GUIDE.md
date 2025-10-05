# 💰 Employee Salary System - Quick Start Guide

## Overview

The salary system allows companies to automatically pay their employees on a regular schedule. Company managers with the `salaries` permission can configure job-level salaries, set custom salaries for specific players, and choose payment cycles that fit their company's needs.

## Key Features

✅ **Job-Level Salaries** - Set a salary for each job title (CEO, CFO, EMPLOYEE, etc.)  
✅ **Player-Specific Salaries** - Override job salaries for individual employees  
✅ **Flexible Payment Cycles** - Choose from hourly, daily, weekly, bi-weekly, or monthly payments  
✅ **Automatic Processing** - Salaries are paid automatically by the server  
✅ **Payment History** - Track all salary payments for auditing  
✅ **Permission-Based** - Only users with `canManageSalaries` can manage salaries (CEO/CFO by default)

## Quick Setup

### 1. Set Payment Cycle

Choose how often salaries should be paid:

```
/company salary cycle YourCompany 1w
```

**Available Cycles:**
- `1h` - Every hour
- `24h` - Every 24 hours (daily)
- `1w` - Every week
- `2w` - Every two weeks
- `1m` - Every month (30 days)

### 2. Configure Job Salaries

Set salaries for each job title:

```
/company salary set YourCompany CEO 1000
/company salary set YourCompany CFO 800
/company salary set YourCompany EMPLOYEE 250
```

### 3. (Optional) Set Custom Player Salaries

Override job salaries for specific employees:

```
/company salary setplayer YourCompany Steve 1500
```

This is useful for rewarding top performers or adjusting compensation for senior employees.

### 4. View Configuration

Check your salary settings:

```
/company salary info YourCompany
```

## How It Works

1. **Automatic Payments**: The server checks all companies every 5 minutes
2. **Cycle Check**: If the payment cycle duration has elapsed, salaries are processed
3. **Balance Validation**: Company balance is checked before each payment
4. **Payment Execution**: 
   - Money is deducted from company balance
   - Money is added to employee's wallet
   - Payment is recorded in history
5. **Failure Handling**: If a company has insufficient balance, the payment fails but other employees and companies are not affected

## Salary Priority

When determining an employee's salary, the system checks in this order:

1. **Player-specific salary** (if set) ✅
2. **Job salary** (if set) ✅
3. **Default salary** from config (usually $0.00) ✅

## Permission Management

By default, only **CEO** and **CFO** roles have the `canManageSalaries` permission.

**To grant salary management to other roles:**

```
/company createjob YourCompany Manager invite,withdraw,salaries
```

or

```
/company editjob YourCompany Manager invite,withdraw,salaries
```

## Managing Salaries

### Remove Custom Salary

Reset a player back to their job salary:

```
/company salary removeplayer YourCompany Steve
```

### Reset Job Salary

Change a job's salary (same as `set`):

```
/company salary reset YourCompany EMPLOYEE 300
```

### View Payment History

Use the salary info command to see recent payment times:

```
/company salary info YourCompany
```

## Financial Planning

### Calculate Total Payroll

**Example Company:**
- CEO: $1,000
- CFO: $800
- 5 Employees: $250 each

**Weekly Cycle:**
- Total per payment: $3,050
- Recommended balance: $6,000+ (2 weeks buffer)

**Daily Cycle:**
- Total per payment: $3,050
- Recommended balance: $9,150+ (3 days buffer)

**Monthly Cycle:**
- Total per payment: $3,050
- Recommended balance: $6,000+ (2 months buffer)

### Best Practices

1. **Maintain Buffer**: Keep 2-3 payment cycles worth of balance
2. **Regular Deposits**: Encourage employees to deposit company revenue
3. **Monitor Balance**: Use `/company info YourCompany` to check balance
4. **Adjust Cycles**: Use longer cycles if revenue is inconsistent
5. **Custom Salaries**: Use sparingly to avoid complexity

## Troubleshooting

### Payments Not Processing

**Possible Causes:**
- Company balance is too low
- Payment cycle hasn't elapsed yet
- Company has no employees

**Solutions:**
1. Check company balance: `/company info YourCompany`
2. Check payment cycle: `/company salary info YourCompany`
3. Deposit funds: `/company deposit YourCompany <amount>`

### Employee Not Receiving Salary

**Check:**
1. Is the player still an employee? `/company employees YourCompany`
2. Is there a salary configured? `/company salary info YourCompany`
3. Does the company have sufficient balance?

### Want to Change Payment Frequency

Simply update the cycle:

```
/company salary cycle YourCompany 24h
```

The next payment will be scheduled based on the new cycle.

## Advanced Usage

### Database Queries

If you have direct database access, you can run analytics queries:

**Recent Payments:**
```sql
SELECT 
  c.name,
  csp.player_uuid,
  csp.amount,
  datetime(csp.payment_ts / 1000, 'unixepoch') as payment_time
FROM company_salary_payments csp
JOIN companies c ON csp.company_id = c.id
ORDER BY csp.payment_ts DESC
LIMIT 20;
```

**Payroll Summary:**
```sql
SELECT 
  c.name,
  c.balance,
  COUNT(ce.player_uuid) as employees,
  SUM(COALESCE(ces.salary_amount, cjs.salary_amount, 0)) as total_payroll
FROM companies c
LEFT JOIN company_employees ce ON c.id = ce.company_id
LEFT JOIN company_employee_salaries ces ON ce.company_id = ces.company_id AND ce.player_uuid = ces.player_uuid
LEFT JOIN company_job_salaries cjs ON ce.job_id = cjs.job_id
GROUP BY c.name;
```

## Configuration

Server admins can customize the salary system in `config.yml`:

```yaml
companies:
  salaries:
    paymentCycles:
      - 1h      # Every hour
      - 24h     # Daily
      - 1w      # Weekly
      - 2w      # Bi-weekly
      - 1m      # Monthly
    defaultJobSalary: 0.0  # Default if not configured
  
  permissionsByTitle:
    CEO:
      canManageSalaries: true
    CFO:
      canManageSalaries: true
```

## Command Reference

| Command | Description | Permission |
|---------|-------------|------------|
| `/company salary set <company> <job> <amount>` | Set job salary | `canManageSalaries` |
| `/company salary setplayer <company> <player> <amount>` | Set player salary | `canManageSalaries` |
| `/company salary removeplayer <company> <player>` | Remove player salary | `canManageSalaries` |
| `/company salary cycle <company> <cycle>` | Set payment cycle | `canManageSalaries` |
| `/company salary reset <company> <job> <amount>` | Reset job salary | `canManageSalaries` |
| `/company salary info <company>` | View salary info | Employee |

## Need Help?

- **Documentation**: See [Commands-Company.md](Documentation/Commands-Company.md) for detailed command reference
- **Configuration**: See [Configuration.md](Documentation/Configuration.md) for config options
- **Permissions**: See [Permissions.md](Documentation/Permissions.md) for permission details
- **Database**: See [Database.md](Documentation/Database.md) for database schema and queries

## Examples

### Small Startup

```bash
/company salary cycle StartupCo 24h
/company salary set StartupCo CEO 500
/company salary set StartupCo EMPLOYEE 100
```

**Daily cost**: $700 (CEO + 2 employees)

### Growing Business

```bash
/company salary cycle BusinessCorp 1w
/company salary set BusinessCorp CEO 2000
/company salary set BusinessCorp CFO 1500
/company salary set BusinessCorp Manager 800
/company salary set BusinessCorp EMPLOYEE 300
```

**Weekly cost**: $5,900 (CEO + CFO + 2 Managers + 5 employees)

### Large Corporation

```bash
/company salary cycle MegaCorp 1m
/company salary set MegaCorp CEO 10000
/company salary set MegaCorp CFO 8000
/company salary set MegaCorp Manager 3000
/company salary set MegaCorp EMPLOYEE 1000
/company salary setplayer MegaCorp Steve 12000  # Star employee
```

**Monthly cost**: $48,000+ (varies with employee count)

---

**Happy Managing! 🎉**
