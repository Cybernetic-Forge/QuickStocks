# Watchlist

## Code-side scope

- Command: `WatchCommand`
- Service: `WatchlistService`
- Related `/market` subcommand: `watchlist`

## User surface

- `/watch` defaults to listing the current watchlist.
- `/watch add <symbol>`
- `/watch remove <symbol>`
- `/watch list|show`
- `/watch info|details <symbol>`
- `/watch clear`
- `/market watchlist` gives a summary from inside the market command surface.

## Runtime behavior

- `WatchlistService` stores the instrument ID, symbol, display name, type, add time, and current pricing snapshot data for each entry.
- The service supports add, remove, existence checks, count, and full clear operations.
- The market command can summarize the watchlist without leaving the `/market` surface.

## Configuration

- `market.yml`
  - `market.features.watchlist`

## Legacy docs summarized

- `Commands-Watch.md`
- Watchlist portions of `Commands-Market.md`
- Watchlist portions of `Getting-Started.md`
- Watchlist permission/feature-toggle notes from `FEATURE_TOGGLES.md` and `TESTING.md`

## Current caveats

- Watchlist functionality is real and code-backed, but legacy docs occasionally described it as part of a broader `/stocks` workflow. In the current codebase, watchlist is tied to instruments discoverable through `/market`, `QueryService`, and the public API.
