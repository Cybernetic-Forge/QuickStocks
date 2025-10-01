package net.cyberneticforge.quickstocks.utils;

import net.cyberneticforge.quickstocks.commands.MarketDeviceCommand;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
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
     * Registers all recipes if enabled in config
     */
    public void registerRecipes() {
        if (plugin.getConfig().getBoolean("marketDevice.recipe.enabled", false)) {
            registerMarketDeviceRecipe();
            plugin.getLogger().info("Market Device crafting recipe enabled");
        }
    }
    
    /**
     * Registers the Market Device crafting recipe
     * Uses mid/late-game materials as specified in requirements
     */
    private void registerMarketDeviceRecipe() {
        // Create a dummy player for the device creation (will be overridden in craft event)
        // The actual device will be created with proper owner in the crafting event listener
        NamespacedKey key = new NamespacedKey(plugin, "market_device_recipe");
        
        // Create the recipe result (will be updated with proper owner when crafted)
        ItemStack result = new ItemStack(Material.RECOVERY_COMPASS);
        result.getItemMeta().setDisplayName(translations.getMessage("market.device.name"));
        
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        
        // Define the recipe pattern - using mid/late game materials
        recipe.shape(
            "GEG",
            "ERE", 
            "GEG"
        );
        
        // Set ingredients
        recipe.setIngredient('G', Material.GOLD_INGOT);      // Gold ingots
        recipe.setIngredient('E', Material.ENDER_PEARL);     // Ender pearls (mid-game)
        recipe.setIngredient('R', Material.RECOVERY_COMPASS); // Recovery compass (late-game)
        
        // Register the recipe
        plugin.getServer().addRecipe(recipe);
        
        plugin.getLogger().info("Registered Market Device shaped recipe: Gold + Ender Pearl + Recovery Compass");
    }
    
    /**
     * Creates a proper Market Device for the given player
     * This should be called from a crafting event listener
     */
    public ItemStack createDeviceForPlayer(Player player) {
        return deviceCommand.createMarketDevice(player);
    }
    
    /**
     * Removes all registered recipes
     */
    public void removeRecipes() {
        NamespacedKey deviceKey = new NamespacedKey(plugin, "market_device_recipe");
        plugin.getServer().removeRecipe(deviceKey);
    }
}