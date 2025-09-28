#!/bin/bash

# QuickStocks Plugin Activation Script
# This script prepares the plugin for Minecraft deployment

echo "🚀 QuickStocks Plugin Activation Script"
echo "======================================"
echo

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    echo "❌ Error: pom.xml not found. Please run this script from the project root directory."
    exit 1
fi

echo "📦 Step 1: Enabling Bukkit API dependency..."

# Enable Paper API dependency in pom.xml
if grep -q "<!-- Paper API for Minecraft" pom.xml; then
    sed -i 's/<!-- Paper API for Minecraft.*/<!-- Paper API for Minecraft (enabled for deployment) -->/g' pom.xml
    sed -i 's/<!--$/<!-- enabled -->/g' pom.xml
    sed -i 's/-->/<!-- enabled -->/g' pom.xml
    
    # Uncomment the dependency block
    sed -i '/<!-- Paper API for Minecraft/,/-->/{/<!--/d; /-->/d;}' pom.xml
    echo "✅ Paper API dependency enabled"
else
    echo "✅ Paper API dependency already enabled"
fi

echo
echo "📝 Step 2: Activating plugin classes..."

# Activate main plugin class
if [ -f "src/main/java/com/example/quickstocks/QuickStocksPlugin.java.ready" ]; then
    mv "src/main/java/com/example/quickstocks/QuickStocksPlugin.java.ready" "src/main/java/com/example/quickstocks/QuickStocksPlugin.java"
    echo "✅ QuickStocksPlugin.java activated"
else
    echo "✅ QuickStocksPlugin.java already active"
fi

# Activate command classes
if [ -f "src/main/java/com/example/quickstocks/commands/StocksCommand.java.ready" ]; then
    mv "src/main/java/com/example/quickstocks/commands/StocksCommand.java.ready" "src/main/java/com/example/quickstocks/commands/StocksCommand.java"
    echo "✅ StocksCommand.java activated"
else
    echo "✅ StocksCommand.java already active"
fi

if [ -f "src/main/java/com/example/quickstocks/commands/CryptoCommand.java.ready" ]; then
    mv "src/main/java/com/example/quickstocks/commands/CryptoCommand.java.ready" "src/main/java/com/example/quickstocks/commands/CryptoCommand.java"
    echo "✅ CryptoCommand.java activated"
else
    echo "✅ CryptoCommand.java already active"
fi

echo
echo "🔨 Step 3: Building plugin JAR..."

# Build the plugin
if mvn clean package -q; then
    echo "✅ Plugin built successfully!"
    echo
    echo "📁 Plugin JAR location: target/QuickStocks-1.0.0-SNAPSHOT.jar"
    echo
    echo "🎯 Next Steps:"
    echo "1. Copy target/QuickStocks-1.0.0-SNAPSHOT.jar to your server's plugins/ directory"
    echo "2. Start/restart your Minecraft server"
    echo "3. The plugin will automatically create database and register commands"
    echo
    echo "📋 Available Commands:"
    echo "• /stocks - View market overview and stock details"
    echo "• /crypto create <symbol> <name> - Create custom cryptocurrencies"
    echo
    echo "🔐 Permissions:"
    echo "• maksy.stocks.crypto.create - Required for crypto creation (default: false)"
    echo
    echo "🎉 Plugin is ready for Minecraft deployment!"
else
    echo "❌ Build failed. Please check for errors above."
    echo
    echo "🔧 Troubleshooting:"
    echo "• Ensure you have Java 17+ installed"
    echo "• Check internet connectivity for Maven dependencies"
    echo "• Verify Paper/Spigot repositories are accessible"
    exit 1
fi