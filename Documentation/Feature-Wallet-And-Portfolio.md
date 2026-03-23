# Wallet And Portfolio

## Code-side scope

- Command: `WalletCommand`
- Services: `WalletService`, `HoldingsService`, `QueryService`
- GUIs and listeners: `PortfolioGUI`, `PortfolioGUIListener`
- Related `/market` subcommands: `portfolio`, `history`

## User and admin surface

- `/wallet` and `/wallet balance|bal` show the current balance.
- `/wallet add <amount>` adds funds to the caller and requires `quickstocks.wallet.add`.
- `/wallet set <amount>` sets the caller balance and requires `quickstocks.wallet.set`.
- `/market portfolio` shows holdings and total asset value.
- `/market history` shows recent trade history.
- The portfolio GUI shows wallet summary, holdings, aggregate value, and lets players sell all of a holding from the GUI.

## Runtime behavior

- `WalletService` uses Vault if the `Vault` plugin and an economy provider are present.
- If Vault is unavailable, the plugin falls back to its internal wallet tables.
- `HoldingsService` tracks quantity, average cost, current value, and unrealized PnL.
- `QueryService` supports symbol lookups, recent price history, top-gainer style queries, and instrument metadata used by the portfolio and market flows.

## Configuration

- `config.yml`
  - `database.*`
  - `metrics.enabled`
  - `logging.debugLevel`
- `guis.yml`
  - `portfolio.*`
  - shared market shortcuts

## Legacy docs summarized

- `Commands-Wallet.md`
- Wallet and portfolio sections of `Commands-Market.md`
- Wallet sections of `Getting-Started.md`
- Wallet permission notes from `Permissions.md`

## Current caveats

- Legacy docs referenced wallet flows such as pay, deposit, withdraw, and admin remove. The current `WalletCommand` implements only `balance`, `add`, and `set`.
- Portfolio operations are split across `/market` chat commands and GUI interactions rather than a dedicated `/portfolio` command.
