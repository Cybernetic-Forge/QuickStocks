package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;

/**
 * Handles interactions with the Company Settings GUI
 */
public class CompanySettingsGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if this is a CompanySettingsGUI
        if (!(event.getInventory().getHolder() instanceof CompanySettingsGUI gui)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item pickup/movement

        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        Company company = gui.getCompany();
        
        try {
            handleClick(player, gui, slot, clickedItem, company);
        } catch (Exception e) {
            logger.warning("Error handling Company Settings GUI click for " + player.getName() + ": " + e.getMessage());
            Translation.GUI_CompanySettings_Error.sendMessage(player);
        }
    }
    
    /**
     * Handles clicks in the Company Settings GUI
     */
    private void handleClick(Player player, CompanySettingsGUI gui, int slot, ItemStack item, Company company) {
        // Refresh button (slot 49)
        if (slot == 49 && item.getType() == Material.CLOCK) {
            gui.refresh();
            Translation.GUI_CompanySettings_Refresh_Success.sendMessage(player);
            return;
        }
        
        // Close button (slot 53)
        if (slot == 53 && item.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        // View Employees button (slot 19)
        if (slot == 19 && item.getType() == Material.PLAYER_HEAD) {
            player.closeInventory();
            player.performCommand("company employees " + company.getName());
            return;
        }
        
        // View Jobs button (slot 20)
        if (slot == 20 && item.getType() == Material.WRITABLE_BOOK) {
            player.closeInventory();
            player.performCommand("company jobs " + company.getName());
            return;
        }
        
        // Deposit button (slot 21)
        if (slot == 21 && item.getType() == Material.HOPPER) {
            player.closeInventory();
            Translation.GUI_CompanySettings_DepositHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Withdraw button (slot 22)
        if (slot == 22 && item.getType() == Material.DISPENSER) {
            player.closeInventory();
            Translation.GUI_CompanySettings_WithdrawHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Assign Job button (slot 23)
        if (slot == 23 && item.getType() == Material.ENCHANTED_BOOK) {
            player.closeInventory();
            Translation.GUI_CompanySettings_AssignJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Invite Player button (slot 24)
        if (slot == 24 && item.getType() == Material.PAPER) {
            player.closeInventory();
            Translation.GUI_CompanySettings_InviteHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Create Job button (slot 25)
        if (slot == 25 && item.getType() == Material.BOOK) {
            player.closeInventory();
            Translation.GUI_CompanySettings_CreateJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Edit Job button (slot 26)
        if (slot == 26 && item.getType() == Material.WRITABLE_BOOK) {
            player.closeInventory();
            Translation.GUI_CompanySettings_EditJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
        }
    }
}
