# QuickStocks API Documentation

This document describes the QuickStocks API for external plugin integration.

## Overview

The QuickStocks API provides a comprehensive interface for external plugins to interact with the QuickStocks plugin. It includes:

- **7 Manager Classes** - High-level interfaces for different domains
- **17 Custom Events** - Bukkit events that can be listened to

## Getting Started

### Adding QuickStocks as a Dependency

#### Maven
```xml
<dependencies>
    <dependency>
        <groupId>net.cyberneticforge</groupId>
        <artifactId>QuickStocks</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### plugin.yml
```yaml
name: YourPlugin
version: 1.0
depend: [QuickStocks]
```

### Accessing the API

```java
import net.cyberneticforge.quickstocks.api.QuickStocksAPI;
import org.bukkit.plugin.Plugin;

public class YourPlugin extends JavaPlugin {
    
    private QuickStocksAPI quickStocksAPI;
    
    @Override
    public void onEnable() {
        Plugin quickStocks = getServer().getPluginManager().getPlugin("QuickStocks");
        if (quickStocks == null || !quickStocks.isEnabled()) {
            getLogger().severe("QuickStocks plugin not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get the API instance
        quickStocksAPI = QuickStocksAPI.getInstance(quickStocks);
        
        // Now you can use the managers
        getLogger().info("QuickStocks API loaded successfully!");
    }
}
```

## Managers

### CompanyManager

Manages company operations including creation, employees, transactions, and market operations (IPO, shares).

```java
CompanyManager companyManager = quickStocksAPI.getCompanyManager();

// Create a company
Company company = companyManager.createCompany(playerUuid, "TechCorp", "PUBLIC");

// Get company info
Optional<Company> companyOpt = companyManager.getCompanyByName("TechCorp");

// Financial operations
companyManager.deposit(companyId, playerUuid, 5000.0);
companyManager.withdraw(companyId, playerUuid, 1000.0);

// Market operations (IPO)
companyManager.enableMarket(companyId, playerUuid);
companyManager.buyShares(companyId, playerUuid, 100);
companyManager.sellShares(companyId, playerUuid, 50);

// Employee management
List<Map<String, Object>> employees = companyManager.getEmployees(companyId);
List<CompanyJob> jobs = companyManager.getJobs(companyId);
```

### TradingManager

Handles buying and selling of instruments (stocks, crypto, items).

```java
TradingManager tradingManager = quickStocksAPI.getTradingManager();

// Buy an instrument
boolean success = tradingManager.buy(playerUuid, instrumentId, 100);

// Sell an instrument
boolean success = tradingManager.sell(playerUuid, instrumentId, 50);

// Get holdings
Map<String, Object> holding = tradingManager.getHolding(playerUuid, instrumentId);
List<Map<String, Object>> allHoldings = tradingManager.getAllHoldings(playerUuid);

// Get portfolio value
double totalValue = tradingManager.getPortfolioValue(playerUuid);

// Get trade history
List<Map<String, Object>> history = tradingManager.getTradeHistory(playerUuid, 10);
```

### MarketManager

Provides access to market data, instruments, and price information.

```java
MarketManager marketManager = quickStocksAPI.getMarketManager();

// Get instrument data
Optional<Map<String, Object>> instrument = marketManager.getInstrument(instrumentId);
Optional<Map<String, Object>> bySymbol = marketManager.getInstrumentBySymbol("GOLD");

// Get instrument state (price, volume, changes)
Optional<Map<String, Object>> state = marketManager.getInstrumentState(instrumentId);
double currentPrice = marketManager.getCurrentPrice(instrumentId);

// Get price history
List<Map<String, Object>> history = marketManager.getPriceHistory(instrumentId, 100);

// Get all instruments
List<Map<String, Object>> allInstruments = marketManager.getAllInstruments();
List<Map<String, Object>> itemsOnly = marketManager.getInstrumentsByType("ITEM");

// Market control
boolean isOpen = marketManager.isMarketOpen();
marketManager.openMarket();
marketManager.closeMarket();

// Stock performance
List<Stock> topPerformers = marketManager.getTopPerformers(10);
List<Stock> worstPerformers = marketManager.getWorstPerformers(10);
```

### WalletManager

Manages player wallet balances.

```java
WalletManager walletManager = quickStocksAPI.getWalletManager();

// Get balance
double balance = walletManager.getBalance(playerUuid);

// Modify balance
walletManager.addBalance(playerUuid, 1000.0);
walletManager.subtractBalance(playerUuid, 500.0);
walletManager.setBalance(playerUuid, 10000.0);

// Check balance
boolean hasEnough = walletManager.hasBalance(playerUuid, 500.0);
```

### WatchlistManager

Manages player watchlists for tracking instruments.

```java
WatchlistManager watchlistManager = quickStocksAPI.getWatchlistManager();

// Add to watchlist
boolean added = watchlistManager.addToWatchlist(playerUuid, instrumentId);

// Remove from watchlist
boolean removed = watchlistManager.removeFromWatchlist(playerUuid, instrumentId);

// Get watchlist
List<Map<String, Object>> watchlist = watchlistManager.getWatchlist(playerUuid);

// Check if in watchlist
boolean isWatching = watchlistManager.isInWatchlist(playerUuid, instrumentId);

// Clear watchlist
watchlistManager.clearWatchlist(playerUuid);
```

### CryptoManager

Manages custom cryptocurrency creation and retrieval.

```java
CryptoManager cryptoManager = quickStocksAPI.getCryptoManager();

// Create custom crypto
String cryptoId = cryptoManager.createCrypto(playerUuid, "BTC", "Bitcoin");

// Get crypto data
Optional<Map<String, Object>> crypto = cryptoManager.getCrypto(cryptoId);
Optional<Map<String, Object>> bySymbol = cryptoManager.getCryptoBySymbol("BTC");

// Get all cryptos
List<Map<String, Object>> allCryptos = cryptoManager.getAllCryptos();
List<Map<String, Object>> playerCryptos = cryptoManager.getCryptosByCreator(playerUuid);
```

### PortfolioManager

Provides portfolio and holdings information with profit/loss calculations.

```java
PortfolioManager portfolioManager = quickStocksAPI.getPortfolioManager();

// Get holdings
Map<String, Object> holding = portfolioManager.getHolding(playerUuid, instrumentId);
List<Map<String, Object>> allHoldings = portfolioManager.getAllHoldings(playerUuid);
int quantity = portfolioManager.getHoldingQuantity(playerUuid, instrumentId);

// Get portfolio value
double totalValue = portfolioManager.getPortfolioValue(playerUuid);

// Get profit/loss
double holdingPL = portfolioManager.getHoldingProfitLoss(playerUuid, instrumentId);
double totalPL = portfolioManager.getTotalProfitLoss(playerUuid);
```

## Events

All events are in the `net.cyberneticforge.quickstocks.api.events` package. They extend Bukkit's `Event` class and can be listened to using standard Bukkit event handling.

### Company Events

#### CompanyCreateEvent
Fired when a company is created. **Cancellable**.

```java
@EventHandler
public void onCompanyCreate(CompanyCreateEvent event) {
    Player creator = event.getCreator();
    String companyName = event.getCompanyName();
    String companyType = event.getCompanyType();
    
    // Cancel if you want to prevent creation
    if (companyName.contains("banned")) {
        event.setCancelled(true);
    }
}
```

#### CompanyIPOEvent
Fired when a company goes public (IPO). **Cancellable**.

```java
@EventHandler
public void onCompanyIPO(CompanyIPOEvent event) {
    String companyId = event.getCompanyId();
    String companyName = event.getCompanyName();
    Player initiator = event.getInitiator();
    
    getLogger().info(companyName + " is going public!");
}
```

#### CompanyEmployeeJoinEvent
Fired when a player joins a company. **Not cancellable** (already processed).

```java
@EventHandler
public void onEmployeeJoin(CompanyEmployeeJoinEvent event) {
    Player employee = event.getEmployee();
    String companyName = event.getCompanyName();
    String jobTitle = event.getJobTitle();
    
    employee.sendMessage("Welcome to " + companyName + " as " + jobTitle + "!");
}
```

#### CompanyEmployeeLeaveEvent
Fired when a player leaves a company. **Not cancellable** (already processed).

```java
@EventHandler
public void onEmployeeLeave(CompanyEmployeeLeaveEvent event) {
    Player employee = event.getEmployee();
    String companyName = event.getCompanyName();
    boolean wasKicked = event.wasKicked();
    
    if (wasKicked) {
        employee.sendMessage("You were removed from " + companyName);
    }
}
```

#### CompanyTransactionEvent
Fired when a company transaction occurs (deposit/withdrawal). **Cancellable**.

```java
@EventHandler
public void onCompanyTransaction(CompanyTransactionEvent event) {
    Player player = event.getPlayer();
    String companyName = event.getCompanyName();
    double amount = event.getAmount();
    CompanyTransactionEvent.TransactionType type = event.getType();
    
    if (event.isWithdrawal() && amount > 10000) {
        // Cancel large withdrawals
        event.setCancelled(true);
        player.sendMessage("Withdrawal amount too large!");
    }
}
```

### Trading Events

#### InstrumentBuyEvent
Fired when a player buys an instrument. **Cancellable**.

```java
@EventHandler
public void onInstrumentBuy(InstrumentBuyEvent event) {
    Player buyer = event.getBuyer();
    String symbol = event.getInstrumentSymbol();
    int quantity = event.getQuantity();
    double totalCost = event.getTotalCost();
    
    buyer.sendMessage("Bought " + quantity + " " + symbol + " for $" + totalCost);
}
```

#### InstrumentSellEvent
Fired when a player sells an instrument. **Cancellable**.

```java
@EventHandler
public void onInstrumentSell(InstrumentSellEvent event) {
    Player seller = event.getSeller();
    String symbol = event.getInstrumentSymbol();
    int quantity = event.getQuantity();
    double totalRevenue = event.getTotalRevenue();
    
    seller.sendMessage("Sold " + quantity + " " + symbol + " for $" + totalRevenue);
}
```

#### ShareBuyEvent
Fired when a player buys company shares. **Cancellable**.

```java
@EventHandler
public void onShareBuy(ShareBuyEvent event) {
    Player buyer = event.getBuyer();
    String companyName = event.getCompanyName();
    int quantity = event.getQuantity();
    
    getLogger().info(buyer.getName() + " bought " + quantity + " shares of " + companyName);
}
```

#### ShareSellEvent
Fired when a player sells company shares. **Cancellable**.

```java
@EventHandler
public void onShareSell(ShareSellEvent event) {
    Player seller = event.getSeller();
    String companyName = event.getCompanyName();
    int quantity = event.getQuantity();
    
    getLogger().info(seller.getName() + " sold " + quantity + " shares of " + companyName);
}
```

### Market Events

#### InstrumentPriceUpdateEvent
Fired when an instrument's price is updated. **Not cancellable** (already occurred).

```java
@EventHandler
public void onPriceUpdate(InstrumentPriceUpdateEvent event) {
    String symbol = event.getInstrumentSymbol();
    double oldPrice = event.getOldPrice();
    double newPrice = event.getNewPrice();
    double changePercent = event.getChangePercent();
    
    if (Math.abs(changePercent) > 10) {
        getLogger().warning(symbol + " price changed by " + changePercent + "%!");
    }
}
```

#### MarketOpenEvent
Fired when the market opens. **Not cancellable** (already occurred).

```java
@EventHandler
public void onMarketOpen(MarketOpenEvent event) {
    getServer().broadcastMessage("The market is now open for trading!");
}
```

#### MarketCloseEvent
Fired when the market closes. **Not cancellable** (already occurred).

```java
@EventHandler
public void onMarketClose(MarketCloseEvent event) {
    getServer().broadcastMessage("The market is now closed.");
}
```

#### CircuitBreakerTriggeredEvent
Fired when a circuit breaker halts trading on an instrument. **Not cancellable** (already triggered).

```java
@EventHandler
public void onCircuitBreaker(CircuitBreakerTriggeredEvent event) {
    String symbol = event.getInstrumentSymbol();
    double changePercent = event.getPriceChangePercent();
    boolean isHalted = event.isHalted();
    
    getServer().broadcastMessage("Circuit breaker triggered on " + symbol + 
                                 " (" + changePercent + "% change)");
}
```

### Wallet Events

#### WalletBalanceChangeEvent
Fired when a player's wallet balance changes. **Not cancellable** (already occurred).

```java
@EventHandler
public void onBalanceChange(WalletBalanceChangeEvent event) {
    Player player = event.getPlayer();
    double oldBalance = event.getOldBalance();
    double newBalance = event.getNewBalance();
    double change = event.getChange();
    WalletBalanceChangeEvent.ChangeReason reason = event.getReason();
    
    if (change > 10000) {
        getLogger().info(player.getName() + " balance increased by $" + change + 
                        " (reason: " + reason + ")");
    }
}
```

### Watchlist Events

#### WatchlistAddEvent
Fired when an instrument is added to a watchlist. **Cancellable**.

```java
@EventHandler
public void onWatchlistAdd(WatchlistAddEvent event) {
    Player player = event.getPlayer();
    String symbol = event.getInstrumentSymbol();
    
    player.sendMessage("Added " + symbol + " to your watchlist");
}
```

#### WatchlistRemoveEvent
Fired when an instrument is removed from a watchlist. **Cancellable**.

```java
@EventHandler
public void onWatchlistRemove(WatchlistRemoveEvent event) {
    Player player = event.getPlayer();
    String symbol = event.getInstrumentSymbol();
    
    player.sendMessage("Removed " + symbol + " from your watchlist");
}
```

### Crypto Events

#### CryptoCreateEvent
Fired when a custom cryptocurrency is created. **Cancellable**.

```java
@EventHandler
public void onCryptoCreate(CryptoCreateEvent event) {
    Player creator = event.getCreator();
    String symbol = event.getSymbol();
    String name = event.getName();
    
    getLogger().info(creator.getName() + " created crypto: " + name + " (" + symbol + ")");
}
```

## Complete Example Plugin

```java
package com.example.quickstocksaddon;

import net.cyberneticforge.quickstocks.api.QuickStocksAPI;
import net.cyberneticforge.quickstocks.api.events.*;
import net.cyberneticforge.quickstocks.api.managers.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class QuickStocksAddon extends JavaPlugin implements Listener {
    
    private QuickStocksAPI api;
    
    @Override
    public void onEnable() {
        // Get QuickStocks plugin
        Plugin quickStocks = getServer().getPluginManager().getPlugin("QuickStocks");
        if (quickStocks == null || !quickStocks.isEnabled()) {
            getLogger().severe("QuickStocks plugin not found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Get API instance
        api = QuickStocksAPI.getInstance(quickStocks);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("QuickStocks Addon enabled!");
    }
    
    @EventHandler
    public void onCompanyCreate(CompanyCreateEvent event) {
        Player creator = event.getCreator();
        String companyName = event.getCompanyName();
        
        getLogger().info(creator.getName() + " created company: " + companyName);
    }
    
    @EventHandler
    public void onInstrumentBuy(InstrumentBuyEvent event) {
        Player buyer = event.getBuyer();
        String symbol = event.getInstrumentSymbol();
        int quantity = event.getQuantity();
        
        // Give the player a reward for trading
        if (quantity >= 100) {
            buyer.sendMessage("§aBonus: You bought 100+ shares!");
        }
    }
    
    @EventHandler
    public void onPriceUpdate(InstrumentPriceUpdateEvent event) {
        double changePercent = event.getChangePercent();
        
        // Alert if major price movement
        if (Math.abs(changePercent) > 20) {
            getServer().broadcastMessage(
                "§c§lALERT: " + event.getInstrumentSymbol() + 
                " moved " + String.format("%.2f%%", changePercent) + "!"
            );
        }
    }
}
```

## Error Handling

All manager methods that interact with the database throw `SQLException`. You should handle these appropriately:

```java
try {
    Company company = api.getCompanyManager().createCompany(playerUuid, "TechCorp", "PUBLIC");
    player.sendMessage("Company created successfully!");
} catch (SQLException e) {
    player.sendMessage("§cError creating company. Please try again.");
    getLogger().severe("Database error: " + e.getMessage());
}
```

## Best Practices

1. **Always check plugin availability** before accessing the API
2. **Handle SQLExceptions** appropriately
3. **Use async tasks** for database-heavy operations to avoid blocking the main thread
4. **Listen to events** to react to changes rather than polling
5. **Cache data** when appropriate to reduce database queries

## API Stability

This is version 1.0.0-SNAPSHOT of the API. The API is considered **stable** but may have additions in future versions. Breaking changes will be avoided when possible, but may occur in major version updates.

## Support

For issues, questions, or feature requests related to the QuickStocks API:
- GitHub Issues: https://github.com/Cybernetic-Forge/QuickStocks/issues
- Documentation: See `/Documentation` folder in the repository

## License

The QuickStocks API follows the same license as the QuickStocks plugin.
