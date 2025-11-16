# Database & Persistence Layer - Copilot Instructions

## Overview
The database layer provides multi-provider persistence (SQLite, MySQL, PostgreSQL) with automatic schema migrations, connection pooling, and a simplified query interface.

## Architecture

### Key Components
```
infrastructure/db/
├── DatabaseManager.java      # Central coordinator & initialization
├── DataSourceProvider.java   # Connection pooling (HikariCP)
├── Db.java                   # Simplified query interface
├── ConfigLoader.java         # Database configuration loader
├── DatabaseConfig.java       # Configuration holder
└── MigrationRunner.java      # Schema migration management
```

### Schema Migrations
**Location:** `src/main/resources/migrations/`

**Naming Convention:** `VX__description.sql` where X is version number
- `V1__init.sql` - Initial schema
- `V2__feature_name.sql` - Next migration
- etc.

**Migration Tracking:** `schema_version` table
- Stores applied migration version
- Prevents duplicate migrations
- Records timestamps and checksums

## Database Providers

### SQLite (Default)
**Configuration:** `config.yml`
```yaml
database:
  provider: sqlite
  sqlite:
    file: "plugins/QuickStocks/data.db"
```

**Pros:**
- No external dependencies
- Easy setup
- Good for single-server
- File-based persistence

**Cons:**
- Single connection
- Not suitable for multi-server
- Limited concurrent access

### MySQL
**Configuration:** `config.yml`
```yaml
database:
  provider: mysql
  mysql:
    host: localhost
    port: 3306
    database: quickstocks
    username: root
    password: password
    properties:
      useSSL: false
      serverTimezone: UTC
```

**Pros:**
- Multi-server support
- Better concurrent access
- Proven reliability
- Good performance

**Setup:**
```sql
CREATE DATABASE quickstocks;
CREATE USER 'quickstocks'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON quickstocks.* TO 'quickstocks'@'localhost';
FLUSH PRIVILEGES;
```

### PostgreSQL
**Configuration:** `config.yml`
```yaml
database:
  provider: postgres
  postgres:
    host: localhost
    port: 5432
    database: quickstocks
    username: postgres
    password: password
    properties:
      ssl: false
```

**Pros:**
- Advanced features
- Excellent performance
- JSON support
- Strong ACID compliance

**Setup:**
```sql
CREATE DATABASE quickstocks;
CREATE USER quickstocks WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE quickstocks TO quickstocks;
```

## Database Schema

### Core Tables

**instruments** - Instrument registry
```sql
CREATE TABLE instruments (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    symbol TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    mc_material TEXT,
    decimals INTEGER DEFAULT 2,
    created_by TEXT,
    created_at INTEGER NOT NULL
);
```

**instrument_state** - Current market state
```sql
CREATE TABLE instrument_state (
    instrument_id TEXT PRIMARY KEY,
    current_price REAL NOT NULL,
    volume_24h REAL DEFAULT 0,
    change_1h REAL DEFAULT 0,
    change_24h REAL DEFAULT 0,
    volatility_24h REAL DEFAULT 0,
    market_cap REAL DEFAULT 0,
    last_updated INTEGER NOT NULL,
    FOREIGN KEY (instrument_id) REFERENCES instruments(id)
);
```

**instrument_price_history** - Historical data
```sql
CREATE TABLE instrument_price_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    instrument_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    price REAL NOT NULL,
    volume REAL DEFAULT 0,
    reason TEXT,
    FOREIGN KEY (instrument_id) REFERENCES instruments(id)
);
CREATE INDEX idx_price_history_instrument ON instrument_price_history(instrument_id);
CREATE INDEX idx_price_history_timestamp ON instrument_price_history(timestamp);
```

**holdings** - Player positions
```sql
CREATE TABLE holdings (
    player_uuid TEXT NOT NULL,
    instrument_id TEXT NOT NULL,
    quantity REAL NOT NULL,
    avg_buy_price REAL NOT NULL,
    last_updated INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, instrument_id),
    FOREIGN KEY (instrument_id) REFERENCES instruments(id)
);
```

**companies** - Company registry
```sql
CREATE TABLE companies (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    type TEXT NOT NULL,
    owner_uuid TEXT NOT NULL,
    balance REAL DEFAULT 0,
    created_at INTEGER NOT NULL,
    symbol TEXT,
    market_enabled INTEGER DEFAULT 0,
    share_price REAL,
    total_shares REAL
);
```

