# Operations And Platform

## Code-side scope

- Plugin bootstrap: `QuickStocksPlugin`
- Admin command: `QuickStocksCommand`
- Config classes: `MarketCfg`, `TradingCfg`, `CompanyCfg`, `CryptoCfg`, `GuiConfig`
- Database and migration layer under `infrastructure/db`
- Logging and metrics: `PluginLogger`, `MetricsService`

## Runtime responsibilities

- Save the default config on first run.
- Initialize the database and run migrations.
- Load market, trading, company, crypto, GUI, and translation configuration.
- Register commands and event listeners conditionally by feature flags.
- Start scheduled tasks for:
  - market price updates
  - market hours open/close checks
  - salary payments
  - plot rent collection
- Initialize the public API and optional metrics.

## Operational surface

- `/quickstocks reload` reloads configs and restarts the relevant services/schedulers.
- Resource/config files currently used by the plugin:
  - `config.yml`
  - `market.yml`
  - `companies.yml`
  - `guis.yml`
  - `Translations.yml`
  - `plugin.yml`
- Database providers supported in config: SQLite, MySQL, PostgreSQL.
- Metrics support is controlled by `metrics.enabled`.
- Logging verbosity is controlled by `logging.debugLevel`.

## Legacy docs summarized

- `Installation.md`
- `Configuration.md`
- `Database.md`
- `LOGGING.md`
- `CONFIG_MIGRATION.md`
- `FEATURE_TOGGLES.md`
- `FEATURE_TOGGLE_TESTING.md`
- `TESTING.md`
- `TEST_SUITE.md`
- `CONTRIBUTING_TESTS.md`
- `PIPELINE_VERSIONING.md`
- `PIPELINE_VERSIONING_IMPLEMENTATION.md`
- `PIPELINE_WORKFLOW_DIAGRAM.md`
- `CODE_QUALITY_ANALYSIS.md`
- `PR_SUMMARY.md`
- `IMPLEMENTATION_SUMMARY.md`
- `IMPLEMENTATION_SUMMARY_TESTS.md`

## Current caveats

- The old doc set mixed user docs, implementation notes, test plans, and release-process notes together. This file is the consolidated operational replacement.
- Feature toggles exist in code and config, but the exact live defaults are determined by the shipped YAML resources, not only by fallback values in the config classes.
