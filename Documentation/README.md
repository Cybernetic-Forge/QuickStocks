# QuickStocks Feature Documentation

This directory replaces the previous wiki-style documentation set with a feature-oriented set grounded in the current source tree.

## Current code-side feature map

| Feature | Primary code artifacts | Player/admin entry points | Notes |
|---|---|---|---|
| Market and trading | `MarketCommand`, `MarketGUI`, `MarketGUIListener`, `StockMarketService`, `TradingService`, `CompanyMarketService`, `MarketScheduler`, `ItemSeederService` | `/market`, market GUI, scheduled market updates | Core trading surface |
| Wallet and portfolio | `WalletCommand`, `WalletService`, `HoldingsService`, `PortfolioGUI`, `PortfolioGUIListener` | `/wallet`, `/market portfolio`, `/market history` | Vault fallback supported |
| Watchlist | `WatchCommand`, `WatchlistService` | `/watch`, `/market watchlist` | Separate command plus market summary |
| Cryptocurrency | `CryptoCommand`, `CryptoService`, `CryptoCfg` | `/crypto create`, `/crypto company` | Custom personal and company instruments |
| Companies and ownership | `CompanyCommand`, `CompanyService`, `CompanyMarketService`, company GUIs/listeners | `/company ...` | Includes IPO and shareholder flows |
| Company plots and territory | `CompanyPlotService`, `CompanyPlotListener`, plot GUIs/listeners | `/company buyplot`, `/company sellplot`, `/company plots`, `/company nearplots`, `/company editplot` | Bundled config disables plots by default |
| Salaries | `SalaryService`, company salary subcommands, salary scheduler | `/company salary ...` | Scheduled payments with cycle configuration |
| Integrations | `WalletService` (Vault), `ChestShopHook`, `ChestShop...Listener`, `WorldGuardHook`, `WorldGuardFlags` | Soft dependencies plus config flags | ChestShop and WorldGuard are optional |
| Operations and platform | `QuickStocksPlugin`, `QuickStocksCommand`, config classes, database layer, metrics | `/quickstocks reload`, config files, schedulers | Covers bootstrap, config, database, metrics |
| API and extensibility | `QuickStocksAPI`, API managers, API event classes | External plugin integration | Current tree exposes 5 managers and 14 event classes |

## Important code-vs-legacy-doc mismatches

- There is no `StocksCommand` class and no `stocks` entry in `plugin.yml`. Legacy docs documented `/stocks`, but the current player-facing market surface is centered on `/market`, the market GUI, `QueryService`, and the public API.
- The market device feature has been removed from the codebase and should no longer be treated as part of the plugin scope.
- The legacy API README claims 7 managers and 17 events. The current source tree contains 5 manager classes under `api/managers` and 14 event classes under `api/events`.
- Legacy wallet docs/translations referenced flows like pay, deposit, and withdraw. The current `WalletCommand` exposes `balance`, `add`, and `set`.

## Replacement documentation set

- [Feature-Market-And-Trading.md](Feature-Market-And-Trading.md)
- [Feature-Wallet-And-Portfolio.md](Feature-Wallet-And-Portfolio.md)
- [Feature-Watchlist.md](Feature-Watchlist.md)
- [Feature-Cryptocurrency.md](Feature-Cryptocurrency.md)
- [Feature-Companies-And-Ownership.md](Feature-Companies-And-Ownership.md)
- [Feature-Company-Plots-And-Territory.md](Feature-Company-Plots-And-Territory.md)
- [Feature-Salaries.md](Feature-Salaries.md)
- [Feature-Integrations.md](Feature-Integrations.md)
- [Feature-Operations-And-Platform.md](Feature-Operations-And-Platform.md)
- [Feature-API-And-Extensibility.md](Feature-API-And-Extensibility.md)

## Legacy docs consolidated into these files

- Market and trading absorbed the previous market, stocks, stock-unification, feature-toggle, testing, and item-instrument docs.
- Wallet and portfolio absorbed the previous wallet-oriented user docs plus the wallet parts of getting-started and permissions docs.
- Watchlist absorbed the previous watch command coverage.
- Cryptocurrency absorbed the previous crypto command coverage and crypto-related configuration notes.
- Companies, plots, and salaries absorbed the former company, plot, salary, WorldGuard, and ChestShop feature pages.
- Operations and platform absorbed installation, configuration, database, logging, testing, migration, and release-process notes.
- API and extensibility absorbed the code-side API documentation from `src/main/java/.../api`.

## How to read these files

Each feature file uses the same structure:

1. Code-side scope
2. User/admin surface
3. Configuration and integration points
4. Legacy docs summarized
5. Current gaps or caveats
