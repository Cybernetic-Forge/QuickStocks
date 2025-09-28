package com.example.quickstocks.listeners;

import com.example.quickstocks.commands.MarketDeviceCommand;
import com.example.quickstocks.utils.TranslationManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Market Device interactions and restrictions
 */
public class MarketDeviceListener implements Listener {
    
    private final JavaPlugin plugin;
    private final TranslationManager translations;
    private final MarketDeviceCommand deviceCommand;
    private final Map<UUID, Long> cooldowns;
    private final long cooldownTime; // in milliseconds
    
    public MarketDeviceListener(JavaPlugin plugin, TranslationManager translations, MarketDeviceCommand deviceCommand) {
        this.plugin = plugin;
        this.translations = translations;
        this.deviceCommand = deviceCommand;
        this.cooldowns = new HashMap<>();
        this.cooldownTime = 1000; // 1 second cooldown
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player is holding a Market Device
        if (!deviceCommand.isMarketDevice(item)) {
            return;
        }
        
        // Check if right-click action
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }
        
        event.setCancelled(true); // Prevent placing the item
        
        // Check ownership
        UUID deviceOwner = deviceCommand.getDeviceOwner(item);
        if (deviceOwner == null || !deviceOwner.equals(player.getUniqueId())) {
            String ownerName = deviceOwner != null ? getPlayerName(deviceOwner) : "Unknown";
            player.sendMessage(translations.getMessage("market.device.wrong_owner", "owner", ownerName));
            return;
        }
        
        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if (currentTime - lastUse < cooldownTime) {
                player.sendMessage(translations.getMessage("market.device.cooldown"));
                return;
            }
        }
        
        // Update cooldown
        cooldowns.put(playerId, currentTime);
        
        // Open market
        openMarketForPlayer(player);
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        Player player = event.getPlayer();
        
        // Check if it's a Market Device
        if (!deviceCommand.isMarketDevice(item)) {
            return;
        }
        
        // Check if soulbound (bound to different player)
        UUID deviceOwner = deviceCommand.getDeviceOwner(item);
        if (deviceOwner != null && !deviceOwner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(translations.getMessage("market.device.drop_prevented"));
        }
    }
    
    /**
     * Opens the market GUI/command for the player
     * Since the issue mentions the GUI exists from Prompt A, we'll implement 
     * a fallback that calls the stocks command for now
     */
    private void openMarketForPlayer(Player player) {
        player.sendMessage(translations.getMessage("market.device.opened"));
        
        // Try to find and execute a market command
        // For now, we'll use the stocks command as a market interface
        // This can be replaced with actual GUI opening when available
        player.performCommand("stocks");
    }
    
    /**
     * Gets player name from UUID (basic implementation)
     */
    private String getPlayerName(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        // In a real implementation, you might want to cache or look up offline player names
        return uuid.toString().substring(0, 8) + "...";
    }
}