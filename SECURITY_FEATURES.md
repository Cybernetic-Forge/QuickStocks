# QuickStocks Security & Concurrency Features

This document describes the security, concurrency, and robustness features implemented in QuickStocks to prevent double-spends, race conditions, and ensure data integrity.

## Overview

QuickStocks implements several layers of protection to ensure safe trading operations:

1. **Atomic Transactions** - All trade operations are wrapped in database transactions
2. **Idempotency Keys** - Prevent duplicate orders from being processed
3. **Optimistic Versioning** - Prevent concurrent modification conflicts
4. **Integrity Auditing** - Detect and repair data inconsistencies
5. **Automated Backups** - Regular data snapshots for recovery
6. **Graceful Shutdown** - Safe plugin shutdown with data preservation

## Atomic Transactions

All trading operations (buy/sell) are executed within database transactions using SERIALIZABLE isolation level:

```java
// Example: Buying shares atomically
database.executeTransaction(db -> {
    // 1. Check current price
    // 2. Validate player balance
    // 3. Debit wallet
    // 4. Update holdings with versioning
    // 5. Record order with idempotency key
});
```

**Benefits:**
- Prevents partial trade execution
- Ensures data consistency
- Automatic rollback on failures

## Idempotency Keys

Each trade order can include a unique idempotency key to prevent duplicate processing:

```java
// Client sends same request twice
TradingService.TradeResult result1 = tradingService.executeBuyOrder(
    playerUuid, "AAPL", 10.0, "unique-key-123"
);
TradingService.TradeResult result2 = tradingService.executeBuyOrder(
    playerUuid, "AAPL", 10.0, "unique-key-123"  // Same key
);

// Second call returns cached result without executing trade
assert result2.getMessage().contains("[CACHED]");
```

**Implementation:**
- Unique index on `orders.client_idempotency` prevents duplicates
- Duplicate requests return the original result
- No side effects on repeated calls

## Optimistic Versioning

Player holdings use version numbers to prevent concurrent modification:

```sql
-- Holdings table includes version column
CREATE TABLE user_holdings (
    player_uuid TEXT NOT NULL,
    instrument_id TEXT NOT NULL,
    quantity REAL NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (player_uuid, instrument_id)
);
```

**Update Process:**
1. Read current holding with version
2. Update using WHERE clause with version check
3. If 0 rows affected, retry with new version
4. Increment version on successful update

```java
// Update with version check
int rowsAffected = db.execute(
    "UPDATE user_holdings SET qty = ?, version = version + 1 WHERE player_uuid = ? AND instrument_id = ? AND version = ?",
    newQty, playerUuid, instrumentId, expectedVersion
);

if (rowsAffected == 0) {
    // Version conflict - retry
}
```

## Integrity Auditing

The audit system validates portfolio consistency and can repair discrepancies:

### Running Audits

```bash
# Check for inconsistencies (read-only)
/stocks audit

# Check and automatically repair issues
/stocks audit repair
```

### How It Works

1. **Recalculate Expected Holdings**: Sum all buy/sell orders for each player
2. **Compare with Current Holdings**: Check database user_holdings table
3. **Report Discrepancies**: Show differences between expected and actual
4. **Repair (Optional)**: Update holdings to match order history

```java
// Audit process
AuditService.AuditResult result = auditService.auditAllHoldings(repair);
System.out.println("Players checked: " + result.totalPlayersChecked);
System.out.println("Issues found: " + result.totalDiscrepancies);
System.out.println("Repairs applied: " + result.totalRepairs);
```

### Use Cases

- **Manual Database Changes**: Detect and fix manual data modifications
- **Bug Recovery**: Repair data corruption from software bugs
- **Migration Validation**: Verify data integrity after system changes
- **Regular Maintenance**: Periodic checks for data consistency

## Automated Backups

QuickStocks automatically creates data backups for disaster recovery:

### Backup Types

1. **Daily Backups**: Scheduled automatic backups
2. **Manual Backups**: On-demand admin-triggered backups
3. **Emergency Backups**: Created during plugin shutdown

### Configuration

```yaml
# config.yml
backup:
  enabled: true
  dailyBackup: true
  emergencyBackup: true
  retentionDays: 30
```

### Backup Contents

Each backup includes CSV exports of:
- `instruments.csv` - All trading instruments
- `instrument_state.csv` - Current prices and market data
- `user_holdings.csv` - Player portfolios
- `orders.csv` - Complete trade history
- `wallets.csv` - Player balances
- `backup_summary.json` - Backup metadata

### Storage Location

```
plugins/QuickStocks/backups/
├── 2024-01-15/
│   ├── instruments.csv
│   ├── user_holdings.csv
│   ├── orders.csv
│   └── backup_summary.json
├── 2024-01-16/
└── 2024-01-17-emergency/
```

## Graceful Shutdown

Plugin shutdown includes proper cleanup and data preservation:

