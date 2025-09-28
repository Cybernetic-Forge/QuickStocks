package com.example.quickstocks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Internationalization utility for QuickStocks plugin.
 * Provides translation functionality with fallback support and placeholder substitution.
 */
public class I18n {
    
    private static final Logger logger = Logger.getLogger(I18n.class.getName());
    private static final Map<String, String> translations = new HashMap<>();
    private static final Map<String, String> fallbacks = new HashMap<>();
    private static JavaPlugin plugin;
    
    // Default English translations (fallbacks)
    static {
        initializeFallbacks();
    }
    
    /**
     * Initializes the I18n system with the plugin instance.
     */
    public static void initialize(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        loadTranslations();
    }
    
    /**
     * Translates a key with optional placeholder substitution.
     * Falls back to English default if key is not found.
     * 
     * @param key The translation key
     * @param placeholders Map of placeholders to substitute
     * @return Translated string with placeholders replaced
     */
    public static String tr(String key, Map<String, Object> placeholders) {
        String translation = translations.getOrDefault(key, fallbacks.get(key));
        
        if (translation == null) {
            logger.warning("Translation key not found: " + key);
            return key; // Return the key itself as last resort
        }
        
        // Replace placeholders
        if (placeholders != null) {
            for (Map.Entry<String, Object> entry : placeholders.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                String value = String.valueOf(entry.getValue());
                translation = translation.replace(placeholder, value);
            }
        }
        
        return translation;
    }
    
    /**
     * Translates a key without placeholders.
     */
    public static String tr(String key) {
        return tr(key, null);
    }
    
    /**
     * Translates a key and returns it as an Adventure Component with color codes parsed.
     */
    public static Component component(String key, Map<String, Object> placeholders) {
        String translated = tr(key, placeholders);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(translated);
    }
    
    /**
     * Translates a key and returns it as an Adventure Component without placeholders.
     */
    public static Component component(String key) {
        return component(key, null);
    }
    
    /**
     * Reloads translations from the file system.
     */
    public static void reload() {
        translations.clear();
        loadTranslations();
        logger.info("Translations reloaded successfully");
    }
    
    /**
     * Loads translations from the Translations.yml file.
     */
    private static void loadTranslations() {
        if (plugin == null) {
            logger.warning("I18n not initialized with plugin instance");
            return;
        }
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File translationsFile = new File(dataFolder, "Translations.yml");
        
        // Create default translations file if it doesn't exist
        if (!translationsFile.exists()) {
            createDefaultTranslationsFile(translationsFile);
        }
        
        // Load translations using simple properties format
        // (Note: This is a simplified YAML reader since we want minimal dependencies)
        loadFromFile(translationsFile);
    }
    
