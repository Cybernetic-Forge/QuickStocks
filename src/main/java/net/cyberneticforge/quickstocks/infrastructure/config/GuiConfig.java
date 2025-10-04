package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages GUI configurations loaded from guis.yml
 * Provides methods to retrieve GUI elements with Adventure API support
 */
public class GuiConfig {

    @Getter
    private YamlParser config;
    
    public GuiConfig() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "guis.yml");
    }
    
    /**
     * Gets the GUI title as an Adventure Component
     * @param path The path to the GUI title in the YAML file
     * @return The GUI title as a Component
     */
    public Component getTitle(String path) {
        String title = config.getString(path + ".title", "GUI");
        return ChatUT.hexComp(title);
    }
    
    /**
     * Gets an item's display name as an Adventure Component
     * @param path The path to the item in the YAML file
     * @return The display name as a Component
     */
    public Component getItemName(String path) {
        String name = config.getString(path + ".name", "Item");
        return ChatUT.hexComp(name);
    }
    
    /**
     * Gets an item's lore as Adventure Components
     * @param path The path to the item in the YAML file
     * @return List of lore lines as Components
     */
    public List<Component> getItemLore(String path, Replaceable... replaceables) {
        List<String> loreStrings = config.getStringList(path + ".lore");
        List<Component> lore = new ArrayList<>();
        for (String line : loreStrings) {
            for (Replaceable r : replaceables) {
                line = line.replace(r.getKey(), r.getValue());
            }
            lore.add(ChatUT.hexComp(line));
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
        String materialName = config.getString(path + ".material", defaultMaterial.name());
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            QuickStocksPlugin.getInstance().getLogger().warning("Invalid material '" + materialName + "' in guis.yml at " + path + ", using default");
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
        return config.getInt(path + ".slot", defaultSlot);
    }
}
