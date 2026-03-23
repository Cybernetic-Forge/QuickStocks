# Integrations

## Code-side scope

- Vault integration inside `WalletService`
- ChestShop integration through `ChestShopHook`, `ChestShopAccountProvider`, and three ChestShop listeners
- WorldGuard integration through `WorldGuardFlags` and `WorldGuardHook`
- Hook detection through `HookManager`

## Supported integrations

### Vault

- `WalletService` uses Vault if present and falls back to the internal wallet implementation if not.
- The integration is runtime-detected; the code avoids hard compile-time dependence for basic wallet behavior.

### ChestShop

- Company-owned ChestShop account names are registered through `ChestShopAccountProvider`.
- `ChestShopHook` checks whether a player can manage a company shop and moves funds directly into or out of company balances for transactions.
- Three listeners cover shop creation, transaction handling, and protection logic.
- The company configuration includes an enable flag, minimum company balance, and debt allowance for ChestShop operations.

### WorldGuard

- Custom flags are registered on load and queried through `WorldGuardHook`.
- The trading flag can restrict stock/market actions by region.
- The plots flag can restrict company plot purchases by region.

## Configuration

- `companies.yml`
  - `companies.chestshop.enabled`
  - `companies.chestshop.companyMinBalance`
  - `companies.allowedDebts.chestshops`
- `config.yml`
  - soft dependency awareness lives in plugin bootstrap rather than direct config

## Legacy docs summarized

- `ChestShop-Integration.md`
- `WORLDGUARD_INTEGRATION.md`
- `WORLDGUARD_IMPLEMENTATION_SUMMARY.md`
- Integration sections of `Installation.md` and `Configuration.md`

## Current caveats

- ChestShop and WorldGuard are optional and only activate when both the dependency and the related feature config are enabled.
- Some legacy docs treated integrations as always-on features; the current code is explicit about soft dependencies and graceful fallback behavior.
