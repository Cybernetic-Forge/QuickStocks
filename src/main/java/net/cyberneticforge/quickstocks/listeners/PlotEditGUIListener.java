package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.gui.PlotEditGUI;
import net.cyberneticforge.quickstocks.gui.PlotPermissionEditGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
            player.sendMessage(ChatUT.hexComp("&cAn error occurred while editing plot permissions."));
        }
    }
    
    /**
     * Handles clicks in the Plot Edit GUI.
     */
    private void handleClick(Player player, PlotEditGUI gui, int slot, ItemStack item) throws Exception {
        // Get configured close button slot
        int closeSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_edit.close", 49);
        Material closeMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_edit.close", Material.BARRIER);
        Material jobMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("plot_edit.job_item", Material.NAME_TAG);
        
        // Close button
        if (slot == closeSlot && item.getType() == closeMaterial) {
            player.closeInventory();
            player.sendMessage(ChatUT.hexComp("&aPlot permissions saved."));
            return;
        }
        
        // Job role items (slots 18-35)
        if (slot >= 18 && slot < 36 && item.getType() == jobMaterial) {
            if(gui.getInvSlots().containsKey(slot)) {
                CompanyJob job = gui.getInvSlots().get(slot);
                if (job != null) {
                    PlotPermissionEditGUI permGui = new PlotPermissionEditGUI(player, gui.getPlot(), job);
                    player.openInventory(permGui.getInventory());
                }
            }
        }
    }
}
