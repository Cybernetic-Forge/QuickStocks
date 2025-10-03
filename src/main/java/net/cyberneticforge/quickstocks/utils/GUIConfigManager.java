package net.cyberneticforge.quickstocks.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages GUI configurations loaded from guis.yml
 * Provides methods to retrieve GUI elements with Adventure API support
 */
public class GUIConfigManager {
    
    private final JavaPlugin plugin;
    private YamlConfiguration guisConfig;
    
    public GUIConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadGUIConfig();
    }
    
    /**
     * Loads GUI configurations from guis.yml file
     */
    private void loadGUIConfig() {
        File guisFile = new File(plugin.getDataFolder(), "guis.yml");
        
        // Create default file if it doesn't exist
        if (!guisFile.exists()) {
            try {
                plugin.saveResource("guis.yml", false);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not save default guis.yml", e);
            }
        }
        
        // Load the configuration
        try {
            guisConfig = YamlConfiguration.loadConfiguration(guisFile);
            
            // Load defaults from JAR if available
            InputStream defaultStream = plugin.getResource("guis.yml");
            if (defaultStream != null) {
                YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
                guisConfig.setDefaults(defaultConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load guis.yml", e);
            guisConfig = new YamlConfiguration(); // Use empty config as fallback
        }
    }
    
    /**
     * Gets the GUI title as an Adventure Component
     * @param path The path to the GUI title in the YAML file
     * @return The GUI title as a Component
     */
    public Component getTitle(String path) {
        String title = guisConfig.getString(path + ".title", "GUI");
        return ChatUT.hexComp(title);
    }
    
    /**
     * Gets the GUI title as a legacy string (for backward compatibility)
     * @param path The path to the GUI title in the YAML file
     * @return The GUI title as a String
     */
    public String getTitleString(String path) {
        String title = guisConfig.getString(path + ".title", "GUI");
        return ChatUT.serialize(ChatUT.hexComp(title));
    }
    
    /**
     * Gets an item's display name as an Adventure Component
     * @param path The path to the item in the YAML file
     * @return The display name as a Component
     */
    public Component getItemName(String path) {
        String name = guisConfig.getString(path + ".name", "Item");
        return ChatUT.hexComp(name);
    }
    
    /**
     * Gets an item's display name as a legacy string
     * @param path The path to the item in the YAML file
     * @return The display name as a String
     */
    public String getItemNameString(String path) {
        String name = guisConfig.getString(path + ".name", "Item");
        return ChatUT.serialize(ChatUT.hexComp(name));
    }
    
    /**
     * Gets an item's lore as Adventure Components
     * @param path The path to the item in the YAML file
     * @return List of lore lines as Components
     */
    public List<Component> getItemLore(String path) {
        List<String> loreStrings = guisConfig.getStringList(path + ".lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreStrings) {
            lore.add(ChatUT.hexComp(line));
        }
        return lore;
    }
    
    /**
     * Gets an item's lore as legacy strings
     * @param path The path to the item in the YAML file
     * @return List of lore lines as Strings
     */
    public List<String> getItemLoreStrings(String path) {
        List<String> loreStrings = guisConfig.getStringList(path + ".lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreStrings) {
            lore.add(ChatUT.serialize(ChatUT.hexComp(line)));
        }
        return lore;
    }
    
    /**
     * Gets an item's material
     * @param path The path to the item in the YAML file
     * @param defaultMaterial The default material if not found
     * @return The Material
     */
    public Material getItemMaterial(String path, Material defaultMaterial) {
        String materialName = guisConfig.getString(path + ".material", defaultMaterial.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in guis.yml at " + path + ", using default");
            return defaultMaterial;
        }
    }
    
    /**
     * Gets the slot number for an item
     * @param path The path to the item in the YAML file
     * @param defaultSlot The default slot if not found
     * @return The slot number
     */
    public int getItemSlot(String path, int defaultSlot) {
        return guisConfig.getInt(path + ".slot", defaultSlot);
    }
    
    /**
     * Gets a string value from the config
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The string value
     */
    public String getString(String path, String defaultValue) {
        return guisConfig.getString(path, defaultValue);
    }
    
    /**
     * Gets a string value from the config as an Adventure Component
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The value as a Component
     */
    public Component getComponent(String path, String defaultValue) {
        String value = guisConfig.getString(path, defaultValue);
        return ChatUT.hexComp(value);
    }
    
    /**
     * Gets an integer value from the config
     * @param path The path to the value
     * @param defaultValue The default value if not found
     * @return The integer value
     */
    public int getInt(String path, int defaultValue) {
        return guisConfig.getInt(path, defaultValue);
    }
    
    /**
     * Gets a configuration section
     * @param path The path to the section
     * @return The ConfigurationSection or null if not found
     */
    public ConfigurationSection getSection(String path) {
        return guisConfig.getConfigurationSection(path);
    }
    
    /**
     * Reloads the GUI configuration from file
     */
    public void reload() {
        loadGUIConfig();
    }
}