**wallets** - Player balances
```sql
CREATE TABLE wallets (
    player_uuid TEXT PRIMARY KEY,
    balance REAL NOT NULL DEFAULT 0
);
```

**watchlists** - Player watchlists
```sql
CREATE TABLE watchlists (
    player_uuid TEXT NOT NULL,
    instrument_id TEXT NOT NULL,
    added_at INTEGER NOT NULL,
    PRIMARY KEY (player_uuid, instrument_id),
    FOREIGN KEY (instrument_id) REFERENCES instruments(id)
);
```

### Company Tables
See company-management.md for:
- `company_employees`
- `company_jobs`
- `company_invitations`
- `company_tx`
- `company_plots`
- `company_salaries`
- `company_salary_payments`

## Database Manager

### Initialization
**Location:** `QuickStocksPlugin.onEnable()`
```java
DatabaseManager dbManager = new DatabaseManager(configFile);
dbManager.initialize();
Db database = dbManager.getDb();
```

### Connection Pooling
Uses HikariCP for connection management:
- Automatic connection reuse
- Connection validation
- Leak detection
- Performance monitoring

**Configuration:**
```java
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

## Query Interface (Db.java)

### Simple Queries
```java
// Get single value
Double balance = database.queryValue(
    "SELECT balance FROM wallets WHERE player_uuid = ?",
    playerUuid
);

// Get single row
Map<String, Object> row = database.querySingle(
    "SELECT * FROM companies WHERE id = ?",
    companyId
);

// Get multiple rows
List<Map<String, Object>> results = database.query(
    "SELECT * FROM instruments WHERE type = ?",
    instrumentType
);
```

### Updates/Inserts
```java
// Execute update
int rowsAffected = database.execute(
    "UPDATE wallets SET balance = ? WHERE player_uuid = ?",
    newBalance,
    playerUuid
);

// Insert with generated key
String generatedId = database.executeAndReturnKey(
    "INSERT INTO instruments (id, symbol, display_name, type, created_at) VALUES (?, ?, ?, ?, ?)",
    UUID.randomUUID().toString(),
    symbol,
    displayName,
    type,
    System.currentTimeMillis()
);
```

### Batch Operations
```java
database.executeBatch(
    "INSERT INTO holdings (player_uuid, instrument_id, quantity, avg_buy_price, last_updated) VALUES (?, ?, ?, ?, ?)",
    batch // List of Object[] for each row
);
```

### Transactions
```java
try {
    database.beginTransaction();
    
    // Multiple operations
    database.execute("UPDATE ...", ...);
    database.execute("INSERT ...", ...);
    
    database.commitTransaction();
} catch (Exception e) {
    database.rollbackTransaction();
    throw e;
}
```

## Migration System

### Creating Migrations
1. Create new file: `src/main/resources/migrations/VX__description.sql`
2. Increment version number from last migration
3. Write SQL for new schema changes
4. Test locally before committing

**Example:** `V2__add_company_plots.sql`
```sql
-- Add company plots table
CREATE TABLE company_plots (
    id TEXT PRIMARY KEY,
    company_id TEXT NOT NULL,
    world TEXT NOT NULL,
    chunk_x INTEGER NOT NULL,
    chunk_z INTEGER NOT NULL,
    purchased_at INTEGER NOT NULL,
    purchase_price REAL NOT NULL,
    FOREIGN KEY (company_id) REFERENCES companies(id),
    UNIQUE (world, chunk_x, chunk_z)
);

