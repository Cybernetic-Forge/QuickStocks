package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.commands.MarketDeviceCommand;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Market Device interactions and restrictions
 */
public class MarketDeviceListener implements Listener {

    private final Map<UUID, Long> cooldowns;
    private final long cooldownTime; // in milliseconds
    
    public MarketDeviceListener() {
        this.cooldowns = new HashMap<>();
        this.cooldownTime = 1000; // 1 second cooldown
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player is holding a Market Device
        if (!MarketDeviceCommand.isMarketDevice(item)) {
            return;
        }
        
        // Check if right-click action
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }
        
        event.setCancelled(true); // Prevent placing the item
        
        // Check ownership
        UUID deviceOwner = MarketDeviceCommand.getDeviceOwner(item);
        if (deviceOwner == null || !deviceOwner.equals(player.getUniqueId())) {
            String ownerName = deviceOwner != null ? getPlayerName(deviceOwner) : "Unknown";
            Translation.Market_Device_WrongOwner.sendMessage(player, new Replaceable("owner", ownerName));
            return;
        }
        
        // Check cooldown
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (cooldowns.containsKey(playerId)) {
            long lastUse = cooldowns.get(playerId);
            if (currentTime - lastUse < cooldownTime) {
                Translation.Market_Device_Cooldown.sendMessage(player);
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
        if (!MarketDeviceCommand.isMarketDevice(item)) {
            return;
        }
        
        // Check if soulbound (bound to different player)
        UUID deviceOwner = MarketDeviceCommand.getDeviceOwner(item);
        if (deviceOwner != null && !deviceOwner.equals(player.getUniqueId())) {
            event.setCancelled(true);
            Translation.Market_Device_DropPrevented.sendMessage(player);
        }
    }
    
    /**
     * Opens the market GUI/command for the player
     * Now properly opens the /market command interface
     */
    private void openMarketForPlayer(Player player) {
        Translation.Market_Device_Opened.sendMessage(player);
        // Execute the market command to open the market interface
        player.performCommand("market");
    }
    
    /**
     * Gets player name from UUID (basic implementation)
     */
    private String getPlayerName(UUID uuid) {
        Player player = QuickStocksPlugin.getInstance().getServer().getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        // In a real implementation, you might want to cache or look up offline player names
        return uuid.toString().substring(0, 8) + "...";
    }
}