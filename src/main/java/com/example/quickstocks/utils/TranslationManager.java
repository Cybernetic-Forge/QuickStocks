package com.example.quickstocks.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * Handles loading and retrieving translated messages from Translations.yml
 */
public class TranslationManager {
    
    private final JavaPlugin plugin;
    private YamlConfiguration translations;
    
    public TranslationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadTranslations();
    }
    
    /**
     * Gets a translated message by key
     */
    public String getMessage(String key) {
        return getMessage(key, new String[0]);
    }
    
    /**
     * Gets a translated message with placeholder replacements
     */
    public String getMessage(String key, String... placeholders) {
        String message = translations.getString(key);
        
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key);
            return "Missing translation: " + key;
        }
        
        // Replace placeholders {0}, {1}, etc.
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("{" + i + "}", placeholders[i]);
        }
        
        // Replace named placeholders
        if (placeholders.length > 0) {
            // For compatibility with I18n system
            java.util.Map<String, Object> placeholderMap = new java.util.HashMap<>();
            for (int i = 0; i < placeholders.length; i++) {
                placeholderMap.put("arg" + i, placeholders[i]);
            }
            message = replacePlaceholders(message, placeholderMap);
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Gets a translated message with named placeholders
     */
    public String getMessage(String key, java.util.Map<String, Object> placeholders) {
        String message = translations.getString(key);
        
        if (message == null) {
            plugin.getLogger().warning("Missing translation key: " + key);
            // Fallback to I18n system if available
            try {
                return com.example.quickstocks.I18n.tr(key, placeholders);
            } catch (Exception e) {
                return "Missing translation: " + key;
            }
        }
        
        message = replacePlaceholders(message, placeholders);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private String replacePlaceholders(String message, java.util.Map<String, Object> placeholders) {
        for (java.util.Map.Entry<String, Object> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            message = message.replace(placeholder, value);
        }
        return message;
    }
    
    /**
     * Loads translations from file
     */
    private void loadTranslations() {
        try {
            // Copy default translations if they don't exist
            File translationsFile = new File(plugin.getDataFolder(), "Translations.yml");
            if (!translationsFile.exists()) {
                plugin.saveResource("Translations.yml", false);
            }
            
            // Load from file
            translations = YamlConfiguration.loadConfiguration(translationsFile);
            
            // Load defaults from JAR as fallback
            try (InputStreamReader reader = new InputStreamReader(
                    plugin.getResource("Translations.yml"))) {
                YamlConfiguration defaultTranslations = YamlConfiguration.loadConfiguration(reader);
                translations.setDefaults(defaultTranslations);
            }
            
            plugin.getLogger().info("Loaded translations");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load translations", e);
            // Create empty configuration as fallback
            translations = new YamlConfiguration();
        }
    }
    
    /**
     * Reloads the translations from file
     */
    public void reload() {
        loadTranslations();
    }
}