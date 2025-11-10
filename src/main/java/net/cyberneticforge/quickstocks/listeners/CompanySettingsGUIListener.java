package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.enums.Translation;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.gui.CompanyEmployeesGUI;
import net.cyberneticforge.quickstocks.gui.CompanyJobsGUI;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import net.cyberneticforge.quickstocks.infrastructure.config.GuiConfig;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
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
         GuiConfig cfg = QuickStocksPlugin.getGuiConfig();
        
        // Get configured slots and materials from guis.yml
        int refreshSlot = cfg.getItemSlot("company_settings.refresh.slot", 49);
        int closeSlot = cfg.getItemSlot("company_settings.close.slot", 53);

        // Management section
        int viewEmployeesSlot = cfg.getItemSlot("company_settings.view_employees.slot", 11);
        int viewJobsSlot = cfg.getItemSlot("company_settings.view_jobs.slot", 13);
        int invitePlayerSlot = cfg.getItemSlot("company_settings.invite_player.slot", 12);

        // Financial section
        int depositSlot = cfg.getItemSlot("company_settings.deposit.slot", 29);
        int withdrawSlot = cfg.getItemSlot("company_settings.withdraw.slot", 30);
        int transactionsSlot = cfg.getItemSlot("company_settings.transactions.slot", 31);
        int marketStatusSlot = cfg.getItemSlot("company_settings.market_status.slot", 39);
        int goPublicSlot = cfg.getItemSlot("company_settings.go_public.slot", 38);
        int manageSharesSlot = cfg.getItemSlot("company_settings.manage_shares.slot", 40);

        // Roles section
        int createJobSlot = cfg.getItemSlot("company_settings.create_job.slot", 14);
        int assignJobSlot = cfg.getItemSlot("company_settings.assign_job.slot", 15);
        int editPermissionsSlot = cfg.getItemSlot("company_settings.edit_permissions.slot", 16);

        // Refresh button
        if (slot == refreshSlot) {
            gui.refresh();
            Translation.GUI_CompanySettings_Refresh_Success.sendMessage(player);
            return;
        }
        
        // Close button
        if (slot == closeSlot) {
            player.closeInventory();
            return;
        }
        
        // === Management Section ===
        // View Employees button
        if (slot == viewEmployeesSlot) {
            player.closeInventory();
            player.performCommand("company employees " + company.getName());
            player.openInventory(new CompanyEmployeesGUI(player, company).getInventory());
            return;
        }
        
        // View Jobs button
        if (slot == viewJobsSlot) {
            player.closeInventory();
            player.performCommand("company jobs " + company.getName());
            player.openInventory(new CompanyJobsGUI(player, company).getInventory());
            return;
        }
        
        // Invite Player button
        if (slot == invitePlayerSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_InviteHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // === Financial Section ===
        // Deposit button
        if (slot == depositSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_DepositHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Withdraw button
        if (slot == withdrawSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_WithdrawHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Transactions button
        if (slot == transactionsSlot) {
            player.closeInventory();
            player.sendMessage(net.cyberneticforge.quickstocks.utils.ChatUT.hexComp(
                "&eTransaction history feature coming soon!"));
            return;
        }
        
        // === Market Section ===
        // Market Status button
        if (slot == marketStatusSlot) {
            player.closeInventory();
            if (company.isOnMarket()) {
                player.performCommand("market");
            } else {
                player.sendMessage(ChatUT.hexComp(
                    "&eCompany is not yet public. Use the IPO button to go public!"));
            }
            return;
        }
        
        // Go Public (IPO) button
        if (slot == goPublicSlot) {
            player.closeInventory();
            player.performCommand("company ipo " + company.getName());
            return;
        }
        
        // Manage Shares button
        if (slot == manageSharesSlot) {
            player.closeInventory();
            player.sendMessage(ChatUT.hexComp(
                "&eShare management feature coming soon!"));
            return;
        }
        
        // === Roles & Titles Section ===
        // Create Job button
        if (slot == createJobSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_CreateJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Assign Job button
        if (slot == assignJobSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_AssignJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
            return;
        }
        
        // Edit Permissions button
        if (slot == editPermissionsSlot) {
            player.closeInventory();
            Translation.GUI_CompanySettings_EditJobHint.sendMessage(player,
                new Replaceable("%company%", company.getName()));
        }
    }
}
