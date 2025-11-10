package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import net.cyberneticforge.quickstocks.infrastructure.config.GuisCfg;
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
        GuisCfg cfg = QuickStocksPlugin.getGuisCfg();
        
        // Get configured slots and materials from guis.yml
        int refreshSlot = cfg.getInt("company_settings.refresh.slot", 49);
        Material refreshMaterial = Material.valueOf(cfg.getString("company_settings.refresh.material", "CLOCK"));
        
        int closeSlot = cfg.getInt("company_settings.close.slot", 53);
        Material closeMaterial = Material.valueOf(cfg.getString("company_settings.close.material", "BARRIER"));
        
        // Management section
        int viewEmployeesSlot = cfg.getInt("company_settings.view_employees.slot", 11);
        Material viewEmployeesMaterial = Material.valueOf(cfg.getString("company_settings.view_employees.material", "PLAYER_HEAD"));
        
        int viewJobsSlot = cfg.getInt("company_settings.view_jobs.slot", 13);
        Material viewJobsMaterial = Material.valueOf(cfg.getString("company_settings.view_jobs.material", "WRITABLE_BOOK"));
        
        int invitePlayerSlot = cfg.getInt("company_settings.invite_player.slot", 12);
        Material invitePlayerMaterial = Material.valueOf(cfg.getString("company_settings.invite_player.material", "EMERALD"));
        
        // Financial section
        int depositSlot = cfg.getInt("company_settings.deposit.slot", 29);
        Material depositMaterial = Material.valueOf(cfg.getString("company_settings.deposit.material", "HOPPER"));
        
        int withdrawSlot = cfg.getInt("company_settings.withdraw.slot", 30);
        Material withdrawMaterial = Material.valueOf(cfg.getString("company_settings.withdraw.material", "GOLD_INGOT"));
        
        int transactionsSlot = cfg.getInt("company_settings.transactions.slot", 31);
        Material transactionsMaterial = Material.valueOf(cfg.getString("company_settings.transactions.material", "BOOK"));
        
        // Market section
        int marketStatusSlot = cfg.getInt("company_settings.market_status.slot", 39);
        Material marketStatusMaterial = Material.valueOf(cfg.getString("company_settings.market_status.material", "EMERALD"));
        
        int goPublicSlot = cfg.getInt("company_settings.go_public.slot", 38);
        Material goPublicMaterial = Material.valueOf(cfg.getString("company_settings.go_public.material", "DIAMOND"));
        
        int manageSharesSlot = cfg.getInt("company_settings.manage_shares.slot", 40);
        Material manageSharesMaterial = Material.valueOf(cfg.getString("company_settings.manage_shares.material", "PAPER"));
        
        // Roles section
        int createJobSlot = cfg.getInt("company_settings.create_job.slot", 14);
        Material createJobMaterial = Material.valueOf(cfg.getString("company_settings.create_job.material", "BOOK"));
        
        int assignJobSlot = cfg.getInt("company_settings.assign_job.slot", 15);
        Material assignJobMaterial = Material.valueOf(cfg.getString("company_settings.assign_job.material", "NAME_TAG"));
        
        int editPermissionsSlot = cfg.getInt("company_settings.edit_permissions.slot", 16);
        Material editPermissionsMaterial = Material.valueOf(cfg.getString("company_settings.edit_permissions.material", "ENCHANTED_BOOK"));
        
        // Refresh button
        if (slot == refreshSlot && item.getType() == refreshMaterial) {
            gui.refresh();
            Translation.GUI_CompanySettings_Refresh_Success.sendMessage(player);
            return;
        }
        
        // Close button
        if (slot == closeSlot && item.getType() == closeMaterial) {
            player.closeInventory();
            return;
        }
        
        // === Management Section ===
        // View Employees button
        if (slot == viewEmployeesSlot && item.getType() == viewEmployeesMaterial) {
            player.closeInventory();
            player.performCommand("company employees " + company.getName());
            // TODO: Open employees sub-GUI instead
            return;
        }
        
        // View Jobs button
        if (slot == viewJobsSlot && item.getType() == viewJobsMaterial) {
            player.closeInventory();
            player.performCommand("company jobs " + company.getName());
            // TODO: Open jobs sub-GUI instead
            return;
        }
        
        // Invite Player button
        if (slot == invitePlayerSlot && item.getType() == invitePlayerMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_InviteHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // === Financial Section ===
        // Deposit button
        if (slot == depositSlot && item.getType() == depositMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_DepositHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Withdraw button
        if (slot == withdrawSlot && item.getType() == withdrawMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_WithdrawHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Transactions button
        if (slot == transactionsSlot && item.getType() == transactionsMaterial) {
            player.closeInventory();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&eTransaction history feature coming soon!"));
            return;
        }
        
        // === Market Section ===
        // Market Status button
        if (slot == marketStatusSlot && item.getType() == marketStatusMaterial) {
            player.closeInventory();
            if (company.isOnMarket()) {
                player.performCommand("market");
            } else {
                player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                    "&eCompany is not yet public. Use the IPO button to go public!"));
            }
            return;
        }
        
        // Go Public (IPO) button
        if (slot == goPublicSlot && item.getType() == goPublicMaterial) {
            player.closeInventory();
            player.performCommand("company ipo " + company.getName());
            return;
        }
        
        // Manage Shares button
        if (slot == manageSharesSlot && item.getType() == manageSharesMaterial) {
            player.closeInventory();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&eShare management feature coming soon!"));
            return;
        }
        
        // === Roles & Titles Section ===
        // Create Job button
        if (slot == createJobSlot && item.getType() == createJobMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_CreateJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Assign Job button
        if (slot == assignJobSlot && item.getType() == assignJobMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_AssignJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Edit Permissions button
        if (slot == editPermissionsSlot && item.getType() == editPermissionsMaterial) {
            player.closeInventory();
            Translation.GUI_CompanySettings_EditJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
        }
    }
}
