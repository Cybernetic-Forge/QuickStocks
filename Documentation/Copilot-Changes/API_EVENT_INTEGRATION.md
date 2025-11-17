# API Event Integration - Implementation Summary

## Overview
This document describes the complete integration of API events into the QuickStocks plugin, as requested in the issue "API-Event integration".

## Changes Made

### 1. Event System Refactoring

#### Created TransactionType Enum
**File**: `src/main/java/net/cyberneticforge/quickstocks/api/events/TransactionType.java`

```java
public enum TransactionType {
    INSTRUMENT,  // Trading standard instruments (items, crypto, etc.)
    SHARE,       // Trading company shares
    CRYPTO       // Trading cryptocurrency (custom or standard)
}
```

#### Unified ShareBuyEvent and ShareSellEvent
**Files**: 
- `src/main/java/net/cyberneticforge/quickstocks/api/events/ShareBuyEvent.java`
- `src/main/java/net/cyberneticforge/quickstocks/api/events/ShareSellEvent.java`

These events now support all transaction types through the `TransactionType` enum:
- `assetId` - Can be instrumentId, companyId, or cryptoId
- `assetSymbol` - Symbol or display name for the asset
- `transactionType` - Distinguishes between INSTRUMENT, SHARE, and CRYPTO

**Legacy Support**: Deprecated constructors maintained for backward compatibility.

#### Removed InstrumentBuyEvent
**File**: Deleted `src/main/java/net/cyberneticforge/quickstocks/api/events/InstrumentBuyEvent.java`

This event was deprecated in favor of the unified `ShareBuyEvent` with `TransactionType.INSTRUMENT`.

#### Added getHandlerList() to All Events
All event classes now properly implement the static `getHandlerList()` method required by Bukkit's event system.

### 2. Service Integration

#### TradingService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/market/TradingService.java`

**Events Integrated**:
- `ShareBuyEvent` - Fires before buy order execution
- `ShareSellEvent` - Fires before sell order execution

**Implementation Details**:
- Events fire BEFORE wallet deduction and holdings update
- Both events are cancellable
- Uses `TransactionType.INSTRUMENT`
- Includes instrument symbol for display

**Example Usage**:
```java
ShareBuyEvent event = new ShareBuyEvent(
    player,
    TransactionType.INSTRUMENT,
    instrumentId,
    symbol,
    qty,
    currentPrice,
    totalCost
);
Bukkit.getPluginManager().callEvent(event);
if (event.isCancelled()) {
    return new TradeResult(false, "Trade cancelled by event handler");
}
```

#### WalletService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/portfolio/WalletService.java`

**Events Integrated**:
- `WalletBalanceChangeEvent` - Fires after balance changes

**Implementation Details**:
- Fires AFTER successful balance modification
- Non-cancellable (post-action event)
- Includes old balance, new balance, and change reason
- Integrated in `addBalance()` and `removeBalance()` methods

#### WatchlistService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/portfolio/WatchlistService.java`

**Events Integrated**:
- `WatchlistAddEvent` - Fires before adding to watchlist
- `WatchlistRemoveEvent` - Fires before removing from watchlist

**Implementation Details**:
- Both events fire BEFORE database modification
- Both events are cancellable
- Helper method `getInstrumentSymbol()` added to fetch symbols

#### CompanyService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/companies/CompanyService.java`

**Events Integrated**:
- `CompanyCreateEvent` - Fires before company creation
- `CompanyEmployeeLeaveEvent` - Fires after employee removal

**Implementation Details**:
- `CompanyCreateEvent`: Fires before charging creation cost, cancellable
- `CompanyEmployeeLeaveEvent`: Fires after removal, includes `wasKicked` flag
- Integrated in `createCompany()`, `removeEmployee()`, and `fireEmployee()` methods

#### InvitationService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/companies/InvitationService.java`

**Events Integrated**:
- `CompanyEmployeeJoinEvent` - Fires after employee joins

**Implementation Details**:
- Fires AFTER employee is added to database
- Non-cancellable (post-action event)
- Includes company name and job title
- Integrated in `acceptInvitation()` method

#### CryptoService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/market/CryptoService.java`

**Events Integrated**:
- `CryptoCreateEvent` - Fires before crypto creation

**Implementation Details**:
- Fires BEFORE balance checks and crypto creation
- Cancellable to prevent unauthorized crypto creation
- Integrated in `createCustomCrypto()` method

#### CompanyMarketService
**File**: `src/main/java/net/cyberneticforge/quickstocks/core/services/features/market/CompanyMarketService.java`

**Events Integrated**:
- `CompanyIPOEvent` - Fires before company goes public

**Implementation Details**:
- Fires BEFORE creating instrument and enabling market
- Cancellable to prevent IPO
- Integrated in `enableMarket()` method

## Events Not Yet Integrated

The following events exist in the API but are not yet connected to active systems:

### InstrumentPriceUpdateEvent
**Status**: Awaiting price simulation/update system integration
**Required**: Integration with market price update scheduler

### CircuitBreakerTriggeredEvent
**Status**: CircuitBreakerService exists but not actively triggered
**Required**: Integration with trading halt logic

