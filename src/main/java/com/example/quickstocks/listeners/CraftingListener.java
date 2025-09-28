package com.example.quickstocks.listeners;

import com.example.quickstocks.utils.RecipeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles crafting events for QuickStocks items
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is a Market Device recipe
        /*if (event.getRecipe().getResult().equals(new NamespacedKey(plugin, "market_device_recipe"))) {
            // Cancel the default crafting
            event.setCancelled(true);
            
            // Create a proper Market Device bound to the crafting player
            ItemStack properDevice = recipeManager.createDeviceForPlayer(player);
            
            // Add the item to the player's inventory
            player.getInventory().addItem(properDevice);
            
            // Remove the crafting ingredients
            event.getInventory().setMatrix(new ItemStack[9]);
            
            // Notify the player
            player.sendMessage("§aYou crafted a Market Link Device!");
        }*/
    }
}