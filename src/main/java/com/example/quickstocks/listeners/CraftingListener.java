package com.example.quickstocks.listeners;

import com.example.quickstocks.utils.RecipeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles crafting events for custom recipes.
 */
public class CraftingListener implements Listener {
    
    private final JavaPlugin plugin;
    private final RecipeManager recipeManager;
    
    public CraftingListener(JavaPlugin plugin, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
    }
    
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        // TODO: Handle custom crafting logic
        NamespacedKey recipeKey = event.getRecipe().getKey();
        
        if (recipeKey.getNamespace().equals(plugin.getName().toLowerCase())) {
            // This is one of our custom recipes
            plugin.getLogger().info("Custom recipe crafted: " + recipeKey.getKey());
        }
    }
}