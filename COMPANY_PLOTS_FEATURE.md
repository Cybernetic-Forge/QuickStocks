# Company Plots Feature

## Overview
The Company Plots feature allows companies to purchase, own, and manage chunks of land (16x16 blocks) in Minecraft. This feature includes configurable rent payments, automatic purchase modes, and debt management with plot seizure.

## Features

### 1. Plot Ownership
- Companies can purchase entire chunks (16x16 blocks)
- Each chunk can only be owned by one company
- Plots are stored with world name and chunk coordinates
- Purchase price is configurable

### 2. Plot Management
- **Buy plots**: `/company buyplot <company>` - Purchase the chunk you're standing in
- **Sell plots**: `/company sellplot <company>` - Sell the chunk you're standing in
- **List plots**: `/company plots <company>` - View all plots owned by a company
- Selling returns a configurable refund amount (default: 80% of purchase price)

### 3. Auto-Buy Mode
- **Enable**: `/company buyplot on <company>`
- **Disable**: `/company buyplot off`
- When enabled, automatically purchases unowned chunks as you walk through them
- Automatically disables if company runs out of funds
- Prevents duplicate purchases with player-chunk caching

### 4. Rent System
- Configurable rent amount per plot
- Rent interval options:
  - Hourly (`1h`)
  - Daily (`24h`)
  - Weekly (`1w`)
  - Monthly (`1m` - 30 days)
- Rent set to -1 means free (no rent)
- Rent collected automatically every 10 minutes
- Each plot tracks its last rent payment time

### 5. Debt Management
- Companies can go into debt for plot purchases and rent
- Configurable debt limits by category:
  - `chestshops`: Max debt for ChestShop transactions
  - `companyPlots`: Max debt for plot rent and purchases
  - `salaries`: Max debt for employee salaries
- When debt limit exceeded, plots are seized in order of purchase (oldest first)
- System automatically seizes plots until debt is within allowed limit

### 6. Permissions
- Only employees with `canManageCompany` permission can:
  - Buy plots
  - Sell plots
  - Enable/disable auto-buy mode
- By default: CEO has this permission

## Configuration

### companies.yml
```yaml
plots:
  enabled: true                # Enable/disable the entire plot system
  buyPlotPrice: 10000.0        # Cost to purchase a chunk/plot
  sellPlotPrice: 8000.0        # Refund amount when selling a plot (80% of buy price)
  plotRent: -1.0               # Default rent amount per interval (-1 = free, no rent)
  plotRentInterval: 'monthly'  # Rent payment frequency: hourly, daily, weekly, monthly

allowedDebts:
  chestshops: -5000.0          # Maximum negative balance allowed for ChestShop purchases
  companyPlots: -10000.0       # Maximum negative balance allowed for plot rent payments
  salaries: -3000.0            # Maximum negative balance allowed for salary payments
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/company buyplot <company>` | Buy the chunk you're standing in | `canManageCompany` |
| `/company buyplot on <company>` | Enable auto-buy mode | `canManageCompany` |
| `/company buyplot off` | Disable auto-buy mode | `canManageCompany` |
| `/company sellplot <company>` | Sell the chunk you're standing in | `canManageCompany` |
| `/company plots <company>` | List all plots owned by a company | Any employee |

## Database Schema

### company_plots
```sql
CREATE TABLE company_plots (
  id            TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  world_name    TEXT NOT NULL,
  chunk_x       INTEGER NOT NULL,
  chunk_z       INTEGER NOT NULL,
  buy_price     REAL NOT NULL,
  purchased_at  INTEGER NOT NULL,
  rent_amount   REAL NOT NULL DEFAULT -1,
  rent_interval TEXT NOT NULL DEFAULT 'monthly',
  last_rent_payment INTEGER,
  UNIQUE(world_name, chunk_x, chunk_z)
);
```

### player_auto_buy_mode
```sql
CREATE TABLE player_auto_buy_mode (
  player_uuid   TEXT PRIMARY KEY,
  company_id    TEXT NOT NULL,
  enabled       INTEGER NOT NULL DEFAULT 0
);
```

### Transaction Types
New transaction types in `company_tx`:
- `PLOT_PURCHASE`: When a plot is bought
- `PLOT_SALE`: When a plot is sold
- `PLOT_RENT`: When rent is paid

## Implementation Details

