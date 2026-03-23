# Company Plots And Territory

## Code-side scope

- Command surface inside `CompanyCommand`
- Service: `CompanyPlotService`
- Listener: `CompanyPlotListener`
- GUIs and listeners: `PlotEditGUI`, `PlotPermissionEditGUI`, `PlotEditGUIListener`, `PlotPermissionEditGUIListener`
- Optional WorldGuard gate: `WorldGuardHook`, `WorldGuardFlags`

## User and admin surface

- `/company buyplot <company>`
- `/company buyplot <company> on|off` for auto-buy mode
- `/company sellplot <company>`
- `/company plots <company>`
- `/company nearplots`
- `/company editplot`

## Runtime behavior

- Plot ownership is chunk-based.
- `CompanyPlotService` supports plot purchase, sale, lookups by location, company plot listing, rent due queries, auto-buy mode, per-plot permission records, and nearby-plot discovery.
- `CompanyPlotListener` provides:
  - terrain enter/leave/wilderness messages
  - auto-buy behavior when moving into new chunks
  - build protection
  - container protection
  - per-plot permission enforcement for employees
- Plot permission editing is exposed through dedicated GUIs.
- WorldGuard can block plot purchasing through the custom QuickStocks plots flag.

## Configuration

- `companies.yml`
  - `companies.plots.enabled`
  - `companies.plots.buyPlotPrice`
  - `companies.plots.sellPlotPrice`
  - `companies.plots.plotRent`
  - `companies.plots.plotRentInterval`
  - `companies.plots.terrainMessages.*`
  - `companies.allowedDebts.companyPlots`
- `guis.yml`
  - `plot_edit.*`
  - `plot_permission_edit.*`
- WorldGuard integration depends on the plugin soft dependency and registered flags.

## Legacy docs summarized

- `COMPANY_PLOTS_FEATURE.md`
- `IMPLEMENTATION_SUMMARY_PLOTS.md`
- `PLOT_FEATURE_TESTING.md`
- Plot sections of `Commands-Company.md`
- Plot sections of `Configuration.md`
- WorldGuard plot sections from `WORLDGUARD_INTEGRATION.md` and `WORLDGUARD_IMPLEMENTATION_SUMMARY.md`

## Current caveats

- The bundled `companies.yml` currently ships with plots disabled by default.
- The feature is deeper than the old docs implied: it includes auto-buy, nearby-plot visualization, GUI-based permission editing, and debt-aware rent processing.
