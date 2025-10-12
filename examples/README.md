# Configuration Examples

This directory contains example configurations demonstrating different use cases for QuickStocks feature toggles.

## Available Examples

### market-minimal.yml
A minimal market configuration that disables most features while keeping basic viewing functionality.
- **Enabled:** Market overview, portfolio viewing, stocks command
- **Disabled:** Trading, watchlist, market device, crypto command
- **Use case:** Display-only market for servers that want to show information without allowing transactions

### market-disabled.yml
Completely disables the market system.
- **Enabled:** None (entire market system disabled)
- **Disabled:** All market features and commands
- **Use case:** Servers that only want to use the companies feature without any market functionality

### companies-disabled.yml
Completely disables the companies/corporations system.
- **Enabled:** None (entire companies system disabled)
- **Disabled:** All company features, commands, and listeners
- **Use case:** Servers that only want market trading without company management features

## How to Use These Examples

1. Copy the desired example file to your server's plugin configuration directory:
   ```bash
   cp examples/market-minimal.yml plugins/QuickStocks/market.yml
   # or
   cp examples/companies-disabled.yml plugins/QuickStocks/companies.yml
   ```

2. Restart your server for changes to take effect

3. Verify the configuration by checking:
   - Server logs for plugin initialization messages
   - Available commands (try the disabled commands to confirm they don't work)
   - Plugin behavior matches your expectations

## Creating Custom Configurations

You can mix and match settings from these examples to create your own custom configuration. Key points:

- Setting `market.enabled: false` disables the entire market system regardless of sub-feature settings
- Setting `companies.enabled: false` disables the entire companies system
- Sub-features under `market.features.*` allow fine-grained control when `market.enabled: true`
- All features default to `true` if not specified

## Validation

Before using custom configurations, validate the YAML syntax:
```bash
python3 -c "import yaml; yaml.safe_load(open('your-config.yml'))"
```

Or use an online YAML validator.
