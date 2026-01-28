# Database Migrations - QuickStocks Skill

**Domain**: Database Management & Schema Versioning
**Last Updated**: 2026-01-28

## Overview

QuickStocks uses a custom migration system to manage database schema changes across SQLite, MySQL, and PostgreSQL.

## Migration System Architecture

### Location
- Migration files: `src/main/resources/migrations/`
- Migration runner: `net.cyberneticforge.quickstocks.infrastructure.db.MigrationRunner`

### Naming Convention

Migration files follow this pattern:
```
V{version}__{description}.sql
```

Examples:
- `V1__init.sql` - Initial schema
- `V2__add_crypto_support.sql` - Add crypto features
- `V3__company_plots.sql` - Add company plot tables

### Version Tracking

The system maintains a `schema_version` table:

```sql
CREATE TABLE schema_version (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Writing Migrations

### Basic Structure

```sql
-- V2__add_new_feature.sql

-- Add new table
CREATE TABLE new_feature (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add index for performance
CREATE INDEX idx_new_feature_name ON new_feature(name);

-- Add column to existing table
ALTER TABLE instruments ADD COLUMN new_field TEXT;
```

### Multi-Database Compatibility

QuickStocks supports SQLite, MySQL, and PostgreSQL. Write migrations that work across all three:

#### ✅ Compatible Patterns

```sql
-- Use standard SQL types
CREATE TABLE example (
    id TEXT PRIMARY KEY,           -- Works everywhere
    count INTEGER,                 -- Works everywhere
    amount REAL,                   -- Works everywhere
    created_at TIMESTAMP           -- Works everywhere
);

-- Use CURRENT_TIMESTAMP for defaults
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
```

#### ❌ Avoid Database-Specific Features

```sql
-- Don't use MySQL-specific syntax
AUTO_INCREMENT                     -- MySQL only
SERIAL                            -- PostgreSQL only

-- Don't use SQLite-specific syntax
INTEGER PRIMARY KEY AUTOINCREMENT -- SQLite only
```

### Migration Best Practices

1. **Idempotent Operations**: Use `IF NOT EXISTS` where supported
2. **Add Indexes**: Always add indexes for foreign keys and commonly queried columns
3. **Data Migration**: Include data transformation logic if schema changes affect data
4. **Backward Compatibility**: Consider rollback scenarios
5. **Testing**: Test migrations on all three database types

### Example: Complete Migration

```sql
-- V3__add_watchlist.sql

-- Create watchlist table
CREATE TABLE watchlist (
    player_uuid TEXT NOT NULL,
    instrument_id TEXT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_uuid, instrument_id)
);

-- Add index for player lookups
CREATE INDEX idx_watchlist_player ON watchlist(player_uuid);

-- Add index for instrument lookups
CREATE INDEX idx_watchlist_instrument ON watchlist(instrument_id);
```

## Migration Runner

### Automatic Execution

Migrations run automatically on plugin startup:

```java
MigrationRunner migrationRunner = new MigrationRunner(dataSource);
migrationRunner.runMigrations();
```

### Migration Process

1. Check current schema version
2. Load migration files from resources
3. Sort migrations by version number
4. Execute unapplied migrations in order
5. Update schema_version table
6. Log results

### Error Handling

- Failed migrations are logged but don't prevent plugin startup
- Partial migrations may leave database in inconsistent state
- Always test migrations thoroughly before deployment

## Testing Migrations

### Manual Testing Checklist

1. Test on SQLite (default)
2. Test on MySQL
3. Test on PostgreSQL
4. Test on existing database (upgrade scenario)
5. Test on fresh database (new install scenario)
6. Verify data integrity after migration
7. Check index creation
8. Validate foreign key constraints

### Automated Testing

```java
@Test
void testMigration() {
    // Setup test database
    DataSource dataSource = createTestDataSource();
    
    // Run migrations
    MigrationRunner runner = new MigrationRunner(dataSource);
    runner.runMigrations();
    
    // Verify schema
    // Check that tables and columns exist
    // Validate indexes were created
}
```

## Common Patterns

### Adding a New Table

```sql
CREATE TABLE new_table (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_new_table_name ON new_table(name);
```

### Adding a Column

```sql
ALTER TABLE existing_table 
ADD COLUMN new_column TEXT;
```

### Creating an Index

```sql
CREATE INDEX idx_table_column ON table_name(column_name);
```

### Adding a Composite Index

```sql
CREATE INDEX idx_table_multi ON table_name(column1, column2);
```

## Rollback Strategy

Currently, QuickStocks doesn't support automatic rollback. For manual rollback:

1. Create a reverse migration manually
2. Execute against database
3. Update schema_version table
4. Restart plugin

## Troubleshooting

### Migration Fails to Apply

- Check SQL syntax for all three database types
- Verify table/column doesn't already exist
- Check for naming conflicts
- Review logs for specific error messages

### Performance Issues

- Add appropriate indexes
- Consider batch operations for large data migrations
- Test with realistic data volumes

### Data Loss Prevention

- Always backup database before migrations
- Test migrations on copy of production database
- Include data transformation logic when restructuring tables

## Resources

- Database schema: `Documentation/Database.md`
- Migration runner: `src/main/java/net/cyberneticforge/quickstocks/infrastructure/db/MigrationRunner.java`
- Existing migrations: `src/main/resources/migrations/`