    /**
     * Creates the default Translations.yml file with English translations.
     */
    private static void createDefaultTranslationsFile(File file) {
        try (InputStream defaultTranslations = I18n.class.getResourceAsStream("/Translations.yml")) {
            if (defaultTranslations != null) {
                Files.copy(defaultTranslations, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("Created default Translations.yml file");
            } else {
                // Create from fallbacks if resource not found
                createFromFallbacks(file);
            }
        } catch (IOException e) {
            logger.severe("Failed to create default translations file: " + e.getMessage());
            createFromFallbacks(file);
        }
    }
    
    /**
     * Creates translations file from in-memory fallbacks.
     */
    private static void createFromFallbacks(File file) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("# QuickStocks Plugin Translations\n");
            content.append("# \n");
            content.append("# Placeholders: Use {placeholder} format, e.g., {symbol}, {price}, {balance}\n");
            content.append("# Color codes: Use & format, e.g., &a for green, &c for red, &b for aqua\n");
            content.append("# \n");
            content.append("# Available placeholders:\n");
            content.append("# - {symbol}: Stock/crypto symbol\n");
            content.append("# - {name}: Display name\n");
            content.append("# - {price}: Price value\n");
            content.append("# - {balance}: Player balance\n");
            content.append("# - {qty}: Quantity\n");
            content.append("# - {change}: Percentage change\n");
            content.append("# - {rank}: Ranking number\n");
            content.append("# - {needed}: Amount needed\n");
            content.append("# - {have}: Amount player has\n");
            content.append("# - {owned}: Amount player owns\n");
            content.append("# - {side}: Buy/sell side\n");
            content.append("# - {query}: Search query\n");
            content.append("\n");
            
            // Convert fallbacks to YAML-like format
            for (Map.Entry<String, String> entry : fallbacks.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().replace("\"", "\\\""); // Escape quotes
                
                // Create nested structure for better organization
                if (key.contains(".")) {
                    String[] parts = key.split("\\.", 2);
                    content.append(parts[0]).append(":\n");
                    content.append("  ").append(parts[1]).append(": \"").append(value).append("\"\n");
                } else {
                    content.append(key).append(": \"").append(value).append("\"\n");
                }
            }
            
            Files.write(file.toPath(), content.toString().getBytes());
            logger.info("Created Translations.yml from fallbacks");
        } catch (IOException e) {
            logger.severe("Failed to create translations file from fallbacks: " + e.getMessage());
        }
    }
    
    /**
     * Loads translations from file using a simple YAML parser.
     */
    private static void loadFromFile(File file) {
        try {
            String content = Files.readString(file.toPath());
            parseYaml(content);
            logger.info("Loaded " + translations.size() + " translations");
        } catch (IOException e) {
            logger.warning("Failed to load translations file, using fallbacks: " + e.getMessage());
        }
    }
    
    /**
     * Simple YAML parser for our specific translation format.
     */
    private static void parseYaml(String content) {
        String[] lines = content.split("\n");
        String currentSection = null;
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Check for section headers (e.g., "general:")
            if (line.endsWith(":") && !line.contains(" ")) {
                currentSection = line.substring(0, line.length() - 1);
                continue;
            }
            
            // Parse key-value pairs
            int colonIndex = line.indexOf(":");
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                
                // Remove quotes from value
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // Build full key with section prefix
                String fullKey = currentSection != null ? currentSection + "." + key : key;
                translations.put(fullKey, value);
            }
        }
    }
    
    /**
     * Initializes fallback translations (English defaults).
     */
    private static void initializeFallbacks() {
        // General messages
        fallbacks.put("general.no_permission", "&cYou don't have permission.");
        fallbacks.put("general.reloaded", "&aConfiguration reloaded.");
        fallbacks.put("general.invalid_number", "&cPlease enter a valid number.");
        fallbacks.put("general.only_players", "&cOnly players can use this command.");
        
        // Market messages
        fallbacks.put("market.open_title_items", "&bMarket — Items");
        fallbacks.put("market.open_title_crypto", "&bMarket — Crypto");
        fallbacks.put("market.balance", "&7Balance: &a{balance}");
        fallbacks.put("market.buy_title", "&aBuy {symbol}");
        fallbacks.put("market.sell_title", "&cSell {symbol}");
        fallbacks.put("market.qty_prompt", "&7Enter quantity:");
        fallbacks.put("market.confirm_buy", "&aConfirmed buy: {qty} × {symbol} @ {price}");
        fallbacks.put("market.confirm_sell", "&cConfirmed sell: {qty} × {symbol} @ {price}");
        fallbacks.put("market.insufficient_funds", "&cNot enough funds. Need {needed}, have {have}.");
        fallbacks.put("market.insufficient_holdings", "&cYou only own {owned} {symbol}.");
        fallbacks.put("market.order_logged", "&7Order saved: {side} {qty} {symbol}.");
        
        // Stocks command messages
        fallbacks.put("stocks.top10_header", "&6📈 TOP 10 GAINERS (24H)");
        fallbacks.put("stocks.no_stocks", "&6📊 No stocks found in the market.");
        fallbacks.put("stocks.header_rank", "RANK");
        fallbacks.put("stocks.header_symbol", "SYMBOL");
        fallbacks.put("stocks.header_name", "NAME");
        fallbacks.put("stocks.header_price", "PRICE");
        fallbacks.put("stocks.header_change", "24H CHANGE");
        fallbacks.put("stocks.not_found", "&cInstrument '{query}' not found.");
        fallbacks.put("stocks.card_header", "&6📊 {name} ({symbol}) [{type}]");
        fallbacks.put("stocks.current_price", "&e💲 Current Price: &f${price}");
        fallbacks.put("stocks.change_1h", "1H Change");
        fallbacks.put("stocks.change_24h", "24H Change");
        fallbacks.put("stocks.change_7d", "7D Change");
        fallbacks.put("stocks.volume_24h", "&e📊 24H Volume: &f{volume}");
        fallbacks.put("stocks.market_cap", "&e🏦 Market Cap: &f${marketCap}");
        fallbacks.put("stocks.last_updated", "&7⏰ Last Updated: {time}");
        fallbacks.put("stocks.price_history", "&e📈 Price History (7 days): &f{sparkline}");
        
        // Crypto command messages
        fallbacks.put("crypto.title", "&6🪙 Crypto Commands");
        fallbacks.put("crypto.create_usage", "&e• /crypto create &b<symbol> <name>");
        fallbacks.put("crypto.create_description", "&7  Creates a custom cryptocurrency");
        fallbacks.put("crypto.examples_title", "&fExamples:");
        fallbacks.put("crypto.example1", "&3  /crypto create MYCOIN \"My Custom Coin\"");
        fallbacks.put("crypto.example2", "&3  /crypto create GOLD \"Digital Gold\"");
        fallbacks.put("crypto.permission_status", "&ePermission: &7{permission} &7- {status}");
        fallbacks.put("crypto.permission_granted", "&a✅ Granted");
        fallbacks.put("crypto.permission_denied", "&c❌ Denied");
        fallbacks.put("crypto.only_players", "&c❌ Only players can create custom crypto.");
        fallbacks.put("crypto.no_permission", "&c❌ You don't have permission to create custom crypto.");
        fallbacks.put("crypto.permission_required", "&7💡 Required permission: {permission}");
        fallbacks.put("crypto.usage_error", "&c❌ Usage: /crypto create <symbol> <name>");
        fallbacks.put("crypto.usage_example", "&7💡 Example: /crypto create MYCOIN \"My Custom Coin\"");
        fallbacks.put("crypto.invalid_symbol", "&c❌ Invalid symbol. Use A-Z, 0-9, underscore, 2-12 chars.");
        fallbacks.put("crypto.symbol_exists", "&c❌ Symbol '{symbol}' already exists.");
        fallbacks.put("crypto.creation_error", "&c❌ Failed to create crypto: {error}");
        fallbacks.put("crypto.success_title", "&a🎉 Custom Crypto Created Successfully!");
        fallbacks.put("crypto.success_symbol", "&e💰 Symbol: &3{symbol}");
        fallbacks.put("crypto.success_name", "&e📝 Name: &f{name}");
        fallbacks.put("crypto.success_price", "&e💲 Starting Price: &6$1.00");
        fallbacks.put("crypto.success_id", "&e🆔 Instrument ID: &8{id}");
        fallbacks.put("crypto.success_footer1", "&a💡 Your crypto is now tradeable on the market!");
        fallbacks.put("crypto.success_footer2", "&7💡 Use /stocks {symbol} to view details");
        
        // Plugin startup messages
        fallbacks.put("plugin.enabling", "QuickStocks enabling (Paper 1.21.8)...");
        fallbacks.put("plugin.enabled", "QuickStocks enabled successfully! Market is now running.");
        fallbacks.put("plugin.enable_failed", "Failed to enable QuickStocks: {error}");
        fallbacks.put("plugin.disabling", "QuickStocks disabling...");
        fallbacks.put("plugin.disabled", "QuickStocks disabled successfully.");
        fallbacks.put("plugin.initialized_stocks", "Initialized {count} default stocks");
        fallbacks.put("plugin.failed_initialize_stocks", "Failed to initialize default stocks: {error}");
    }
}