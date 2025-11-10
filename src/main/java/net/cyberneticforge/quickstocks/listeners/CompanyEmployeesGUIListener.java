package net.cyberneticforge.quickstocks.listeners;

import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.gui.CompanyEmployeesGUI;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles interactions with the Company Employees GUI
 */
public class CompanyEmployeesGUIListener implements Listener {
    
    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getInventory().getHolder() instanceof CompanyEmployeesGUI gui)) {
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
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_employees.back.slot", 49);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_employees.back.material", "ARROW"));
            
            if (slot == backSlot && clickedItem.getType() == backMaterial) {
                // Go back to company settings
                CompanySettingsGUI settingsGUI = new CompanySettingsGUI(player, company);
                player.openInventory(settingsGUI.getInventory());
                return;
            }
            
            // Employee head clicked - show job assignment if player has permission
            if (clickedItem.getType() == Material.PLAYER_HEAD) {
                Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), player.getUniqueId().toString());
                if (playerJob.isPresent() && playerJob.get().canManageCompany()) {
                    // Get the clicked employee's UUID from the skull meta
                    // For now, show a message
                    player.closeInventory();
                    player.sendMessage(ChatUT.hexComp("&eJob assignment feature coming soon! Use: &f/company assignjob <company> <player> <job>"));
                } else {
                    player.sendMessage(ChatUT.hexComp("&cYou don't have permission to assign jobs."));
                }
                return;
            }
            
        } catch (Exception e) {
            logger.warning("Error handling Company Employees GUI click for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cAn error occurred."));
        }
    }
}
