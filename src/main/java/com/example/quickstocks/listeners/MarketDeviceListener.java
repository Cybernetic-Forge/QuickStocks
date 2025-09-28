package com.example.quickstocks.listeners;

import com.example.quickstocks.I18n;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles Market Link Device interactions.
 */
public class MarketDeviceListener implements Listener {
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        String displayName = meta.getDisplayName();
        if ("§bMarket Link Device".equals(displayName)) {
            // This is a Market Link Device
            event.setCancelled(true);
            
            // TODO: Open market GUI
            event.getPlayer().sendMessage(I18n.component("market.open_title_items"));
        }
    }
}