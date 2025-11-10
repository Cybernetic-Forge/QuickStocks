package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
        
        // === Management Section ===
        // View Employees button (slot 19)
        if (slot == 19 && item.getType() == Material.PLAYER_HEAD) {
            player.closeInventory();
            player.performCommand("company employees " + company.getName());
            // TODO: Open employees sub-GUI instead
            return;
        }
        
        // View Jobs button (slot 20)
        if (slot == 20 && item.getType() == Material.WRITABLE_BOOK) {
            player.closeInventory();
            player.performCommand("company jobs " + company.getName());
            // TODO: Open jobs sub-GUI instead
            return;
        }
        
        // Invite Player button (slot 21)
        if (slot == 21 && item.getType() == Material.EMERALD) {
            player.closeInventory();
            Translation.GUI_CompanySettings_InviteHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // === Financial Section ===
        // Deposit button (slot 23)
        if (slot == 23 && item.getType() == Material.HOPPER) {
            player.closeInventory();
            Translation.GUI_CompanySettings_DepositHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Withdraw button (slot 24)
        if (slot == 24 && item.getType() == Material.GOLD_INGOT) {
            player.closeInventory();
            Translation.GUI_CompanySettings_WithdrawHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Transactions button (slot 25)
        if (slot == 25 && item.getType() == Material.BOOK) {
            player.closeInventory();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&eTransaction history feature coming soon!"));
            return;
        }
        
        // === Market Section ===
        // Market Status button (slot 28)
        if (slot == 28 && item.getType() == Material.EMERALD) {
            player.closeInventory();
            if (company.isOnMarket()) {
                player.performCommand("market");
            } else {
                player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                    "&eCompany is not yet public. Use the IPO button to go public!"));
            }
            return;
        }
        
        // Go Public (IPO) button (slot 29)
        if (slot == 29 && item.getType() == Material.DIAMOND) {
            player.closeInventory();
            player.performCommand("company ipo " + company.getName());
            return;
        }
        
        // Manage Shares button (slot 30)
        if (slot == 30 && item.getType() == Material.PAPER) {
            player.closeInventory();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&eShare management feature coming soon!"));
            return;
        }
        
        // === Roles & Titles Section ===
        // Create Job button (slot 32)
        if (slot == 32 && item.getType() == Material.BOOK) {
            player.closeInventory();
            Translation.GUI_CompanySettings_CreateJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Assign Job button (slot 33)
        if (slot == 33 && item.getType() == Material.NAME_TAG) {
            player.closeInventory();
            Translation.GUI_CompanySettings_AssignJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Edit Permissions button (slot 34)
        if (slot == 34 && item.getType() == Material.ENCHANTED_BOOK) {
            player.closeInventory();
            Translation.GUI_CompanySettings_EditJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
        }
    }
}
