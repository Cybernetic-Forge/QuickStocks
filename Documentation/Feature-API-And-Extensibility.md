# API And Extensibility

## Code-side scope

- Public entrypoint: `QuickStocksAPI`
- Manager facade classes in `src/main/java/net/cyberneticforge/quickstocks/api/managers`
- Event classes in `src/main/java/net/cyberneticforge/quickstocks/api/events`

## Current public API surface

- Manager facades currently present in the source tree:
  - `CompanyManager`
  - `CryptoManager`
  - `MarketManager`
  - `PortfolioManager`
  - `TradingManager`
- Event classes currently present in the source tree: 14 total
  - company lifecycle and employment events
  - market open/close and price-update events
  - trading events
  - wallet balance change events
  - watchlist events
  - crypto creation event
  - circuit breaker event

## Integration model

- `QuickStocksAPI.initialize(...)` is called by the plugin during startup.
- External plugins call `QuickStocksAPI.getInstance()` after QuickStocks has loaded.
- The API provides a centralized facade over company, market, trading, crypto, and portfolio functionality.
- Events are Bukkit events and can be consumed through normal listener registration.

## Legacy docs summarized

- `src/main/java/net/cyberneticforge/quickstocks/api/README.md`
- API-related notes from the previous implementation summaries and root README files

## Current caveats

- The legacy API README is stale relative to the tree: it mentions 7 managers and 17 events, but the current source tree exposes 5 manager classes and 14 event classes.
- The API is real and initialized from plugin bootstrap, but its documentation previously lived outside the main `Documentation/` directory. This file now serves as the consolidated API-facing overview.
