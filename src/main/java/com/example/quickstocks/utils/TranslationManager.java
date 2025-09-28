package com.example.quickstocks.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
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
     * Loads translations from Translations.yml file
     */
    private void loadTranslations() {
        File translationsFile = new File(plugin.getDataFolder(), "Translations.yml");
        
        // Create default file if it doesn't exist
        if (!translationsFile.exists()) {
            try {
                plugin.saveResource("Translations.yml", false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not save default Translations.yml", e);
            }
        }
        
        // Load the translations
        try {
            translations = YamlConfiguration.loadConfiguration(translationsFile);
            
            // Load defaults from JAR if available
            InputStream defaultStream = plugin.getResource("Translations.yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                translations.setDefaults(defaultConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Translations.yml", e);
            translations = new YamlConfiguration(); // Use empty config as fallback
        }
    }
    
    /**
     * Gets a translated message with color codes converted
     * @param path The path to the message in the YAML file
     * @return The translated message with colors, or the path if not found
     */
    public String getMessage(String path) {
        String message = translations.getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Gets a translated message with placeholder replacements
     * @param path The path to the message in the YAML file
     * @param placeholders Key-value pairs for placeholder replacement
     * @return The translated message with colors and placeholders replaced
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        // Replace placeholders in format {key} with values
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = "{" + placeholders[i] + "}";
            String value = placeholders[i + 1];
            message = message.replace(placeholder, value);
        }
        
        return message;
    }
    
    /**
     * Reloads the translations from file
     */
    public void reload() {
        loadTranslations();
    }
}