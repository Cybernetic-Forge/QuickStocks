# Salaries

## Code-side scope

- Salary subcommands inside `CompanyCommand`
- Service: `SalaryService`
- Scheduler: `QuickStocksPlugin.startSalaryPaymentScheduler()`

## User and admin surface

- `/company salary`
- `/company salary set <company> <job> <amount>`
- `/company salary setplayer <company> <player> <amount>`
- `/company salary removeplayer <company> <player>`
- `/company salary cycle <company> <cycle>`
- `/company salary reset <company> <job> <amount>`
- `/company salary info <company>`

## Runtime behavior

- Salaries can be defined per job title or overridden per player.
- The service computes effective salary based on player override first, then job salary.
- Payment cycles are configurable per company.
- Salary processing runs on a scheduler and records payment history.
- The company config includes debt allowance specifically for salary payments.
- Offline payment behavior is configurable.

## Configuration

- `companies.yml`
  - `companies.salaries.paymentCycles`
  - `companies.salaries.defaultJobSalary`
  - `companies.salaries.offlinePayment`
  - `companies.allowedDebts.salaries`

## Legacy docs summarized

- `SALARY_FEATURE_GUIDE.md`
- Salary sections of `Commands-Company.md`
- Salary sections of `Configuration.md`
- Salary testing notes from `TESTING.md`

## Current caveats

- Salaries are implemented as a company-management sub-feature, not as a standalone plugin subsystem.
- Legacy docs tended to focus on command usage. The current code also includes scheduler-driven batch processing and payment-history queries that matter for operational understanding.
