# Watchlist Implementation

This document describes the watchlist functionality implemented for the QuickStocks plugin.

## Overview

The watchlist feature allows players to track their favorite instruments (stocks, crypto, items) for quick access and monitoring. Players can add/remove instruments from their personal watchlist and view real-time price data for watched items.

## Commands

### `/watch` Command

The main watchlist management command with the following subcommands:

- `/watch` - Show watchlist overview with current prices and changes
- `/watch add <symbol>` - Add an instrument to your watchlist
- `/watch remove <symbol>` - Remove an instrument from your watchlist  
- `/watch list` - Same as `/watch`, shows watchlist overview
- `/watch info <symbol>` - Show detailed information for a watchlisted symbol
- `/watch clear` - Clear all items from your watchlist

**Aliases:** `/watchlist`, `/wl`

**Tab Completion:** Smart completion for subcommands and instrument symbols

### Market Integration

The `/market` command now includes watchlist integration:

- `/market watchlist` - Show watchlist summary
- Market overview displays ★ indicator for watchlisted items
- `/market browse` shows which instruments are in your watchlist

## Database Schema

### `user_watchlists` Table

```sql
CREATE TABLE user_watchlists (
  player_uuid   TEXT NOT NULL,
  instrument_id TEXT NOT NULL,
  added_at      INTEGER NOT NULL,
  PRIMARY KEY (player_uuid, instrument_id),
  FOREIGN KEY (instrument_id) REFERENCES instruments(id) ON DELETE CASCADE
);
```

**Indexes:**
- `idx_watchlists_player` on `player_uuid`
- `idx_watchlists_added` on `player_uuid, added_at`

## Service Layer

### WatchlistService

Core service class handling all watchlist operations:

**Methods:**
- `addToWatchlist(playerUuid, instrumentId)` - Add instrument to watchlist
- `removeFromWatchlist(playerUuid, instrumentId)` - Remove from watchlist
- `isInWatchlist(playerUuid, instrumentId)` - Check membership
- `getWatchlist(playerUuid)` - Get full watchlist with market data
- `getWatchlistCount(playerUuid)` - Get count of watchlisted items
- `clearWatchlist(playerUuid)` - Remove all items from watchlist

**Data Model:**
- `WatchlistItem` class containing instrument data + market state
- Includes real-time price, 24h/1h changes, volatility
- Sorted by date added (newest first)

## Features

### Real-time Market Data
- Watchlist displays current prices, 24h changes, and 1h changes
- Color-coded indicators: green for gains, red for losses
- Unicode arrows (▲/▼) for visual change direction

### Duplicate Prevention
- Prevents adding the same instrument multiple times
- Clear feedback when attempting duplicate additions

### Integration Points
- Market command shows watchlist indicators (★)
- Tab completion suggests available instrument symbols
- Consistent error handling and user feedback

### Quality of Life Features
- Multiple command aliases for convenience
- Detailed info view for individual watchlist items
- Bulk clear functionality
- Informative help messages

## Testing

The implementation includes comprehensive testing:

- **WatchlistStandaloneDemo** - Standalone test validating all core functionality
- Tests cover: empty watchlist, additions, removals, duplicates, membership checks, clearing
- All tests pass with realistic market data simulation

## Usage Examples

```
# Add instruments to watchlist
/watch add MC_STONE
/watch add AAPL

# View your watchlist
/watch
# Shows:
# Watching 2 instruments:
# AAPL (Apple Inc.): $150.25 ▲2.45% | 1h: +0.75%
# MC_STONE (Stone): $12.50 ▼1.20% | 1h: -0.30%

# Get detailed info
/watch info AAPL
# Shows detailed instrument information including when added

# Remove from watchlist
/watch remove MC_STONE

# View watchlist from market command
/market watchlist

# Browse market with watchlist indicators
/market browse
# Shows ★ next to watchlisted items
```

## Integration with Existing Systems

### QueryService Extensions
Added methods to support watchlist functionality:
- `getInstrumentDisplayName(instrumentId)` - Get display name by ID
- `getInstrumentSymbols()` - Get all symbols for tab completion

### MarketCommand Integration
- Added watchlist subcommand
- Enhanced market overview with watchlist indicators
- Updated tab completion to include watchlist option

### Plugin Registration
- Watchlist service initialized in plugin startup
- Commands properly registered with Bukkit
- Service dependencies injected correctly

## Future Enhancements

Potential improvements for future versions:

1. **GUI Integration** - Add watchlist panel to existing Market GUI
2. **Alerts** - Price alerts when watchlisted items hit target prices
3. **Sorting Options** - Sort watchlist by price, change, alphabetical
4. **Categories** - Group watchlist items by type (stocks, crypto, items)
5. **Import/Export** - Share watchlists between players
6. **Notifications** - Chat notifications for significant price movements
7. **Historical Tracking** - Track performance since watchlist addition

## Performance Considerations

- Efficient database queries with proper indexing
- Minimal impact on existing market operations
- Lazy loading of watchlist data only when needed
- Prepared statements prevent SQL injection
- Connection pooling through existing DatabaseManager

## Migration

The watchlist feature uses migration `V3__watchlists.sql` which:
- Creates the `user_watchlists` table
- Adds proper foreign key constraints
- Creates performance indexes
- Is safe to run on existing databases