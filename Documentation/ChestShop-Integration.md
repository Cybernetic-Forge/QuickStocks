# ChestShop Integration Guide

## Overview

QuickStocks provides seamless integration with the ChestShop plugin, allowing companies to own and manage chest shops. This integration is fully optional and only activates when ChestShop is installed on the server.

## Features

### Company-Owned Chest Shops
- Companies can own chest shops instead of individual players
- Shop revenues go directly to company balance
- Shop purchases are deducted from company balance
- Minimum company balance threshold prevents shops with insufficient funds

### Employee Management
- Employees with `chestshop` permission can create and manage company chest shops
- Permission-based access control integrated with company job system
- Multiple employees can manage the same company's shops

### Soft Dependency
- Feature is completely invisible if ChestShop is not installed
- No configuration needed - automatic detection
- Plugin works normally without ChestShop present

## Configuration

Add to `config.yml` under the `companies` section:

```yaml
companies:
  enabled: true
  # ... other company settings ...
  
  # ChestShop Integration (soft-depends on ChestShop plugin)
  chestshop:
    enabled: true                # Enable company-owned chest shops
    companyMinBalance: 1000.0    # Minimum company balance for shop purchases
```

### Configuration Options

#### `enabled`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable or disable ChestShop integration
- **Note:** Only takes effect if ChestShop plugin is installed

#### `companyMinBalance`
- **Type:** Number (dollars)
- **Default:** `1000.0`
- **Description:** Minimum balance a company must have before customers can purchase items from its shops
- **Purpose:** Prevents shops from accepting purchases when company can't afford to pay suppliers

## Usage

### 1. Grant ChestShop Permission to Employees

When creating or editing job titles, include the `chestshop` permission:

```
/company createjob MyCompany Manager invite,chestshop
/company editjob MyCompany Employee chestshop
```

Permission format: `invite,createjobs,withdraw,manage,chestshop` (comma-separated)

### 2. Create a Company Chest Shop

As an employee with `chestshop` permission:

1. Place a chest
2. Place a sign above or next to it
3. Format the sign:
   ```
   Line 1: [Company Name]
   Line 2: [Quantity]
   Line 3: [B Price:S Price]
   Line 4: [Item Name]
   ```

Example:
```
TechCorp
64
B 10:S 8
Diamond
```

### 3. Shop Validation

When creating the sign, QuickStocks will:
- ✅ Verify the company exists
- ✅ Check if you're an employee with `chestshop` permission
- ✅ Validate company has minimum balance threshold
- ❌ Cancel sign creation if any check fails

### 4. Shop Transactions

Once created, the shop operates normally:
- **Customers buy items:** Money is added to company balance
- **Customers sell items:** Money is deducted from company balance
- **Insufficient funds:** Transaction is cancelled if company can't afford it

## Examples

### Example 1: Setting Up a Company Shop

```bash
# Step 1: Create the company
/company create TechCorp PUBLIC

# Step 2: Deposit funds to meet minimum threshold
/company deposit TechCorp 2000

# Step 3: Invite employees
/company invite TechCorp Steve Manager

# Step 4: Grant ChestShop permission to Manager role
/company editjob TechCorp Manager invite,chestshop

# Step 5: Steve can now create chest shops for TechCorp
```

### Example 2: Multiple Employees Managing Shops

```bash
# Create different roles with chestshop permission
/company createjob TechCorp ShopKeeper chestshop
/company createjob TechCorp Supplier chestshop,withdraw

# Assign employees to roles
/company invite TechCorp Alice ShopKeeper
/company invite TechCorp Bob Supplier

# Both Alice and Bob can now create and manage TechCorp's shops
```

### Example 3: Checking Company Shop Status

```bash
# View company balance
/company info TechCorp

# View employees with shop access
/company employees TechCorp

# View job permissions
/company jobs TechCorp
```

## Permissions

### Plugin Permissions
- `quickstocks.company.create` - Create companies (default: true)
- `quickstocks.company.manage` - Manage companies (default: true)

### Company Job Permissions
- `chestshop` - Can create and manage chest shops for the company

## Troubleshooting

### "Company does not have sufficient balance to create chest shops"
- **Cause:** Company balance is below `companyMinBalance` threshold
- **Solution:** Deposit more funds with `/company deposit <company> <amount>`

### "You do not have permission to create chest shops for company"
- **Cause:** Your job title doesn't have `chestshop` permission
- **Solution:** Ask company manager to update your job permissions

### "Company not found"
- **Cause:** Company name doesn't match exactly
- **Solution:** Check exact company name with `/company list`
- **Note:** Company names are case-sensitive

### ChestShop integration not working
- **Verify ChestShop is installed:** `/plugins` should show ChestShop
- **Check configuration:** Ensure `companies.chestshop.enabled: true`
- **Check logs:** Look for "Registered ChestShop integration listeners"

## Technical Details

### Database Schema
The feature adds a `can_manage_chestshop` column to the `company_jobs` table via migration `V10__chestshop_permission.sql`.

### Event Handling
1. **SignChangeEvent** - Validates company ownership and permissions when signs are placed
2. **ChestShop Transaction Events** - Handles money flow between customers and company balance

### Balance Management
- Company balance is updated directly without player wallet interaction
- Transactions are recorded in `company_tx` table with system UUID
- Type: `DEPOSIT` for sales, `WITHDRAW` for purchases

### Protection Integration
- Only employees with `chestshop` permission can modify company shops
- Integrated with ChestShop's protection system
- Inherits ChestShop's hopper, explosion, and theft protections

## Developer Notes

### Future Enhancements
The current implementation includes a framework for full ChestShop transaction handling. To complete the integration:

1. Add ChestShop API dependency to `pom.xml`
2. Implement ChestShop event handlers in `ChestShopTransactionListener.java`
3. Handle these events:
   - `TransactionEvent` - Buy/sell transactions
   - `PreTransactionEvent` - Transaction validation
   - `ProtectionCheckEvent` - Employee access control

See detailed notes in `ChestShopTransactionListener.java` for implementation details.

### API Access
The `ChestShopHook` class provides utility methods:
- `canManageShop(companyName, player)` - Check employee permissions
- `getCompany(companyName)` - Get company by name
- `addFunds(companyName, amount)` - Add money to company (for sales)
- `removeFunds(companyName, amount)` - Remove money from company (for purchases)
- `getBalance(companyName)` - Get current company balance

## FAQ

**Q: Can individual players still create ChestShops?**  
A: Yes! Player-owned shops work exactly as before. Company ownership is optional.

**Q: Can one company own multiple shops?**  
A: Yes, there's no limit to the number of shops a company can own.

**Q: Can employees from different companies manage each other's shops?**  
A: No, employees can only manage shops for companies they work for with proper permissions.

**Q: What happens if a company is disbanded?**  
A: Existing shops would need to be manually removed or transferred. This is a limitation of ChestShop.

**Q: Does this work with ChestShop's admin shops?**  
A: Admin shops (with blank owner line) work independently of this system.

**Q: Can I set different minimum balances for different companies?**  
A: Not currently. The `companyMinBalance` applies to all companies.

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review server logs for error messages
3. Open an issue on the QuickStocks GitHub repository
4. Include your QuickStocks and ChestShop versions

## See Also
- [Company System Documentation](./Commands-Company.md)
- [Company Permissions](./Permissions.md)
- [ChestShop Plugin](https://www.spigotmc.org/resources/chestshop.50/)
