# Market GUI Usage Guide

## Overview

The Market GUI replaces the previous chat-based `/market` command with a professional, interactive inventory interface. This provides a much more intuitive and visual way to trade stocks.

## How to Access

1. **Command**: Use `/market` in chat
2. **Market Device**: Right-click with a Market Link Device (if configured)

## Market GUI Layout

### Header Row
- **Portfolio Overview** (Book): Shows your financial summary
- **Wallet** (Gold Ingot): Displays current cash balance

### Stock Display
- **36 slots** showing the top gainers/stocks
- Each stock is represented by a **themed material**:
  - Technology: Redstone
  - Crypto: Gold Nugget
  - Energy: Coal
  - Finance: Emerald
  - Healthcare: Potion
  - Consumer: Apple
  - High performers (+5%): Diamond variants
  - Poor performers (-5%): Coal variants

### Stock Information
Each stock item shows:
- **Display Name** and **Symbol**
- **Current Price**
- **24h Change** (with ▲/▼ arrows and colors)
- **Volatility** percentage
- **Stock Type**

### Navigation Bar
- **Refresh** (Clock): Update market data
- **My Holdings** (Chest): Open portfolio view
- **Close** (Barrier): Exit the interface

## How to Trade

### Quick Trading
- **Left Click**: Buy 1 share immediately
- **Right Click**: Sell 1 share immediately

### Custom Amount Trading
- **Shift + Left Click**: Prompts for custom buy amount
- **Shift + Right Click**: Prompts for custom sell amount

### Trading Process
1. Click on a stock using the desired method
2. For quick trades: Transaction executes immediately if you have sufficient funds/shares
3. For custom amounts: Close GUI and use command `/market buy/sell SYMBOL AMOUNT`
4. Get immediate feedback on success/failure
5. Balance updates are shown in chat

## Portfolio GUI

Access via the "My Holdings" button in the Market GUI.

### Features
- **Account Summary**: Cash balance and portfolio value
- **Holdings Display**: All your stock positions
- **Performance Tracking**: Unrealized P&L with color coding
- **Quick Actions**: Right-click any holding to sell all shares

### Holdings Information
Each holding shows:
- **Stock Symbol**
- **Shares Owned**
- **Average Cost**
- **Current Price**
- **Total Value**
- **Unrealized P&L** (profit/loss) with percentage

## Visual Feedback

### Colors
- **Green**: Profitable/positive performance
- **Red**: Losing/negative performance
- **Yellow**: Neutral/informational
- **Gray**: Secondary information

### Materials
- **Diamond**: Exceptional performance
- **Emerald**: Good performance (finance stocks)
- **Gold**: Moderate performance (crypto)
- **Coal**: Poor performance
- **Redstone**: Technology stocks
- **Potion**: Healthcare stocks
- **Apple**: Consumer stocks

## Error Handling

The GUI includes comprehensive error handling:
- **Insufficient Funds**: Clear error messages
- **Insufficient Shares**: Shows available quantity
- **Price Data Issues**: Graceful fallback to chat
- **GUI Load Failures**: Automatic fallback to original chat interface

## Benefits Over Chat Interface

1. **Visual**: See all stocks at once
2. **Interactive**: Click to trade instantly
3. **Informative**: Rich tooltips with detailed information
4. **Intuitive**: No need to remember command syntax
5. **Professional**: Clean, organized presentation
6. **Efficient**: Quick actions for common trades
7. **Comprehensive**: All trading functions in one interface

## Technical Notes

- The GUI automatically refreshes when opened
- All existing `/market` commands still work for advanced users
- Market Device integration is seamless
- Portfolio updates are real-time
- Supports all existing stock types and data

This GUI transformation makes QuickStocks much more accessible and professional, providing a modern trading interface similar to real-world trading platforms.