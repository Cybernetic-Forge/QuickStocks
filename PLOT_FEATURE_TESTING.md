# Company Plots Feature - Testing Guide

## Overview
This document outlines how to test the company plots feature, which allows companies to own chunks of land with configurable rent and debt management.

## Prerequisites
1. Server running Paper 1.21.8
2. QuickStocks plugin installed
3. A player with permission to create companies
4. Sufficient funds to create a company and buy plots

## Configuration
The plot system is configured in `companies.yml`:

```yaml
plots:
  enabled: true                # Enable/disable plot system
  buyPlotPrice: 10000.0        # Cost to purchase a chunk/plot
  sellPlotPrice: 8000.0        # Refund when selling (80% of buy price)
  plotRent: -1.0               # Rent per interval (-1 = free)
  plotRentInterval: 'monthly'  # hourly, daily, weekly, monthly

allowedDebts:
  chestshops: -5000.0          # Max debt for ChestShop purchases
  companyPlots: -10000.0       # Max debt for plot rent
  salaries: -3000.0            # Max debt for salary payments
```

## Test Cases

### 1. Basic Plot Purchase
**Steps:**
1. Create a company: `/company create TestCorp PRIVATE`
2. Deposit funds: `/company deposit TestCorp 15000`
3. Stand in a chunk you want to purchase
4. Buy the plot: `/company buyplot TestCorp`
5. Verify purchase message shows location and price

**Expected Result:**
- Plot purchased successfully
- Company balance reduced by `buyPlotPrice`
- Success message displayed with chunk coordinates

### 2. Plot Already Owned
**Steps:**
1. Try to buy the same plot again: `/company buyplot TestCorp`

**Expected Result:**
- Error message: "This plot is already owned by another company"

### 3. Insufficient Funds
**Steps:**
1. Create a new company with insufficient funds
2. Try to buy a plot

**Expected Result:**
- Error message showing required amount and available balance

### 4. Sell Plot
**Steps:**
1. Stand in a plot owned by your company
2. Sell the plot: `/company sellplot TestCorp`
3. Check company balance

**Expected Result:**
- Plot sold successfully
- Company balance increased by `sellPlotPrice`
- Plot removed from ownership

### 5. List Company Plots
**Steps:**
1. Buy several plots in different locations
2. List all plots: `/company plots TestCorp`

**Expected Result:**
- List shows all owned plots with coordinates
- Rent information displayed for each plot

### 6. Auto-Buy Mode
**Steps:**
1. Enable auto-buy: `/company buyplot on TestCorp`
2. Walk into unowned chunks
3. Observe automatic purchases

**Expected Result:**
- Auto-buy mode enabled message
- Plots automatically purchased as you enter new chunks
- Purchase messages for each plot

**Disable:**
- `/company buyplot off`

### 7. Auto-Buy with Insufficient Funds
**Steps:**
1. Enable auto-buy mode
2. Ensure company has low balance
3. Walk into a new chunk

**Expected Result:**
- Auto-buy mode automatically disabled
- Error message about insufficient funds

### 8. Rent Collection (with rent enabled)
**Prerequisites:**
- Set `plotRent: 100.0` and `plotRentInterval: hourly` in config
- Reload plugin or restart server

**Steps:**
1. Buy a plot
2. Wait for rent collection cycle (10 minutes)
3. Check company balance

**Expected Result:**
- Company balance reduced by rent amount
- Transaction logged in company_tx table

### 9. Debt Management - Plot Seizure
**Prerequisites:**
- Set `plotRent: 100.0` and `allowedDebts.companyPlots: -500.0`

**Steps:**
1. Buy multiple plots
2. Withdraw most company funds
3. Wait for rent collection cycles
4. Company goes into debt beyond allowed limit

**Expected Result:**
- Plots seized in order of purchase (oldest first)
- Plots removed until debt is within allowed limit
- Console logs plot seizures

### 10. Permission Checks
**Steps:**
1. Invite a player as EMPLOYEE (no manage permission)
2. Have them try to buy/sell plots

**Expected Result:**
- Error message: "You don't have permission to buy/sell plots"

### 11. Tab Completion
**Steps:**
1. Type `/company buyplot ` and press TAB
2. Type `/company plots ` and press TAB

**Expected Result:**
- Suggests company names
- For buyplot: also suggests "on" and "off"

## Database Verification

### Check Plots Table
```sql
SELECT * FROM company_plots;
```

**Columns:**
- id, company_id, world_name, chunk_x, chunk_z
- buy_price, purchased_at
- rent_amount, rent_interval, last_rent_payment

### Check Transactions
```sql
SELECT * FROM company_tx WHERE type LIKE 'PLOT%' ORDER BY ts DESC LIMIT 10;
```

**Transaction Types:**
- PLOT_PURCHASE
- PLOT_SALE
- PLOT_RENT

### Check Auto-Buy Mode
```sql
SELECT * FROM player_auto_buy_mode;
```

## Integration Tests

### With Salary System
1. Configure salaries with allowed debt
2. Set plot rent
3. Verify both systems can cause debt independently

### With ChestShop Integration
1. Create company-owned chest shops
2. Buy plots
3. Verify both use debt allowance correctly

## Performance Testing

### Rent Collection at Scale
1. Create multiple companies
2. Each company buys 10+ plots
3. Enable rent on all plots
4. Monitor rent collection cycle performance

**Expected:**
- Rent collection completes within reasonable time
- No server lag
- All rents collected accurately

### Auto-Buy Performance
1. Enable auto-buy mode
2. Fly/walk through many chunks quickly
3. Monitor plugin performance

**Expected:**
- No duplicate purchases
- No race conditions
- Player not spammed with messages

## Known Issues / Notes

1. **Auto-buy cache:** The listener caches the last chunk per player to avoid duplicate purchases when moving within the same chunk.

2. **Rent collection frequency:** Runs every 10 minutes. For testing, you may want to adjust this in QuickStocksPlugin.java.

3. **Debt threshold:** When a company reaches the debt threshold, plots are seized immediately during the next rent collection cycle.

4. **Plot ownership verification:** Always check that the chunk is not already owned before allowing purchase.

## Debugging

### Enable Debug Logging
In `config.yml`:
```yaml
logging:
  debugLevel: 2  # or 3 for trace
```

### Useful Commands
- Check company balance: `/company info TestCorp`
- View transactions: Check database `company_tx` table
- List all plots: `/company plots TestCorp`

### Common Issues

**"Company not found"**
- Verify company name spelling
- Check if company exists: `/company list`

**"Insufficient funds"**
- Check company balance: `/company info <company>`
- Consider debt allowance settings

**Auto-buy not working**
- Verify plots are enabled in config
- Check player has manage permission
- Ensure auto-buy mode is enabled: look for confirmation message

**Rent not being collected**
- Check rent_amount is not -1 (free)
- Verify rent collection scheduler is running (check console logs)
- Ensure enough time has passed based on rent_interval

## Success Criteria

All test cases should pass with:
- ✅ Plots can be purchased and sold
- ✅ Auto-buy mode works correctly
- ✅ Rent collection processes successfully
- ✅ Debt management enforces limits
- ✅ Plot seizure happens when debt exceeded
- ✅ Permissions are respected
- ✅ Tab completion works
- ✅ Transactions are logged
- ✅ No server errors or lag