### MarketOpenEvent / MarketCloseEvent
**Status**: Awaiting market scheduling system
**Required**: Market hours scheduler implementation

These events can be integrated when their respective systems become active.

## Implementation Patterns

### Cancellable Events (Pre-Action)
Events that fire BEFORE critical operations and can be cancelled:

```java
try {
    Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
    if (player != null) {
        MyEvent event = new MyEvent(player, ...);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            throw new IllegalArgumentException("Action cancelled by event handler");
        }
    }
} catch (IllegalArgumentException e) {
    throw e; // Rethrow cancellation
} catch (Exception e) {
    logger.debug("Could not fire event: " + e.getMessage());
}
```

### Non-Cancellable Events (Post-Action)
Events that fire AFTER operations complete:

```java
try {
    Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
    if (player != null) {
        MyEvent event = new MyEvent(player, ...);
        Bukkit.getPluginManager().callEvent(event);
    }
} catch (Exception e) {
    logger.debug("Could not fire event: " + e.getMessage());
}
```

## Breaking Changes

### InstrumentBuyEvent Removed
The `InstrumentBuyEvent` class has been removed. Use `ShareBuyEvent` with `TransactionType.INSTRUMENT` instead:

**Before**:
```java
InstrumentBuyEvent event = new InstrumentBuyEvent(player, instrumentId, symbol, qty, price, totalCost);
```

**After**:
```java
ShareBuyEvent event = new ShareBuyEvent(player, TransactionType.INSTRUMENT, instrumentId, symbol, qty, price, totalCost);
```

### getHandlerList() Required
All custom event listeners must now properly handle the static `getHandlerList()` method.

## Testing Recommendations

### Unit Tests
Create tests for:
1. Event cancellation behavior
2. Event data accuracy
3. Service behavior when events are cancelled
4. Event firing in error conditions

### Integration Tests
Verify:
1. Events fire at correct times in the workflow
2. Multiple event listeners can coexist
3. Event cancellation properly prevents actions
4. Event data is accessible to listeners

### Manual Testing
Test scenarios:
1. Normal trading operations with event listeners
2. Event cancellation via custom plugins
3. Concurrent operations with multiple players
4. Error recovery when event firing fails

## API for Plugin Developers

### Listening to Events

```java
@EventHandler
public void onShareBuy(ShareBuyEvent event) {
    Player buyer = event.getBuyer();
    TransactionType type = event.getTransactionType();
    
    if (type == TransactionType.INSTRUMENT) {
        // Handle instrument purchase
    } else if (type == TransactionType.SHARE) {
        // Handle company share purchase
    }
    
    // Cancel if needed
    if (shouldPreventTrade(buyer)) {
        event.setCancelled(true);
    }
}
```

### Available Events

| Event | Type | Cancellable | When Fired |
|-------|------|-------------|------------|
| ShareBuyEvent | Trading | Yes | Before buy execution |
| ShareSellEvent | Trading | Yes | Before sell execution |
| WalletBalanceChangeEvent | Portfolio | No | After balance change |
| WatchlistAddEvent | Portfolio | Yes | Before adding to watchlist |
| WatchlistRemoveEvent | Portfolio | Yes | Before removing from watchlist |
| CompanyCreateEvent | Company | Yes | Before company creation |
| CompanyIPOEvent | Company | Yes | Before company IPO |
| CompanyEmployeeJoinEvent | Company | No | After employee joins |
| CompanyEmployeeLeaveEvent | Company | No | After employee leaves/fired |
| CryptoCreateEvent | Market | Yes | Before crypto creation |

## Migration Guide

### For Event Listeners
If you were listening to `InstrumentBuyEvent`:

```java
// OLD
@EventHandler
public void onInstrumentBuy(InstrumentBuyEvent event) {
    // ...
}

// NEW
@EventHandler
public void onShareBuy(ShareBuyEvent event) {
    if (event.getTransactionType() == TransactionType.INSTRUMENT) {
        // Same logic
    }
}
```

### For Event Callers
If you were calling `InstrumentBuyEvent`:

```java
// OLD
InstrumentBuyEvent event = new InstrumentBuyEvent(player, id, symbol, qty, price, cost);
Bukkit.getPluginManager().callEvent(event);

// NEW
ShareBuyEvent event = new ShareBuyEvent(player, TransactionType.INSTRUMENT, id, symbol, qty, price, cost);
Bukkit.getPluginManager().callEvent(event);
```

## Future Enhancements

1. **Price Update Events**: Integrate `InstrumentPriceUpdateEvent` when price simulation system is active
2. **Circuit Breaker Events**: Connect `CircuitBreakerTriggeredEvent` to trading halt logic
3. **Market Schedule Events**: Implement market hours with `MarketOpenEvent` and `MarketCloseEvent`
4. **Event Priorities**: Consider adding priority levels for critical listeners
5. **Event Metrics**: Track event firing frequency and cancellation rates
6. **Event Documentation**: Generate API docs for event classes

## Conclusion

All major API events have been successfully integrated into their respective service layers. The event system now provides comprehensive hooks for plugin developers to monitor and control all critical operations in QuickStocks. The unified `ShareBuyEvent`/`ShareSellEvent` design simplifies the API while maintaining flexibility through the `TransactionType` enum.
