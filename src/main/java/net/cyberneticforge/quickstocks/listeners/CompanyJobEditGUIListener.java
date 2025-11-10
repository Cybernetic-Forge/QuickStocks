package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.gui.CompanyJobEditGUI;
import net.cyberneticforge.quickstocks.gui.CompanyJobsGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

/**
 * Handles interactions with the Company Job Edit GUI
 */
public class CompanyJobEditGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof CompanyJobEditGUI gui)) {
            return;
        }
        
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        Company company = gui.getCompany();
        CompanyJob job = gui.getJob();
        
        try {
            // Save button
            int saveSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_job_edit.save.slot", 22);
            Material saveMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.save.material", "LIME_DYE"));
            
            if (slot == saveSlot && clickedItem.getType() == saveMaterial) {
                // Update job permissions in database
                try {
                    QuickStocksPlugin.getDatabaseManager().getDb().execute(
                        "UPDATE company_jobs SET can_invite = ?, can_create_titles = ?, can_withdraw = ?, can_manage_company = ?, can_manage_chestshop = ?, can_manage_salaries = ?, can_manage_plots = ? WHERE id = ?",
                        job.canInvite() ? 1 : 0,
                        job.canCreateTitles() ? 1 : 0,
                        job.canWithdraw() ? 1 : 0,
                        job.canManageCompany() ? 1 : 0,
                        job.canManageChestShop() ? 1 : 0,
                        job.canManageSalaries() ? 1 : 0,
                        job.canManagePlots() ? 1 : 0,
                        job.getId()
                    );
                    player.sendMessage(ChatUT.hexComp("&aJob permissions updated successfully!"));
                    
                    // Go back to jobs list
                    CompanyJobsGUI jobsGUI = new CompanyJobsGUI(player, company);
                    player.openInventory(jobsGUI.getInventory());
                } catch (SQLException e) {
                    logger.warning("Error saving job permissions: " + e.getMessage());
                    player.sendMessage(ChatUT.hexComp("&cFailed to save permissions."));
                }
                return;
            }
            
            // Back button
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_job_edit.back.slot", 18);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.back.material", "ARROW"));
            
            if (slot == backSlot && clickedItem.getType() == backMaterial) {
                // Go back to jobs list without saving
                CompanyJobsGUI jobsGUI = new CompanyJobsGUI(player, company);
                player.openInventory(jobsGUI.getInventory());
                return;
            }
            
            // Permission toggles
            if (slot >= 10 && slot <= 16) {
                togglePermission(gui, slot);
                gui.refresh();
                return;
            }
            
        } catch (Exception e) {
            logger.warning("Error handling Company Job Edit GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cAn error occurred."));
        }
    }
    
    private void togglePermission(CompanyJobEditGUI gui, int slot) {
        CompanyJob job = gui.getJob();
        
        switch (slot) {
            case 10: // Can Invite
                job.setCanInvite(!job.canInvite());
                break;
            case 11: // Can Create Titles
                job.setCanCreateTitles(!job.canCreateTitles());
                break;
            case 12: // Can Withdraw
                job.setCanWithdraw(!job.canWithdraw());
                break;
            case 13: // Can Manage Company
                job.setCanManageCompany(!job.canManageCompany());
                break;
            case 14: // Can Manage ChestShop
                job.setCanManageChestShop(!job.canManageChestShop());
                break;
            case 15: // Can Manage Salaries
                job.setCanManageSalaries(!job.canManageSalaries());
                break;
            case 16: // Can Manage Plots
                job.setCanManagePlots(!job.canManagePlots());
                break;
        }
    }
}
