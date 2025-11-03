package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.gui.PlotEditGUI;
import net.cyberneticforge.quickstocks.gui.PlotPermissionEditGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles interactions with the Plot Edit GUI.
 */
public class PlotEditGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Check if this is a PlotEditGUI
        if (!(event.getInventory().getHolder() instanceof PlotEditGUI gui)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement
        
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        try {
            handleClick(player, gui, slot, clickedItem);
        } catch (Exception e) {
            logger.warning("Error handling Plot Edit GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cAn error occurred while editing plot permissions.");
        }
    }
    
    /**
     * Handles clicks in the Plot Edit GUI.
     */
    private void handleClick(Player player, PlotEditGUI gui, int slot, ItemStack item) throws Exception {
        // Close button (slot 49)
        if (slot == 49 && item.getType() == Material.BARRIER) {
            player.closeInventory();
            player.sendMessage("§aPlot permissions saved.");
            return;
        }
        
        // Job role items (slots 18-35)
        if (slot >= 18 && slot < 36 && item.getType() == Material.NAME_TAG) {
            // Extract job title from item display name
            String jobTitle = item.getItemMeta().displayName().toString();
            // Remove color codes
            jobTitle = jobTitle.replaceAll("§[0-9a-fk-or]", "").trim();
            
            // Get job ID from title
            List<net.cyberneticforge.quickstocks.core.model.CompanyJob> jobs = 
                QuickStocksPlugin.getCompanyService().getCompanyJobs(gui.getPlot().getCompanyId());
            
            for (net.cyberneticforge.quickstocks.core.model.CompanyJob job : jobs) {
                if (job.getTitle().equals(jobTitle)) {
                    // Open permission edit GUI for this job
                    PlotPermissionEditGUI permGui = new PlotPermissionEditGUI(player, gui.getPlot(), job);
                    player.openInventory(permGui.getInventory());
                    return;
                }
            }
        }
    }
}
