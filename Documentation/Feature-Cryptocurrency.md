# Cryptocurrency

## Code-side scope

- Command: `CryptoCommand`
- Service: `CryptoService`
- Config: `CryptoCfg`
- Market integration: `MarketGUI`, `TradingService`, `InstrumentPersistenceService`

## User surface

- `/crypto create <symbol> <name...>` creates a personal cryptocurrency.
- `/crypto company <company> <symbol> <name...>` creates a company-owned cryptocurrency.
- Newly created crypto instruments show up in the market GUI and trade through the same instrument pipeline as other non-company instruments.

## Runtime behavior

- The command is gated by `market.features.cryptoCommand`.
- Personal crypto creation requires the `quickstocks.command.crypto.create` permission.
- Company crypto creation requires the same permission plus company-management authority.
- `CryptoService` validates symbol uniqueness and enforces creation rules, balance thresholds, maximum counts, and default instrument values.
- Crypto instruments are persisted as market instruments and can be queried by ID, symbol, creator, or full list.

## Configuration

- `market.yml`
  - `market.features.cryptoCommand`
  - `crypto.enabled`
  - `crypto.personal.*`
  - `crypto.company.*`
  - `crypto.defaults.*`
  - `crypto.trading.*`

## Legacy docs summarized

- `Commands-Crypto.md`
- Crypto sections of `Getting-Started.md`
- Crypto sections of `Configuration.md`
- Crypto permission references from `Permissions.md`

## Current caveats

- The command layer exposes creation only. Discovery and trading happen through `/market`, not through a separate crypto browser command.
- The legacy documentation sometimes blended command-toggle settings and the deeper `crypto.*` config. Both matter: one controls command availability and the other controls creation rules.
