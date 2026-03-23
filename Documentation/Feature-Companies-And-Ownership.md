# Companies And Ownership

## Code-side scope

- Command: `CompanyCommand`
- Services: `CompanyService`, `CompanyMarketService`, `InvitationService`
- GUIs and listeners: `CompanySettingsGUI`, `CompanyEmployeesGUI`, `CompanyJobsGUI`, `CompanyJobEditGUI` and their listeners

## User and admin surface

- `/company create`
- `/company info`
- `/company list`
- `/company invite`, `/company accept`, `/company decline`, `/company invitations`
- `/company deposit`, `/company withdraw`
- `/company employees`, `/company jobs`
- `/company createjob`, `/company editjob`, `/company assignjob`
- `/company settings`
- `/company setsymbol`
- `/company market enable|disable|settings`
- `/company notifications`
- `/company leave`
- `/company transferownership`
- `/company fire`

## Runtime behavior

- `CompanyService` handles company creation, membership, treasury operations, debt-aware balance removal, job creation/editing, employee lookups, ownership transfer, and employee removal.
- `CompanyMarketService` handles company listing, symbol assignment, IPO enable/disable, market settings, share issuance logic, share trading, shareholder queries, and company notifications.
- Company GUIs expose company information, treasury operations, jobs, employees, and market controls.
- Marketable company types are configured, and IPO-related defaults such as market percentage are managed through company configuration.

## Configuration

- `companies.yml`
  - `companies.enabled`
  - `companies.creationCost`
  - `companies.defaultTypes`
  - `companies.defaultJobTitles`
  - `companies.permissionsByTitle.*`
  - `companies.marketableTypes` and company market defaults via `CompanyCfg`
- `guis.yml`
  - `company_settings.*`
  - `company_employees.*`
  - `company_jobs.*`

## Legacy docs summarized

- `Commands-Company.md`
- Company sections of `Getting-Started.md`
- Company sections of `Configuration.md`
- Company sections of `Permissions.md`
- Company market and ownership notes from `IMPLEMENTATION_SUMMARY.md`

## Current caveats

- Legacy docs often described company operations and company market operations as separate systems. In the codebase they are tightly coupled: `/company market ...` is the IPO/shareholder entrypoint while `/market` is the investor-facing trading entrypoint.
- Company features are broad and implemented, but several flows rely on GUI navigation rather than dedicated command-only coverage.