```java
@Override
public void onDisable() {
    // 1. Create emergency backup
    backupService.performEmergencyBackup();
    
    // 2. Stop simulation engine
    simulationEngine.stop();
    
    // 3. Cancel background tasks
    if (marketUpdateTask != null) {
        marketUpdateTask.cancel();
    }
    
    // 4. Close market to prevent new trades
    stockMarketService.setMarketOpen(false);
    
    // 5. Log final statistics
    logger.info("Final stats: " + queryService.getTotalOrderCount() + " orders");
    
    // 6. Shutdown database connections
    databaseManager.shutdown();
}
```

## Configuration Options

### Security Settings

```yaml
# config.yml
security:
  maxRetries: 3                # Optimistic locking retry attempts
  transactionTimeout: 30       # Transaction timeout in seconds
  auditEnabled: true          # Enable integrity auditing
```

### Permissions

```yaml
# plugin.yml
permissions:
  quickstocks.admin.audit:
    description: "Run integrity audits and repairs"
    default: op
  quickstocks.admin.backup:
    description: "Manage backup operations"
    default: op
```

## Database Schema Changes

The security features require additional database columns and tables:

### V3 Migration

```sql
-- Add idempotency support to orders
ALTER TABLE orders ADD COLUMN client_idempotency TEXT;
CREATE UNIQUE INDEX idx_orders_idempotency ON orders(client_idempotency) WHERE client_idempotency IS NOT NULL;

-- Add versioning to holdings
ALTER TABLE user_holdings ADD COLUMN version INTEGER NOT NULL DEFAULT 1;

-- Add audit logging
CREATE TABLE audit_log (
    id TEXT PRIMARY KEY,
    audit_type TEXT NOT NULL,
    player_uuid TEXT,
    details TEXT NOT NULL,
    timestamp INTEGER NOT NULL
);

-- Add backup metadata
CREATE TABLE backup_metadata (
    id TEXT PRIMARY KEY,
    backup_date TEXT NOT NULL,
    backup_type TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
```

## Performance Considerations

### Transaction Overhead

- Database transactions add slight latency but ensure consistency
- SERIALIZABLE isolation may cause retries under high concurrency
- Version-based updates reduce lock contention

### Audit Performance

- Full audits scale with player count and order history
- Consider running during low-activity periods
- Audit results are cached for performance

### Backup Impact

- CSV exports are lightweight and fast
- Backups run asynchronously to avoid blocking gameplay
- Automatic cleanup based on retention policy

## Testing

Comprehensive tests validate the security features:

```bash
# Run security and concurrency tests
mvn test -Dtest="*Security*,*Audit*,*Trading*"
```

Test coverage includes:
- **Concurrent Trading**: Multiple simultaneous orders
- **Idempotency**: Duplicate request handling
- **Version Conflicts**: Optimistic locking scenarios
- **Audit Integration**: End-to-end consistency checking
- **Backup Operations**: Data export and recovery

## Monitoring and Alerts

### Log Events

Important security events are logged:

```
[INFO] Executed buy order for player123: BUY 10.00 shares at $150.00
[INFO] Returning cached result for idempotency key: unique-key-123
[WARN] Version conflict when updating holding, retrying... (2 retries left)
[INFO] Audit completed: 45 players checked, 2 with issues, 3 discrepancies found
[INFO] Emergency backup completed: 6 files, 2048 bytes
```

### Metrics to Monitor

- Trade execution latency
- Version conflict frequency
- Audit discrepancy rates
- Backup success/failure rates
- Database transaction rollback rates

## Best Practices

### For Administrators

1. **Regular Audits**: Run `/stocks audit` weekly
2. **Monitor Logs**: Watch for version conflicts or backup failures
3. **Test Recovery**: Periodically validate backup restoration
4. **Update Retention**: Adjust backup retention based on storage

### For Developers

1. **Use Transactions**: Wrap multi-step operations in transactions
2. **Handle Retries**: Implement proper retry logic for version conflicts
3. **Validate Input**: Always validate player permissions and data
4. **Test Concurrency**: Include concurrent access in test scenarios

## Troubleshooting

### Common Issues

**High Version Conflicts**
```
[WARN] Version conflict when updating holding, retrying...
```
- **Cause**: High concurrent trading activity
- **Solution**: Consider increasing max retries or implementing back-off

**Audit Discrepancies**
```
[ERROR] Holdings audit found 5 discrepancies
```
- **Cause**: Manual database changes or software bugs
- **Solution**: Run `/stocks audit repair` to fix inconsistencies

**Backup Failures**
```
[ERROR] Backup failed: Database connection failed
```
- **Cause**: Database connectivity issues
- **Solution**: Check database status and retry backup

### Recovery Procedures

**Data Corruption Recovery**
1. Stop the plugin
2. Restore from latest backup
3. Run integrity audit
4. Restart plugin

**Performance Issues**
1. Check database connection pool settings
2. Monitor transaction timeout values
3. Consider optimizing concurrent access patterns

---

For technical support or questions about these security features, please refer to the project documentation or contact the development team.