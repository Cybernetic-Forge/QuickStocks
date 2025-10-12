# Testing Guide for Feature Toggles

## Manual Testing Procedures

Since the project uses the Minecraft Paper API which requires a running server, manual testing is recommended.

### Setup

1. Build the plugin:
   ```bash
   mvn clean package
   ```

2. Copy the resulting JAR to your test server's plugins folder:
   ```bash
   cp target/QuickStocks-*.jar /path/to/server/plugins/
   ```

3. Start the server and verify plugin loads successfully

### Test Scenarios

#### Test 1: Companies System Disabled
**Config:** Set `companies.enabled: false` in `plugins/QuickStocks/companies.yml`

**Expected Results:**
- Server logs should NOT show "Registered company-related event listeners"
- `/company` command should not be registered (server should say "Unknown command")
- Company-related listeners should not be registered
- ChestShop integration for companies should be disabled

**Test Steps:**
1. Restart server after config change
2. Try running `/company` - should show "Unknown command"
3. Verify no company-related features are accessible

#### Test 2: Market System Disabled
**Config:** Set `market.enabled: false` in `plugins/QuickStocks/market.yml`

**Expected Results:**
- Server logs should NOT show "Registered market-related event listeners"
- `/market`, `/watch`, `/stocks`, `/crypto`, `/marketdevice` commands should not be registered
- Market-related listeners should not be registered

**Test Steps:**
1. Restart server after config change
2. Try running any market command - should show "Unknown command"
3. Verify no market-related features are accessible

#### Test 3: Trading Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.trading: false` in `market.yml`

**Expected Results:**
- `/market` command should work
- `/market buy` should show "This feature is currently disabled."
- `/market sell` should show "This feature is currently disabled."
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Run `/market` - should work (show market overview)
3. Run `/market buy <symbol> <qty>` - should show disabled message
4. Run `/market sell <symbol> <qty>` - should show disabled message
5. Run `/market portfolio` - should work (if portfolio is enabled)

#### Test 4: Watchlist Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.watchlist: false` in `market.yml`

**Expected Results:**
- `/watch` command should not be registered
- `/market watchlist` should show "This feature is currently disabled."
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Try running `/watch` - should show "Unknown command"
3. Run `/market watchlist` - should show disabled message
4. Run `/market buy` - should work (if trading is enabled)

#### Test 5: Portfolio Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.portfolio: false` in `market.yml`

**Expected Results:**
- `/market portfolio` should show "This feature is currently disabled."
- `/market history` should show "This feature is currently disabled."
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Run `/market portfolio` - should show disabled message
3. Run `/market history` - should show disabled message
4. Run `/market buy` - should work (if trading is enabled)

#### Test 6: Market Device Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.marketDevice: false` in `market.yml`

**Expected Results:**
- `/marketdevice` command should not be registered
- Market device listener should not be registered
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Try running `/marketdevice` - should show "Unknown command"
3. Right-clicking with a market device should do nothing (if you have one from before)
4. Run `/market` - should work

#### Test 7: Stocks Command Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.stocksCommand: false` in `market.yml`

**Expected Results:**
- `/stocks` command should not be registered
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Try running `/stocks` - should show "Unknown command"
3. Run `/market` - should work

#### Test 8: Crypto Command Sub-feature Disabled
**Config:** 
- Set `market.enabled: true` in `market.yml`
- Set `market.features.cryptoCommand: false` in `market.yml`

**Expected Results:**
- `/crypto` command should not be registered
- Other market commands should work normally

**Test Steps:**
1. Restart server after config change
2. Try running `/crypto` - should show "Unknown command"
3. Run `/market` - should work

#### Test 9: All Features Enabled (Default)
**Config:** Use default configuration with all features enabled

**Expected Results:**
- All commands should be registered
- All listeners should be registered
- All features should be accessible

**Test Steps:**
1. Use default config or explicitly set all features to `true`
2. Restart server
3. Verify all commands work: `/company`, `/market`, `/watch`, `/stocks`, `/crypto`, `/marketdevice`
4. Verify all subcommands work within `/market`

### Automated Validation

#### YAML Syntax Validation
```bash
# Install yamllint if not present
pip install yamllint

# Validate YAML files
yamllint src/main/resources/market.yml
yamllint src/main/resources/companies.yml
```

#### Build Verification
```bash
# Ensure code compiles without errors
mvn clean compile

# Run any existing tests
mvn test
```

### Server Log Verification

When testing, always check the server logs for:
1. Plugin initialization messages
2. Command registration confirmations
3. Listener registration confirmations
4. Any error messages or warnings

Expected log entries when features are enabled:
```
[QuickStocks] QuickStocks enabling (Paper 1.21.8)...
[QuickStocks] Database initialized: ...
[QuickStocks] Registered market-related event listeners
[QuickStocks] Registered company-related event listeners
[QuickStocks] QuickStocks enabled successfully! Market is now running.
```

When features are disabled, corresponding registration messages should be absent.

## Troubleshooting

### Commands Not Working After Config Change
**Solution:** Restart the server. Config changes require a server restart to take effect.

### Commands Still Registered When Disabled
**Solution:** 
1. Verify the config file syntax is correct
2. Ensure you're editing the correct config file (in plugins/QuickStocks/)
3. Check server logs for config loading errors
4. Restart the server

### Error Messages When Testing
**Solution:**
1. Check if the feature is properly disabled in config
2. Verify the command syntax is correct
3. Check server logs for detailed error information

## CI/CD Integration

For automated testing in CI/CD pipelines:
1. Compile verification: `mvn clean compile`
2. YAML validation: `yamllint src/main/resources/*.yml`
3. Code style checks: Use existing linting tools
4. Integration tests would require a test Minecraft server instance

## Performance Testing

To verify that disabled features don't impact performance:
1. Measure plugin load time with all features enabled
2. Measure plugin load time with features disabled
3. Compare server memory usage
4. Verify no unnecessary background tasks are running for disabled features
