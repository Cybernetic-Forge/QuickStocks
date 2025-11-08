# Crypto Enhancement - Testing Guide

## Overview
This document provides testing instructions for the crypto command enhancement feature.

## What Was Implemented

### 1. Personal Cryptocurrency Creation
- **Cost**: $500,000 (configurable in `crypto.yml`)
- **Command**: `/crypto create <symbol> <name>`
- **Requirements**:
  - Player must have sufficient balance ($500k)
  - Player must have `maksy.stocks.crypto.create` permission
  - Symbol must be unique and 2-10 characters

### 2. Company Cryptocurrency Creation
- **Requirements**: Company balance thresholds by type
  - PRIVATE: $100,000
  - PUBLIC: $250,000
  - DAO: $150,000
- **Command**: `/crypto company <company-name> <symbol> <name>`
- **Requirements**:
  - Player must have company management permission
  - Company must meet balance threshold for its type
  - Symbol must be unique and 2-10 characters

### 3. Configuration (crypto.yml)
New configuration file with comprehensive settings:
- Personal crypto creation cost and limits
- Company crypto balance thresholds by type
- Starting price, decimals, and trading limits

### 4. Database Changes
- Added `company_id` column to `instruments` table
- Migration V15 automatically runs on plugin startup

## Manual Testing Checklist

### Setup
- [ ] Install plugin on test server
- [ ] Verify crypto.yml is created in plugins/QuickStocks/
- [ ] Check logs for successful migration V15 execution

### Test Personal Crypto Creation

#### Test 1: Successful Creation
1. Give player $500,000: `/wallet set <player> 500000`
2. Create crypto: `/crypto create TESTCOIN "Test Coin"`
3. **Expected**: Success message showing:
   - Symbol: TESTCOIN
   - Cost: $500,000
   - Remaining balance
   - Instrument ID
4. **Verify**: Balance reduced by $500,000

#### Test 2: Insufficient Balance
1. Set player balance to $100,000: `/wallet set <player> 100000`
2. Try to create crypto: `/crypto create POORCOIN "Poor Coin"`
3. **Expected**: Error message about insufficient funds

#### Test 3: Duplicate Symbol
1. Create first crypto: `/crypto create DUPE "Dupe Coin 1"`
2. Try to create with same symbol: `/crypto create DUPE "Dupe Coin 2"`
3. **Expected**: Error about symbol already existing

#### Test 4: Invalid Symbol
1. Try symbol too short: `/crypto create A "Short"`
2. Try symbol too long: `/crypto create TOOLONGNAME "Long"`
3. Try special characters: `/crypto create TEST$ "Special"`
4. **Expected**: Error about symbol format

### Test Company Crypto Creation

#### Setup Company
1. Create test company: `/company create TestCorp PRIVATE`
2. Deposit funds: `/company deposit TestCorp 100000`

#### Test 5: Successful Company Crypto
1. Create company crypto: `/crypto company TestCorp TCOIN "TestCorp Coin"`
2. **Expected**: Success message showing:
   - Company name
   - Symbol: TCOIN
   - Company balance
   - Instrument ID

#### Test 6: Insufficient Company Balance
1. Create company with low balance: `/company create PoorCorp PRIVATE`
2. Deposit only $10,000: `/company deposit PoorCorp 10000`
3. Try to create crypto: `/crypto company PoorCorp PCOIN "Poor Coin"`
4. **Expected**: Error about insufficient company balance

#### Test 7: No Management Permission
1. Player A creates company
2. Player B (employee without management permission) tries:
   `/crypto company CompanyA BCOIN "B Coin"`
3. **Expected**: Error about lacking company management permission

#### Test 8: Company Type Thresholds
1. Create PUBLIC company: `/company create PublicCo PUBLIC`
2. Deposit $200,000: `/company deposit PublicCo 200000`
3. Try to create crypto: `/crypto company PublicCo PUBCOIN "Public Coin"`
4. **Expected**: Error (PUBLIC requires $250k)
5. Deposit more: `/company deposit PublicCo 50000`
6. Try again: `/crypto company PublicCo PUBCOIN "Public Coin"`
7. **Expected**: Success

