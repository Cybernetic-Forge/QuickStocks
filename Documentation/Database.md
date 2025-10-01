# Database Management Guide

This guide covers database administration, maintenance, and troubleshooting for QuickStocks.

## 📋 Overview

QuickStocks uses a relational database to store:
- Instrument data (stocks, crypto, company shares)
- Price history
- Player portfolios and holdings
- Company information
- Transactions and order history
- Watchlists

---

## 🗄️ Database Schema

### Core Tables

**Instruments and Market Data:**
- `instruments` - All tradeable instruments
- `instrument_state` - Current prices and statistics
- `instrument_price_history` - Historical price data

**Trading:**
- `orders` - Trade history
- `user_holdings` - Player portfolios

**Companies:**
- `companies` - Company registry
- `company_jobs` - Job titles and permissions
- `company_employees` - Employee memberships
- `company_invitations` - Invitation tracking
- `company_tx` - Financial transactions
- `company_shareholders` - Share ownership
- `company_share_tx` - Share trade history
- `player_notifications` - Offline notifications

**Player Data:**
- `wallets` - Player wallet balances (if not using Vault)
- `watchlists` - Player watchlists

**System:**
- `schema_version` - Migration tracking

---

## 🔧 Database Maintenance

### Audit Command

Check database integrity:

```bash
/stocks audit
```

**What it checks:**
- Missing records
- Orphaned entries
- Inconsistent data
- Corruption indicators

**Sample Output:**
```
🔍 Database Audit Report
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ Instruments: 156 records
✅ Price History: 45,678 entries
✅ State Records: 156 current
⚠️  Found 3 orphaned history entries
⚠️  Found 1 missing state record
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
💡 Use /stocks audit repair to fix issues
```

### Repair Command

Fix identified issues:

```bash
/stocks audit repair
```

**⚠️ Warning:** Always backup before repair!

**What it fixes:**
- Creates missing state records
- Removes orphaned entries
- Rebuilds indexes
- Normalizes data

---

## 💾 Backup Strategies

### SQLite Backups

**Manual Backup:**
```bash
# Stop server
stop

# Copy database file
cp plugins/QuickStocks/data.db plugins/QuickStocks/data.db.backup

# Or with timestamp
cp plugins/QuickStocks/data.db backups/data-$(date +%Y%m%d-%H%M%S).db

# Start server
./start.sh
```

**Automated Backup Script:**
```bash
#!/bin/bash
# backup-quickstocks.sh

BACKUP_DIR="/path/to/backups"
DB_FILE="/path/to/server/plugins/QuickStocks/data.db"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Create backup
cp "$DB_FILE" "$BACKUP_DIR/quickstocks-$TIMESTAMP.db"

# Keep only last 7 days
find "$BACKUP_DIR" -name "quickstocks-*.db" -mtime +7 -delete

echo "Backup completed: quickstocks-$TIMESTAMP.db"
```

**Schedule with cron:**
```bash
# Edit crontab
crontab -e

# Add daily backup at 3 AM
0 3 * * * /path/to/backup-quickstocks.sh
```

---

### MySQL Backups

**Manual Backup:**
```bash
# Full database dump
mysqldump -u quickstocks -p quickstocks > quickstocks-backup.sql

# With timestamp
mysqldump -u quickstocks -p quickstocks > quickstocks-$(date +%Y%m%d-%H%M%S).sql

# Compressed backup
mysqldump -u quickstocks -p quickstocks | gzip > quickstocks-backup.sql.gz
```

**Restore from Backup:**
```bash
# Stop server first
stop

# Restore database
mysql -u quickstocks -p quickstocks < quickstocks-backup.sql

# Or from compressed
gunzip < quickstocks-backup.sql.gz | mysql -u quickstocks -p quickstocks

# Start server
./start.sh
```