-- Add indexes
CREATE INDEX idx_company_plots_company ON company_plots(company_id);
CREATE INDEX idx_company_plots_location ON company_plots(world, chunk_x, chunk_z);
```

### Migration Process
**Automatic on plugin startup:**
1. MigrationRunner checks `schema_version` table
2. Finds unapplied migrations in `migrations/` folder
3. Applies migrations in order (V1, V2, V3...)
4. Records each migration in `schema_version`
5. Logs success/failure

**Manual Migration:**
```java
MigrationRunner migrationRunner = new MigrationRunner(database);
migrationRunner.runMigrations();
```

### Migration Safety
- Migrations run in transactions (rollback on error)
- Version numbers must be unique and sequential
- Failed migrations prevent plugin startup
- Migration checksums prevent tampering

## Configuration Loading

### From config.yml
```java
DatabaseConfig config = ConfigLoader.loadFromYaml(configFile);
```

### From Properties File
```java
DatabaseConfig config = ConfigLoader.loadFromProperties(propsFile);
```

### Environment Variables
```java
DatabaseConfig config = ConfigLoader.loadFromEnvironment();
```

**Environment Variables:**
- `QS_DB_PROVIDER` - sqlite, mysql, or postgres
- `QS_DB_HOST` - Database host
- `QS_DB_PORT` - Database port
- `QS_DB_NAME` - Database name
- `QS_DB_USER` - Database username
- `QS_DB_PASSWORD` - Database password

## Development Guidelines

### Adding New Tables
1. Create migration file: `VX__add_new_table.sql`
2. Define schema with proper types
3. Add foreign keys where appropriate
4. Create indexes for frequently queried columns
5. Test migration locally
6. Update documentation

### Modifying Existing Tables
1. Create migration file: `VX__modify_table.sql`
2. Use `ALTER TABLE` statements
3. Consider data migration if changing types
4. Test with existing data
5. Document breaking changes

### Query Optimization
1. **Use Indexes:** Add for foreign keys and WHERE clauses
2. **Limit Results:** Use `LIMIT` for large datasets
3. **Select Specific Columns:** Avoid `SELECT *`
4. **Batch Operations:** Use `executeBatch()` for multiple inserts
5. **Connection Reuse:** Use connection pooling

### Testing Database Code
```java
@BeforeEach
void setUp() throws Exception {
    // Use in-memory SQLite for tests
    database = new Db(DataSourceProvider.createSQLiteDataSource(":memory:"));
    
    // Run migrations
    MigrationRunner migrationRunner = new MigrationRunner(database);
    migrationRunner.runMigrations();
}

@Test
void testDatabaseOperation() throws Exception {
    // Test your database operations
    database.execute("INSERT INTO ...", ...);
    
    Object result = database.queryValue("SELECT ...", ...);
    assertEquals(expectedValue, result);
}
```

## Common Patterns

### Checking if Record Exists
```java
Integer count = database.queryValue(
    "SELECT COUNT(*) FROM companies WHERE name = ?",
    companyName
);
boolean exists = (count != null && count > 0);
```

### Getting or Creating Record
```java
Map<String, Object> wallet = database.querySingle(
    "SELECT * FROM wallets WHERE player_uuid = ?",
    playerUuid
);

if (wallet == null) {
    database.execute(
        "INSERT INTO wallets (player_uuid, balance) VALUES (?, ?)",
        playerUuid,
        0.0
    );
}
```

### Updating with Timestamp
```java
database.execute(
    "UPDATE instrument_state SET current_price = ?, last_updated = ? WHERE instrument_id = ?",
    newPrice,
    System.currentTimeMillis(),
    instrumentId
);
```

### Safe Delete (Soft Delete Pattern)
Instead of:
```java
database.execute("DELETE FROM holdings WHERE quantity = 0");
```

Consider:
```java
database.execute(
    "UPDATE holdings SET quantity = 0, deleted_at = ? WHERE quantity = 0",
    System.currentTimeMillis()
);
```

## Performance Considerations

### Connection Pooling
- Pool size based on concurrent load
- Monitor connection leaks
- Use try-with-resources for connections

### Index Strategy
- Index all foreign keys
- Index columns used in WHERE clauses
- Index columns used in JOIN conditions
- Avoid over-indexing (slows writes)

### Query Optimization
- Use prepared statements (automatic in Db.java)
- Batch inserts for bulk data
- Avoid N+1 query problems
- Cache frequently accessed data

### Database Size Management
- Archive old price history periodically
- Clean up deleted records
- Monitor database size
- Optimize tables regularly (MySQL/PostgreSQL)

## Troubleshooting

### Migration Failures
1. Check migration file syntax
2. Verify version number is sequential
3. Check database permissions
4. Look for conflicts with existing schema
5. Review logs for SQL errors

### Connection Issues
1. Verify database is running
2. Check connection credentials
3. Verify network connectivity
4. Check firewall rules
5. Review connection pool configuration

### Performance Issues
1. Add missing indexes
2. Optimize slow queries
3. Increase connection pool size
4. Check for connection leaks
5. Monitor query execution time

### Data Integrity Issues
1. Check foreign key constraints
2. Verify unique constraints
3. Look for null violations
4. Review transaction handling
5. Check for race conditions

## Related Documentation
- Main instructions: `.github/copilot-instructions.md`
- Market trading: `.github/copilot/features/market-trading.md`
- Company management: `.github/copilot/features/company-management.md`
