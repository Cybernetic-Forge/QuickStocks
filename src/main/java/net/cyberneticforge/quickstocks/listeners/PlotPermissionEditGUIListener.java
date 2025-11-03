package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.PlotPermission;
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

import java.util.Optional;

/**
 * Handles interactions with the Plot Permission Edit GUI.
 */
public class PlotPermissionEditGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // Check if this is a PlotPermissionEditGUI
        if (!(event.getInventory().getHolder() instanceof PlotPermissionEditGUI gui)) {
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
            logger.warning("Error handling Plot Permission Edit GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cAn error occurred while editing permissions."));
        }
    }
    
    /**
     * Handles clicks in the Plot Permission Edit GUI.
     */
    private void handleClick(Player player, PlotPermissionEditGUI gui, int slot, ItemStack item) throws Exception {
        // Get configured slots
        int buildSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_permission_edit.build_permission", 10);
        int interactSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_permission_edit.interact_permission", 13);
        int containerSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_permission_edit.container_permission", 16);
        int backSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("plot_permission_edit.back", 22);
        
        // Get current permissions
        Optional<PlotPermission> permOpt = QuickStocksPlugin.getCompanyPlotService()
            .getPlotPermission(gui.getPlot().getId(), gui.getJob().getId());
        
        boolean canBuild = permOpt.map(PlotPermission::canBuild).orElse(true);
        boolean canInteract = permOpt.map(PlotPermission::canInteract).orElse(true);
        boolean canContainer = permOpt.map(PlotPermission::canContainer).orElse(true);
        
        boolean changed = false;
        
        // Build permission
        if (slot == buildSlot) {
            canBuild = !canBuild;
            changed = true;
        }
        // Interact permission
        else if (slot == interactSlot) {
            canInteract = !canInteract;
            changed = true;
        }
        // Container permission
        else if (slot == containerSlot) {
            canContainer = !canContainer;
            changed = true;
        }
        // Back button
        else if (slot == backSlot && item.getType() == Material.ARROW) {
            // Return to plot edit GUI
            PlotEditGUI plotEditGUI = new PlotEditGUI(player, gui.getPlot());
            player.openInventory(plotEditGUI.getInventory());
            return;
        }
        
        // Save changes if any permission was toggled
        if (changed) {
            QuickStocksPlugin.getCompanyPlotService().setPlotPermission(
                gui.getPlot().getId(),
                gui.getJob().getId(),
                canBuild,
                canInteract,
                canContainer
            );
            
            // Refresh GUI
            player.closeInventory();
            PlotPermissionEditGUI newGui = new PlotPermissionEditGUI(player, gui.getPlot(), gui.getJob());
            player.openInventory(newGui.getInventory());
            
            player.sendMessage(ChatUT.hexComp("&aPermission updated for " + gui.getJob().getTitle()));
        }
    }
}