**Automated Backup Script:**
```bash
#!/bin/bash
# mysql-backup.sh

BACKUP_DIR="/path/to/backups"
DB_USER="quickstocks"
DB_PASS="your_password"
DB_NAME="quickstocks"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

# Create backup
mysqldump -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" | gzip > "$BACKUP_DIR/quickstocks-$TIMESTAMP.sql.gz"

# Keep only last 30 days
find "$BACKUP_DIR" -name "quickstocks-*.sql.gz" -mtime +30 -delete

echo "MySQL backup completed: quickstocks-$TIMESTAMP.sql.gz"
```

---

### PostgreSQL Backups

**Manual Backup:**
```bash
# Full database dump
pg_dump -U quickstocks quickstocks > quickstocks-backup.sql

# With timestamp
pg_dump -U quickstocks quickstocks > quickstocks-$(date +%Y%m%d-%H%M%S).sql

# Compressed backup
pg_dump -U quickstocks quickstocks | gzip > quickstocks-backup.sql.gz

# Custom format (faster restore)
pg_dump -U quickstocks -Fc quickstocks > quickstocks-backup.dump
```

**Restore from Backup:**
```bash
# Stop server
stop

# Restore (SQL format)
psql -U quickstocks quickstocks < quickstocks-backup.sql

# Restore (custom format)
pg_restore -U quickstocks -d quickstocks quickstocks-backup.dump

# Start server
./start.sh
```

---

## 🔄 Database Migrations

### How Migrations Work

QuickStocks uses versioned migrations to update the database schema:

1. **Version Check:** Plugin checks current schema version
2. **Migration Scan:** Finds unapplied migrations
3. **Sequential Apply:** Runs migrations in order
4. **Version Update:** Records completed migrations

**Migration Files:**
```
plugins/QuickStocks/migrations/
├── V1__init.sql              # Initial schema
├── V2__add_watchlists.sql    # Watchlist feature
├── V3__add_holdings.sql      # Holdings tracking
├── V4__add_wallet.sql        # Wallet system
├── V5__update_jobs.sql       # Job permissions
├── V6__add_analytics.sql     # Analytics tables
├── V7__companies.sql         # Company system
└── V9__company_market.sql    # Company IPO
```

### Viewing Migration Status

**Check applied migrations:**
```sql
-- SQLite/MySQL/PostgreSQL
SELECT * FROM schema_version ORDER BY version;
```

**Sample Output:**
```
version | description        | applied_at
--------|-------------------|-------------------
1       | init              | 2024-01-15 10:00:00
2       | add_watchlists    | 2024-01-15 10:00:01
3       | add_holdings      | 2024-01-15 10:00:02
...
```

### Manual Migration (Advanced)

**⚠️ Only if automatic migration fails!**

```bash
# Stop server
stop

# Backup database
cp data.db data.db.backup

# Apply migration manually (SQLite example)
sqlite3 plugins/QuickStocks/data.db < plugins/QuickStocks/migrations/V8__next_migration.sql

# Update version table
sqlite3 plugins/QuickStocks/data.db "INSERT INTO schema_version VALUES (8, 'description', datetime('now'));"

# Start server
./start.sh
```

---

## 📊 Database Optimization

### SQLite Optimization

**Vacuum Database:**
```bash
# Reduces file size and improves performance
sqlite3 plugins/QuickStocks/data.db "VACUUM;"
```

**Analyze Database:**
```bash
# Updates query optimizer statistics
sqlite3 plugins/QuickStocks/data.db "ANALYZE;"
```

**Check Integrity:**
```bash
sqlite3 plugins/QuickStocks/data.db "PRAGMA integrity_check;"
```

---

### MySQL Optimization

**Optimize Tables:**
```sql
-- Optimize all QuickStocks tables
OPTIMIZE TABLE instruments, instrument_state, instrument_price_history, 
               companies, company_employees, orders, user_holdings;
```

**Analyze Tables:**
```sql
ANALYZE TABLE instruments, instrument_state, orders;
```

**Check Table Status:**
```sql
SHOW TABLE STATUS WHERE Name LIKE '%';
```

**Repair Tables (if corrupted):**
```sql
REPAIR TABLE instruments;
```