### Test Trading Crypto

#### Test 9: View Crypto in Market
1. Create crypto (personal or company)
2. Use `/market` to open market browser
3. **Expected**: Crypto appears in listings

#### Test 10: Buy Crypto
1. Create crypto: `/crypto create BUYME "Buy Me Coin"`
2. Buy some: `/market buy BUYME 100`
3. **Expected**: Purchase successful at starting price ($1.00)

#### Test 11: Sell Crypto
1. After buying, sell some: `/market sell BUYME 50`
2. **Expected**: Sale successful

#### Test 12: View Crypto Details
1. Check crypto info: `/stocks BUYME`
2. **Expected**: Shows:
   - Price
   - Volume
   - Market cap
   - Creator info

### Test Configuration

#### Test 13: Modify Personal Cost
1. Edit `crypto.yml`: Set `personal.creationCost: 100000.0`
2. Restart server
3. Create crypto with $100k balance
4. **Expected**: Success with new cost

#### Test 14: Modify Company Threshold
1. Edit `crypto.yml`: Set `company.balanceThresholds.PRIVATE: 50000.0`
2. Restart server
3. Create PRIVATE company with $50k
4. Create crypto
5. **Expected**: Success with new threshold

#### Test 15: Disable Personal Crypto
1. Edit `crypto.yml`: Set `personal.enabled: false`
2. Restart server
3. Try `/crypto create TEST "Test"`
4. **Expected**: Error about personal crypto being disabled

#### Test 16: Disable Company Crypto
1. Edit `crypto.yml`: Set `company.enabled: false`
2. Restart server
3. Try `/crypto company <name> TEST "Test"`
4. **Expected**: Error about company crypto being disabled

#### Test 17: Max Per Player Limit
1. Edit `crypto.yml`: Set `personal.maxPerPlayer: 2`
2. Restart server
3. Create first crypto: Success
4. Create second crypto: Success
5. Try third crypto: **Expected**: Error about reaching limit

### Test Tab Completion

#### Test 18: Command Tab Completion
1. Type `/crypto ` and press TAB
2. **Expected**: Shows `create` and `company`
3. Type `/crypto company ` and press TAB
4. **Expected**: Shows company names

### Test Help Display

#### Test 19: Help Command
1. Run `/crypto` with no args
2. **Expected**: Shows:
   - `/crypto create` usage with cost
   - `/crypto company` usage
   - Examples
   - Permission status

## Configuration Testing

### Test crypto.yml Defaults
- personal.creationCost: 500000.0
- personal.maxPerPlayer: -1 (unlimited)
- company.balanceThresholds.PRIVATE: 100000.0
- company.balanceThresholds.PUBLIC: 250000.0
- company.balanceThresholds.DAO: 150000.0
- defaults.startingPrice: 1.0
- defaults.decimals: 8

### Test Database Migration
1. Check logs for: "Running migration V15__enhanced_crypto.sql"
2. Query database: `SELECT * FROM instruments WHERE company_id IS NOT NULL`
3. Verify company_id column exists

## Known Limitations
- External repositories may be unreachable in sandboxed environments (expected)
- Full plugin may not load in MockBukkit tests (database dependencies)
- Manual testing on real Minecraft server is required for full validation

## Troubleshooting

### Plugin Won't Load
- Check Java version (requires Java 21+)
- Verify all dependencies are present
- Check server logs for specific errors

### Migration Fails
- Backup database first
- Check database permissions
- Review migration logs

### Crypto Creation Fails
- Verify crypto.yml exists and is valid YAML
- Check player/company balance
- Verify permissions are set

### Crypto Not Tradeable
- Crypto should work automatically through existing market
- Check if symbol appears in `/market`
- Verify instrument was created in database

## Success Criteria
✅ Personal crypto creation requires and deducts $500k (configurable)
✅ Company crypto creation validates company balance thresholds
✅ Both personal and company crypto are tradeable in /market
✅ Configuration system works with crypto.yml
✅ Database migration runs successfully
✅ All error cases handled gracefully
✅ Help and tab completion work correctly
