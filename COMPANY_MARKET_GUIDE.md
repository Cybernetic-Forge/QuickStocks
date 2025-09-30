# Company Market Feature - Quick Start Guide

## Overview
Companies can now go on the market (IPO) and sell shares to players. This feature enables:
- Trading company shares on the market
- Player investment in companies
- Potential buyouts and ownership changes
- Market-based company valuation

## Quick Start

### For Company Owners

#### 1. Prepare Your Company
```
/company create MyCompany PUBLIC
/company deposit MyCompany 15000
```
Note: PUBLIC companies need $10,000 minimum, DAO companies need $15,000.

#### 2. Set a Trading Symbol
```
/company setsymbol MyCompany MYCO
```
Trading symbols must be 2-6 alphanumeric characters and unique.

#### 3. Go on the Market (IPO)
```
/company market enable MyCompany
```
Your company is now publicly traded!

#### 4. Configure Market Settings (Optional)
```
# View current settings
/company market settings MyCompany

# Change market percentage (how much of company is tradable)
/company market settings MyCompany percentage 60

# Enable/disable buyout protection
/company market settings MyCompany buyout true   # Allow buyouts
/company market settings MyCompany buyout false  # Prevent buyouts (default)
```

#### 5. Delist from Market
```
/company market disable MyCompany
```
All shareholders will be automatically paid out at current share price.

### For Investors/Players

#### 1. View Company Information
```
/company info MyCompany
/company shareholders MyCompany
```

#### 2. Buy Shares
```
/company buyshares MyCompany 100
```
This buys 100 shares at current market price.

#### 3. Sell Shares
```
/company sellshares MyCompany 50
```
This sells 50 shares back to the company at current market price.

#### 4. Check Your Notifications
```
/company notifications
```
See all market events, buyouts, and important company updates.

## How Share Pricing Works

- Each company has 10,000 shares
- Share price = Company Balance ÷ 10,000
- Example: If company has $50,000 balance, each share costs $5.00

## Market Percentage Explained

The market percentage controls how much of the company can be traded:
- **70% (default)**: 7,000 of 10,000 shares can be sold to players
- **50%**: 5,000 of 10,000 shares can be sold to players
- **100%**: All 10,000 shares can be sold (full company value)

This protects the company from being completely bought out unless the owner allows it.

## Buyout Protection

### Enabled (Default)
- Players cannot buy more than 50% of shares
- Owner retains control
- Safer for company owners

### Disabled
- Players can buy majority shares (>50%)
- Majority shareholder automatically becomes new owner and CEO
- All parties are notified
- Riskier but allows full market dynamics

## Important Notes

### For Company Owners
1. **Set a symbol before going public** - Required for market trading
2. **Maintain sufficient balance** - Company needs funds to buy back shares when players sell
3. **CEO protection** - You cannot demote yourself from CEO role
4. **Market requirements**:
   - Only PUBLIC and DAO companies can go on market
   - Minimum balance thresholds apply
   - PRIVATE companies cannot trade shares

### For Investors
1. **Share availability** - Limited by market percentage setting
2. **Buyout protection** - Check if company allows buyouts before investing
3. **Company balance** - Company needs sufficient balance to buy back your shares
4. **Notifications** - Important events are saved for offline players

## Example Scenarios

### Scenario 1: Safe Investment
```
Company: TechCorp (PUBLIC)
Balance: $100,000
Market Percentage: 70%
Buyout Protection: Enabled

Share Price: $10.00
Available Shares: 7,000
Max Ownership: 50% (5,000 shares)
```
Investors can buy up to 5,000 shares but cannot take over the company.

### Scenario 2: Buyout Opportunity
```
Company: StartupCo (DAO)
Balance: $50,000
Market Percentage: 100%
Buyout Protection: Disabled

Share Price: $5.00
Available Shares: 10,000
```
An investor can buy 5,001+ shares to become the new owner!

### Scenario 3: Market Exit
```
Owner: /company market disable TechCorp

Result:
- All shareholders paid out at $10.00/share
- Player with 100 shares receives $1,000
- Company removed from market
- All shareholders notified
```

## Configuration

Admins can configure market settings in `config.yml`:

```yaml
companies:
  marketableTypes:
    - PUBLIC
    - DAO
  marketBalanceThresholds:
    PUBLIC: 10000.0
    DAO: 15000.0
  defaultMarketPercentage: 70.0
```

## Commands Reference

| Command | Description |
|---------|-------------|
| `/company setsymbol <company> <symbol>` | Set trading symbol |
| `/company market enable <company>` | Go on market (IPO) |
| `/company market disable <company>` | Delist from market |
| `/company market settings <company>` | View/edit market settings |
| `/company buyshares <company> <qty>` | Buy shares |
| `/company sellshares <company> <qty>` | Sell shares |
| `/company shareholders <company>` | View all shareholders |
| `/company notifications` | View your notifications |

## Troubleshooting

**"Company type 'PRIVATE' cannot go on the market"**
- Solution: Only PUBLIC and DAO companies can trade shares

**"Company needs at least $10,000 balance to go on the market"**
- Solution: Deposit more funds before enabling market

**"Company must have a trading symbol set"**
- Solution: Use `/company setsymbol <company> <symbol>` first

**"Symbol is already taken"**
- Solution: Choose a different unique symbol

**"Cannot buy more than 50% of company"**
- Solution: Buyout protection is enabled. Owner must disable it first

**"Company has insufficient balance to buy back shares"**
- Solution: Company needs more balance to buy back your shares

## Support

For more information about the company system, see `COMPANY_FEATURE.md`.
