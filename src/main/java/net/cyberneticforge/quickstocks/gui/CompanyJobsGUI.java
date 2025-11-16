package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

/**
 * GUI for viewing and managing company job titles
 */
public class CompanyJobsGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Player player;
    @Getter
    private final Company company;
    private final Inventory inventory;

    public CompanyJobsGUI(Player player, Company company) {
        this.player = player;
        this.company = company;

        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_jobs.size", 54);
        String title = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_jobs.title", "&6{company_name} - Jobs")
                .replace("{company_name}", company.getName());
        this.inventory = Bukkit.createInventory(this, guiSize, ChatUT.hexComp(title));
        setupGUI();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void setupGUI() {
        try {
            // Add navigation buttons
            addNavigationButtons();

            // Add job list
            addJobs();

        } catch (Exception e) {
            logger.warning("Error setting up Company Jobs GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cFailed to load jobs list."));
        }
    }

    private void addJobs() {
        try {
            List<CompanyJob> jobs = QuickStocksPlugin.getCompanyService().getCompanyJobs(company.getId());

            int slot = 9; // Start from row 2
            for (CompanyJob job : jobs) {
                if (slot >= 45) break; // Leave bottom row for navigation

                ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(ChatUT.hexComp("&e" + job.getTitle()));

                List<Component> lore = new ArrayList<>();
                lore.add(ChatUT.hexComp("&7Permissions:"));
                lore.add(ChatUT.hexComp((job.canInvite() ? "&a✓" : "&c✗") + " &7Invite Players"));
                lore.add(ChatUT.hexComp((job.canCreateTitles() ? "&a✓" : "&c✗") + " &7Create Titles"));
                lore.add(ChatUT.hexComp((job.canWithdraw() ? "&a✓" : "&c✗" ) + " &7Withdraw Funds"));
                lore.add(ChatUT.hexComp((job.canManageCompany() ? "&a✓" : "&c✗") + " &7Manage Company"));
                lore.add(ChatUT.hexComp((job.canManageChestShop() ? "&a✓" : "&c✗") + " &7Manage ChestShop"));
                lore.add(ChatUT.hexComp((job.canManageSalaries() ? "&a✓" : "&c✗") + " &7Manage Salaries"));
                lore.add(ChatUT.hexComp((job.canManagePlots() ? "&a✓" : "&c✗") + " &7Manage Plots"));
                lore.add(ChatUT.hexComp(""));
                
                // Check if player has permission to edit
                Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), player.getUniqueId().toString());
                if (playerJob.isPresent() && playerJob.get().canManageCompany()) {
                    lore.add(ChatUT.hexComp("&aClick to edit permissions"));
                }

                meta.lore(lore);
                item.setItemMeta(meta);

                inventory.setItem(slot, item);
                slot++;
            }

        } catch (SQLException e) {
            logger.warning("Error loading jobs: " + e.getMessage());
        }
    }

    private void addNavigationButtons() {
        try {
            // Back button
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_jobs.back.slot", 49);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_jobs.back.material", "ARROW"));
            String backName = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_jobs.back.name", "&cBack");

            ItemStack backItem = new ItemStack(backMaterial);
            ItemMeta backMeta = backItem.getItemMeta();
            backMeta.displayName(ChatUT.hexComp(backName));
            backItem.setItemMeta(backMeta);
            inventory.setItem(backSlot, backItem);

        } catch (Exception e) {
            logger.warning("Error adding navigation buttons: " + e.getMessage());
        }
    }

    public void refresh() {
        inventory.clear();
        setupGUI();
    }
}
