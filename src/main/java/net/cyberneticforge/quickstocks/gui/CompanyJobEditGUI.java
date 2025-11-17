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

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for editing job permissions
 */
public class CompanyJobEditGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Player player;
    @Getter
    private final Company company;
    @Getter
    private final CompanyJob job;
    private final Inventory inventory;

    public CompanyJobEditGUI(Player player, Company company, CompanyJob job) {
        this.player = player;
        this.company = company;
        this.job = job;

        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_job_edit.size", 27);
        String title = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.title", "&6Edit: {job_title}")
                .replace("{job_title}", job.getTitle());
        this.inventory = Bukkit.createInventory(this, guiSize, ChatUT.hexComp(title));
        setupGUI();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void setupGUI() {
        try {
            // Add permission toggles
            addPermissionToggles();

            // Add navigation buttons
            addNavigationButtons();

        } catch (Exception e) {
            logger.warning("Error setting up Company Job Edit GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cFailed to load job edit GUI."));
        }
    }

    private void addPermissionToggles() {
        // Can Invite
        addPermissionToggle(10, "Invite Players", job.canInvite(), Material.EMERALD, Material.REDSTONE);
        
        // Can Create Titles
        addPermissionToggle(11, "Create Titles", job.canCreateTitles(), Material.EMERALD, Material.REDSTONE);
        
        // Can Withdraw
        addPermissionToggle(12, "Withdraw Funds", job.canWithdraw(), Material.EMERALD, Material.REDSTONE);
        
        // Can Manage Company
        addPermissionToggle(13, "Manage Company", job.canManageCompany(), Material.EMERALD, Material.REDSTONE);
        
        // Can Manage ChestShop
        addPermissionToggle(14, "Manage ChestShop", job.canManageChestShop(), Material.EMERALD, Material.REDSTONE);
        
        // Can Manage Salaries
        addPermissionToggle(15, "Manage Salaries", job.canManageSalaries(), Material.EMERALD, Material.REDSTONE);
        
        // Can Manage Plots
        addPermissionToggle(16, "Manage Plots", job.canManagePlots(), Material.EMERALD, Material.REDSTONE);
    }

    private void addPermissionToggle(int slot, String permissionName, boolean enabled, Material enabledMat, Material disabledMat) {
        ItemStack item = new ItemStack(enabled ? enabledMat : disabledMat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUT.hexComp((enabled ? "&a✓ " : "&c✗ ") + permissionName));
        
        List<Component> lore = new ArrayList<>();
        lore.add(ChatUT.hexComp("&7Status: " + (enabled ? "&aEnabled" : "&cDisabled")));
        lore.add(ChatUT.hexComp(""));
        lore.add(ChatUT.hexComp("&eClick to toggle"));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void addNavigationButtons() {
        try {
            // Save button
            int saveSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_job_edit.save.slot", 22);
            Material saveMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.save.material", "LIME_DYE"));
            String saveName = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.save.name", "&aSave Changes");

            ItemStack saveItem = new ItemStack(saveMaterial);
            ItemMeta saveMeta = saveItem.getItemMeta();
            saveMeta.displayName(ChatUT.hexComp(saveName));
            saveItem.setItemMeta(saveMeta);
            inventory.setItem(saveSlot, saveItem);

            // Back button
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_job_edit.back.slot", 18);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.back.material", "ARROW"));
            String backName = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_job_edit.back.name", "&cCancel");

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