---

### PostgreSQL Optimization

**Vacuum Database:**
```sql
-- Standard vacuum
VACUUM ANALYZE;

-- Full vacuum (locks tables)
VACUUM FULL;
```

**Reindex:**
```sql
REINDEX DATABASE quickstocks;
```

**Update Statistics:**
```sql
ANALYZE;
```

---

## 🔍 Monitoring and Queries

### Check Database Size

**SQLite:**
```bash
ls -lh plugins/QuickStocks/data.db
# Or detailed
sqlite3 plugins/QuickStocks/data.db "SELECT page_count * page_size as size FROM pragma_page_count(), pragma_page_size();"
```

**MySQL:**
```sql
SELECT 
  table_name AS 'Table',
  ROUND((data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
FROM information_schema.tables
WHERE table_schema = 'quickstocks'
ORDER BY (data_length + index_length) DESC;
```

**PostgreSQL:**
```sql
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Useful Queries

**Total Instruments:**
```sql
SELECT COUNT(*) FROM instruments;
```

**Total Players:**
```sql
SELECT COUNT(DISTINCT player_uuid) FROM user_holdings;
```

**Price History Size:**
```sql
SELECT COUNT(*) FROM instrument_price_history;
```

**Recent Trades:**
```sql
SELECT * FROM orders 
ORDER BY ts DESC 
LIMIT 10;
```

**Top Holdings:**
```sql
SELECT i.symbol, SUM(h.qty) as total_shares
FROM user_holdings h
JOIN instruments i ON h.instrument_id = i.id
GROUP BY i.symbol
ORDER BY total_shares DESC
LIMIT 10;
```

**Companies by Balance:**
```sql
SELECT name, type, balance, owner_uuid
FROM companies
ORDER BY balance DESC
LIMIT 10;
```

---

## 🔧 Troubleshooting

### Database Locked (SQLite)

**Error:** `database is locked`

**Causes:**
- Multiple processes accessing database
- Backup process running
- Server crash with lock file

**Solutions:**
```bash
# Stop server
stop

# Remove lock file if exists
rm -f plugins/QuickStocks/data.db-shm
rm -f plugins/QuickStocks/data.db-wal

# Start server
./start.sh
```

### Connection Pool Exhausted

**Error:** `Could not get JDBC Connection`

**Solutions:**
1. Increase max connections in database server
2. Reduce player count
3. Optimize slow queries
4. Check for connection leaks in logs

### Corrupted Database

**SQLite:**
```bash
# Check integrity
sqlite3 data.db "PRAGMA integrity_check;"

# If corrupted, restore from backup
cp data.db data.db.corrupted
cp data.db.backup data.db
```

**MySQL:**
```sql
-- Check table
CHECK TABLE instruments;

-- Repair if needed
REPAIR TABLE instruments;
```

### Slow Queries

**Solutions:**
1. Run `ANALYZE` to update statistics
2. Add indexes if missing
3. Optimize table structure
4. Vacuum database
5. Upgrade hardware

---

## 📈 Performance Tips

### General

1. **Regular Backups:** Daily automated backups
2. **Regular Maintenance:** Weekly VACUUM/OPTIMIZE
3. **Monitor Size:** Watch database growth
4. **Clean Old Data:** Archive old price history
5. **Index Management:** Keep indexes updated

### High-Traffic Servers

1. **Use MySQL/PostgreSQL:** Better for 100+ players
2. **Separate Database Server:** Dedicated database machine
3. **Connection Pooling:** Configure optimal pool size
4. **Read Replicas:** For read-heavy workloads
5. **Caching Layer:** Add Redis if needed

---

## 🔗 Related Documentation

- **[Installation Guide](Installation.md)** - Initial database setup
- **[Configuration Guide](Configuration.md)** - Database configuration
- **[Commands-Stocks](Commands-Stocks.md)** - Audit commands

---

*For detailed configuration, see [Configuration Guide](Configuration.md)*
