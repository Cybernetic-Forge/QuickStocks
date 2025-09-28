package com.example.quickstocks.utils;

import com.example.quickstocks.commands.MarketDeviceCommand;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages crafting recipes for QuickStocks items
 */
public class RecipeManager {
    
    private final JavaPlugin plugin;
    private final MarketDeviceCommand deviceCommand;
    private final TranslationManager translations;
    
    public RecipeManager(JavaPlugin plugin, MarketDeviceCommand deviceCommand, TranslationManager translations) {
        this.plugin = plugin;
        this.deviceCommand = deviceCommand;
        this.translations = translations;
    }
    
    /**
     * Registers custom recipes.
     */
    public void registerRecipes() {
        try {
            // Create Market Device recipe
            NamespacedKey deviceKey = new NamespacedKey(plugin, "market_device_recipe");
            ItemStack deviceItem = createMarketDevice();
            
            ShapedRecipe deviceRecipe = new ShapedRecipe(deviceKey, deviceItem);
            deviceRecipe.shape("IGI", "GCG", "IGI");
            deviceRecipe.setIngredient('I', Material.IRON_INGOT);
            deviceRecipe.setIngredient('G', Material.GOLD_INGOT);
            deviceRecipe.setIngredient('C', Material.COMPASS);
            
            plugin.getServer().addRecipe(deviceRecipe);
            plugin.getLogger().info("Registered Market Device recipe");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register recipes: " + e.getMessage());
        }
    }
    
    /**
     * Removes all registered recipes
     */
    public void removeRecipes() {
        NamespacedKey deviceKey = new NamespacedKey(plugin, "market_device_recipe");
        plugin.getServer().removeRecipe(deviceKey);
        plugin.getLogger().info("Removed custom recipes");
    }
    
    private ItemStack createMarketDevice() {
        ItemStack item = new ItemStack(Material.COMPASS);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bMarket Link Device");
            meta.setLore(java.util.Arrays.asList(
                "§7Right-click to access the market",
                "§7from anywhere in the world!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}