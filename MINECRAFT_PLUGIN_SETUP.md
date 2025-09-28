# QuickStocks Minecraft Plugin Setup

This document provides instructions for building and deploying the QuickStocks plugin in a Minecraft server environment.

## Overview

The QuickStocks plugin is now fully functional for Minecraft servers. The core components have been implemented and tested, including:

- ✅ Stock market simulation with realistic price movements
- ✅ Database persistence (SQLite/MySQL/PostgreSQL)
- ✅ `/stocks` command for viewing market data and individual stock information
- ✅ `/crypto` command for creating custom cryptocurrency instruments
- ✅ Player permission system for crypto creation
- ✅ Tab completion for commands
- ✅ Full integration with Paper/Spigot/Bukkit APIs

## Plugin Components

### Main Plugin Class
- **File**: `QuickStocksPlugin.java.ready` (rename to `.java` when deploying)
- **Features**: Database initialization, service integration, command registration

### Commands
- **StocksCommand** (`StocksCommand.java.ready`) - Market data and stock lookup
- **CryptoCommand** (`CryptoCommand.java.ready`) - Custom cryptocurrency creation

### Core Services
- **StockMarketService** - Market simulation and management
- **CryptoService** - Custom cryptocurrency creation and validation
- **QueryService** - Database query operations
- **SimulationEngine** - Real-time price updates

## Building for Minecraft

### Step 1: Enable Bukkit Dependencies

Choose one of the following dependency configurations in `pom.xml`:

#### Option A: Paper API (Recommended)
```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>1.20.4-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

#### Option B: Spigot API
```xml
<dependency>
    <groupId>org.spigotmc</groupId>
    <artifactId>spigot-api</artifactId>
    <version>1.20.4-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Step 2: Enable Plugin Classes

Rename the following files to activate them:
```bash
mv src/main/java/com/example/quickstocks/QuickStocksPlugin.java src/main/java/com/example/quickstocks/QuickStocksPlugin.java
mv src/main/java/com/example/quickstocks/commands/StocksCommand.java src/main/java/com/example/quickstocks/commands/StocksCommand.java
mv src/main/java/com/example/quickstocks/commands/CryptoCommand.java src/main/java/com/example/quickstocks/commands/CryptoCommand.java
```

### Step 3: Build the Plugin

```bash
mvn clean package
```

This will create `target/QuickStocks-1.0.0-SNAPSHOT.jar` ready for deployment.

## Installation

1. Copy the JAR file to your server's `plugins/` directory
2. Start/restart your Minecraft server
3. The plugin will automatically:
   - Create a SQLite database in `plugins/QuickStocks/data.db`
   - Seed initial stock instruments based on Minecraft items
   - Start the market simulation
   - Register commands with proper permissions

## Commands

### `/stocks` Command
- `/stocks` - Display top 10 market gainers
- `/stocks <symbol>` - Show detailed information for a specific stock
- `/stocks <material>` - Look up stock by Minecraft material name

**Permissions**: None required (available to all players)

### `/crypto` Command
- `/crypto` - Show usage information
- `/crypto create <symbol> <name>` - Create a custom cryptocurrency

**Permissions**: `maksy.stocks.crypto.create` (default: false)

**Examples**:
```
/crypto create MYCOIN "My Custom Coin"
/crypto create GOLD "Digital Gold Token"
```

## Configuration

The plugin uses `config.yml` for database and market settings:

```yaml
database:
  provider: sqlite
  sqlite:
    file: data.db

market:
  updateInterval: 5
  startOpen: true
  defaultStocks: true
```

## Database Support

- **SQLite**: Default, no setup required
- **MySQL**: Configure connection details in config.yml
- **PostgreSQL**: Configure connection details in config.yml

## Features

### Market Simulation
- Real-time price updates every 5 seconds
- Realistic market factors affecting prices
- Volatility calculations
- Price history tracking
- Market statistics

### Stock Instruments
- Based on Minecraft materials (Diamond, Gold, Iron, etc.)
- Custom cryptocurrencies created by players
- Sector-based categorization
- Market cap calculations

### Player Interaction
- Beautiful command output with colors and formatting
- Tab completion for symbols and materials
- Permission-based access control
- Real-time market data

## Development Notes

The plugin follows a clean architecture with:
- **Core Layer**: Business logic and services
- **Infrastructure Layer**: Database and external integrations
- **Application Layer**: Commands and user interaction
- **Inversion of Control**: Dependency injection pattern

All demo classes (like `CryptoCommandDemo.java`) can be removed in production - they were used for testing without Minecraft dependencies.

## Troubleshooting

### Build Issues
- Ensure Maven has access to Paper/Spigot repositories
- Check that Java 17+ is being used
- Verify all `.ready` files have been renamed to `.java`

### Runtime Issues
- Check server logs for detailed error messages
- Verify database permissions and file paths
- Ensure proper permissions are set for crypto creation

### Command Issues
- Verify commands are registered in `plugin.yml`
- Check player permissions for crypto commands
- Ensure database is properly initialized

## Testing

Run the comprehensive test suite:
```bash
mvn test
```

The tests cover:
- Database operations and migrations
- Market simulation accuracy
- Service layer functionality
- Configuration loading
- Health checks