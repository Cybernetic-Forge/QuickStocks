# Market And Trading

## Code-side scope

- Commands: `MarketCommand`
- GUIs and listeners: `MarketGUI`, `MarketGUIListener`
- Services: `StockMarketService`, `TradingService`, `CompanyMarketService`, `InstrumentPersistenceService`, `InstrumentSyncService`, `ItemSeederService`, `MarketScheduler`, `CircuitBreakerService`, `FeeService`, `RateLimitService`, `SlippageService`
- Config: `MarketCfg`, `TradingCfg`, `GuiConfig`

## User and admin surface

- `/market` opens the market GUI by default.
- `/market browse|list` reopens or refreshes the market overview.
- `/market buy <company> <qty>` and `/market sell <company> <qty>` trade company shares from chat.
- `/market shareholders <company>` shows company shareholder distribution.
- `/market portfolio`, `/market history`, and `/market watchlist` bridge into portfolio/history/watchlist flows.
- The market GUI exposes:
  - filter modes for all instruments, company shares, crypto instruments, and item instruments
  - quick buy and quick sell interactions
  - shift-click prompts for custom amount entry
  - portfolio and wallet shortcut buttons
  - refresh/navigation buttons

## Runtime behavior

- Market hours are enforced through `MarketScheduler` using `market.hours.*`.
- Price updates run on a scheduled task started by `QuickStocksPlugin.startMarketPriceUpdateTask()`.
- Item instruments can be seeded at startup through `ItemSeederService` when `market.items.seedOnStartup` is enabled.
- Company shares trade through `CompanyMarketService`; generic instruments such as crypto and items trade through `TradingService`.
- Trading behavior is configurable with fees, cooldowns, notional limits, circuit breakers, allowed order types, and slippage settings.

## Configuration

- `market.yml`
  - `market.enabled`
  - `market.updateInterval`
  - `market.startOpen`
  - `market.items.enabled`
  - `market.items.seedOnStartup`
  - `market.items.seedItems.*`
  - `market.features.watchlist`
  - `market.features.portfolio`
  - `market.features.trading`
  - `market.features.cryptoCommand`
  - `market.priceThreshold.*`
  - `market.hours.*`
  - `analytics.*`
- `market.yml` also holds `trading.*` for the `TradingCfg` surface.
- `guis.yml` controls market and portfolio GUI layout, filter labels, slot positions, and item rendering.

## Legacy docs summarized

- `Commands-Market.md`
- `Commands-Stocks.md`
- `STOCK_UNIFICATION.md`
- `FEATURE_TOGGLES.md`
- `FEATURE_TOGGLE_TESTING.md`
- `TESTING.md`
- `ITEM_INSTRUMENTS_INTEGRATION.md`
- Market sections of `Getting-Started.md`, `Configuration.md`, and `Permissions.md`

## Current caveats

- Legacy docs treated `/stocks` as a first-class player command. The current code does not register `/stocks`; market discovery is centered on `/market`, the GUI, `QueryService`, and the public API.
- Legacy docs often described the market as only company-share trading. The current GUI and trading services also support crypto and item instruments.
- Some advanced analytics are implemented or configurable on the service side, but not all of them are surfaced as dedicated player commands.
