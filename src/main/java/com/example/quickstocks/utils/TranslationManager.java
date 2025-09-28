package com.example.quickstocks.utils;

import com.example.quickstocks.I18n;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * Wrapper around I18n for backward compatibility with existing translation system.
 */
public class TranslationManager {
    
    private final JavaPlugin plugin;
    
    public TranslationManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Gets a translated message.
     */
    public String getMessage(String key) {
        return I18n.tr(key);
    }
    
    /**
     * Gets a translated message with placeholders.
     */
    public String getMessage(String key, Map<String, Object> placeholders) {
        return I18n.tr(key, placeholders);
    }
    
    /**
     * Reloads translations.
     */
    public void reload() {
        I18n.reload();
    }
}