### Services
1. **CompanyPlotService**: Main service for plot management
   - `buyPlot()`: Purchase a plot
   - `sellPlot()`: Sell a plot
   - `getPlotByLocation()`: Check plot ownership
   - `getCompanyPlots()`: Get all plots for a company
   - `processRentCollection()`: Automated rent collection
   - `setAutoBuyMode()`: Toggle auto-buy mode
   - `collectRent()`: Collect rent for a single plot
   - `handleUnpaidRent()`: Seize plots when debt exceeded

2. **CompanyService** (Updated):
   - `removeWithDebtAllowance()`: Deduct funds with debt limit checks
   - Supports different debt categories

### Listeners
1. **CompanyPlotListener**: Handles auto-buy functionality
   - Monitors player movement between chunks
   - Automatically purchases plots when auto-buy enabled
   - Caches last chunk to prevent duplicates
   - Disables auto-buy when funds insufficient

### Scheduled Tasks
- **Rent Collection**: Runs every 10 minutes
  - Checks all plots with rent enabled
  - Collects rent if payment is due
  - Seizes plots if company in excessive debt

## Example Usage

### Basic Purchase
```
/company create MyCorp PRIVATE
/company deposit MyCorp 15000
/company buyplot MyCorp
```

### Auto-Buy Mode
```
/company buyplot on MyCorp
# Walk around - plots automatically purchased
/company buyplot off
```

### Rent Configuration
To enable rent, edit `companies.yml`:
```yaml
plots:
  plotRent: 100.0
  plotRentInterval: 'daily'
```

Then reload: `/reload confirm`

### Check Plots
```
/company plots MyCorp
```

## Error Messages

| Message | Cause | Solution |
|---------|-------|----------|
| "This plot is already owned" | Chunk already owned by a company | Choose different chunk |
| "Insufficient company funds" | Not enough balance or exceeds debt limit | Deposit more funds |
| "You don't have permission" | Player lacks `canManageCompany` | Get promoted to CEO or CFO |
| "Plot system is not enabled" | Plots disabled in config | Enable in companies.yml |
| "This plot is not owned by your company" | Trying to sell another company's plot | Only sell your own plots |

## Integration

### With Salary System
- Both plots and salaries can cause debt
- Each has separate debt allowance
- Combined debt management ensures company solvency

### With ChestShop
- ChestShop transactions can also cause debt
- Separate debt allowance for shop purchases
- All systems respect overall debt limits

## Performance Considerations

1. **Rent Collection**: Runs every 10 minutes, processes only plots with due rent
2. **Auto-Buy**: Uses chunk caching to prevent duplicate checks
3. **Plot Queries**: Indexed by world_name, chunk_x, chunk_z for fast lookups
4. **Debt Checks**: Efficient balance checks before transactions

## Future Enhancements

Potential improvements for future versions:
- Plot permissions system (who can build/break blocks)
- Plot visualization (borders, markers)
- Plot grouping/regions
- Plot transfer between companies
- Plot leasing (temporary ownership)
- Plot management GUI
- Plot statistics and analytics
- Customizable plot sizes (not just chunks)
- Plot protection integration with WorldGuard/GriefPrevention

## Troubleshooting

### Rent not being collected
1. Check `plotRent` is not -1 (free)
2. Verify rent interval has passed since last payment
3. Check server console for rent collection logs
4. Ensure plots system is enabled in config

### Auto-buy not working
1. Verify auto-buy mode is enabled (check for confirmation message)
2. Ensure player has `canManageCompany` permission
3. Check company has sufficient funds
4. Verify plots system is enabled

### Plots seized unexpectedly
1. Check company balance is above debt threshold
2. Review `allowedDebts.companyPlots` setting
3. Check rent collection logs in console
4. Verify rent amount and interval settings

## API Usage

For plugin developers:

```java
// Get plot service
CompanyPlotService plotService = QuickStocksPlugin.getCompanyPlotService();

// Check if location is owned
Optional<CompanyPlot> plot = plotService.getPlotByLocation(worldName, chunkX, chunkZ);

// Get all plots for a company
List<CompanyPlot> plots = plotService.getCompanyPlots(companyId);

// Process rent collection manually
plotService.processRentCollection();
```

## Credits

Feature implemented as part of QuickStocks company management system.

## See Also

- [Testing Guide](PLOT_FEATURE_TESTING.md) - Comprehensive testing instructions
- [Company System](SALARY_FEATURE_GUIDE.md) - General company features
- [Configuration](companies.yml) - Full configuration options
