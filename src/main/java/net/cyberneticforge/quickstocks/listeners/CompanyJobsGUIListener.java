package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.gui.CompanyJobEditGUI;
import net.cyberneticforge.quickstocks.gui.CompanyJobsGUI;
import net.cyberneticforge.quickstocks.gui.CompanySettingsGUI;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Handles interactions with the Company Jobs GUI
 */
public class CompanyJobsGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof CompanyJobsGUI gui)) {
            return;
        }
        
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        Company company = gui.getCompany();
        
        try {
            // Back button
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_jobs.back.slot", 49);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_jobs.back.material", "ARROW"));
            
            if (slot == backSlot && clickedItem.getType() == backMaterial) {
                // Go back to company settings
                CompanySettingsGUI settingsGUI = new CompanySettingsGUI(player, company);
                player.openInventory(settingsGUI.getInventory());
                return;
            }
            
            // Job clicked - open edit GUI if player has permission
            if (clickedItem.getType() == Material.WRITABLE_BOOK) {
                Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), player.getUniqueId().toString());
                if (playerJob.isPresent() && playerJob.get().canManageCompany()) {
                    // Find the job by checking all jobs and matching slot
                    List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(company.getId());
                    int jobIndex = slot - 9; // Jobs start at slot 9
                    if (jobIndex >= 0 && jobIndex < jobs.size()) {
                        CompanyJob job = jobs.get(jobIndex);
                        CompanyJobEditGUI editGUI = new CompanyJobEditGUI(player, company, job);
                        player.openInventory(editGUI.getInventory());
                    }
                } else {
                    player.sendMessage(ChatUT.hexComp("&cYou don't have permission to edit job permissions."));
                }
                return;
            }
            
        } catch (Exception e) {
            logger.warning("Error handling Company Jobs GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cAn error occurred."));
        }
    }
}
