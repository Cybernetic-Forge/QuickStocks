package net.cyberneticforge.quickstocks.gui;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.model.Replaceable;
import net.cyberneticforge.quickstocks.infrastructure.logging.PluginLogger;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Company Settings GUI for managing company settings and viewing information
 */
public class CompanySettingsGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Player player;

    /**
     * -- GETTER --
     * Gets the company associated with this GUI
     */
    @Getter
    private Company company;  // Not final so it can be refreshed
    private final Inventory inventory;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CompanySettingsGUI(Player player, Company company) {
        this.player = player;
        this.company = company;

        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_settings.size", 54);
        String title = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.title", "&6Company: &f{company_name}")
                .replace("{company_name}", company.getName());
        this.inventory = Bukkit.createInventory(this, guiSize, ChatUT.hexComp(title));
        setupGUI();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets up the GUI with company information and action buttons
     */
    private void setupGUI() {
        try {
            // Add company info (top section)
            addCompanyInfo();

            // Add player's job info
            addPlayerJobInfo();

            // Add action buttons
            addActionButtons();

            // Add navigation buttons
            addNavigationButtons();

        } catch (Exception e) {
            logger.warning("Error setting up Company Settings GUI for " + player.getName() + ": " + e.getMessage());
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.error_message", "&cFailed to load company settings.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }

    /**
     * Adds company information display
     */
    private void addCompanyInfo() {
        try {
            // Company info item
            Material companyMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("company_settings.company_info", Material.GOLDEN_HELMET);
            int companySlot = QuickStocksPlugin.getGuiConfig().getItemSlot("company_settings.company_info", 4);
            ItemStack companyItem = new ItemStack(companyMaterial);
            ItemMeta meta = companyItem.getItemMeta();

            String companyName = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.company_info.name", "&6{company_name}")
                    .replace("{company_name}", company.getName());
            meta.displayName(ChatUT.hexComp(companyName));

            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(company.getOwnerUuid()));
            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("company_settings.company_info", new Replaceable("{company_name}", company.getName())
                    , new Replaceable("{company_type}", company.getType())
                    , new Replaceable("{balance}", String.format("%.2f", company.getBalance()))
                    , new Replaceable("{owner_name}", owner.getName() != null ? owner.getName() : "Unknown")
                    , new Replaceable("{created_date}", dateFormat.format(new Date(company.getCreatedAt()))));

            meta.lore(lore);
            companyItem.setItemMeta(meta);
            inventory.setItem(companySlot, companyItem);

            // Balance display
            Material balanceMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("company_settings.balance_display", Material.GOLD_INGOT);
            int balanceSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("company_settings.balance_display", 0);
            ItemStack balanceItem = new ItemStack(balanceMaterial);
            ItemMeta balanceMeta = balanceItem.getItemMeta();
            balanceMeta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("company_settings.balance_display"));

            List<Component> balanceLore = QuickStocksPlugin.getGuiConfig().getItemLore("company_settings.balance_display", new Replaceable("{balance}", String.format("%.2f", company.getBalance())));
            balanceMeta.lore(balanceLore);
            balanceItem.setItemMeta(balanceMeta);
            inventory.setItem(balanceSlot, balanceItem);

        } catch (Exception e) {
            logger.warning("Error adding company info: " + e.getMessage());
        }
    }

    /**
     * Adds player's job information
     */
    private void addPlayerJobInfo() {
        try {
            String playerUuid = player.getUniqueId().toString();
            Optional<CompanyJob> jobOpt = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);

            if (jobOpt.isEmpty()) {
                return;
            }

            CompanyJob job = jobOpt.get();

            Material jobMaterial = QuickStocksPlugin.getGuiConfig().getItemMaterial("company_settings.player_job", Material.NAME_TAG);
            int jobSlot = QuickStocksPlugin.getGuiConfig().getItemSlot("company_settings.player_job", 8);
            ItemStack jobItem = new ItemStack(jobMaterial);
            ItemMeta meta = jobItem.getItemMeta();
            meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName("company_settings.player_job"));

            List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore("company_settings.player_job", new Replaceable("{job_title}", job.getTitle()));

            String permPrefix = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.player_job.permission_prefix", "&a✓ &f");
            String noPerm = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.player_job.no_permissions", "&cNo special permissions");

            boolean hasPerms = false;
            if (job.canManageCompany()) {
                lore.add(ChatUT.hexComp(permPrefix + "Manage Company"));
                hasPerms = true;
            }
            if (job.canInvite()) {
                lore.add(ChatUT.hexComp(permPrefix + "Invite Players"));
                hasPerms = true;
            }
            if (job.canCreateTitles()) {
                lore.add(ChatUT.hexComp(permPrefix + "Create Job Titles"));
                hasPerms = true;
            }
            if (job.canWithdraw()) {
                lore.add(ChatUT.hexComp(permPrefix + "Withdraw Funds"));
                hasPerms = true;
            }

            if (!hasPerms) {
                lore.add(ChatUT.hexComp(noPerm));
            }

            meta.lore(lore);
            jobItem.setItemMeta(meta);
            inventory.setItem(jobSlot, jobItem);

        } catch (Exception e) {
            logger.warning("Error adding player job info: " + e.getMessage());
        }
    }

    /**
     * Adds action buttons for quick commands
     */
    private void addActionButtons() {
        try {
            String playerUuid = player.getUniqueId().toString();
            Optional<CompanyJob> jobOpt = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), playerUuid);

            // View Employees button
            addButton("view_employees");

            // View Jobs button
            addButton("view_jobs");

            // Deposit button
            addButton("deposit");

            // Withdraw button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canWithdraw()) {
                addButton("withdraw");
            }

            // Assign Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canManageCompany()) {
                addButton("assign_job");
            }

            // Invite Player button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canInvite()) {
                addButton("invite_player");
            }

            // Create Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                addButton("create_job");
            }

            // Edit Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                addButton("edit_job");
            }

        } catch (Exception e) {
            logger.warning("Error adding action buttons: " + e.getMessage());
        }
    }

    /**
     * Adds navigation buttons
     */
    private void addNavigationButtons() {
        // Refresh button
        addButton("refresh");

        // Close button
        addButton("close");
    }

    /**
     * Helper method to add a button from config
     */
    private void addButton(String buttonName) {
        String path = "company_settings." + buttonName;
        Material material = QuickStocksPlugin.getGuiConfig().getItemMaterial(path, Material.STONE);
        int slot = QuickStocksPlugin.getGuiConfig().getItemSlot(path, 0);

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(QuickStocksPlugin.getGuiConfig().getItemName(path));
        List<Component> lore = QuickStocksPlugin.getGuiConfig().getItemLore(path, new Replaceable("{company_name}", company.getName()));
        meta.lore(lore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    /**
     * Opens the GUI for the player
     */
    public void open() {
        player.openInventory(inventory);
    }

    /**
     * Refreshes the GUI with updated information
     */
    public void refresh() {
        try {
            // Clear the inventory
            inventory.clear();

            // Reload company data
            Optional<Company> updatedCompanyOpt = QuickStocksPlugin.getCompanyService().getCompanyById(company.getId());
            if (updatedCompanyOpt.isPresent()) {
                // Replace with the updated company object
                this.company = updatedCompanyOpt.get();

                // Rebuild GUI
                setupGUI();
            }
        } catch (Exception e) {
            logger.warning("Error refreshing Company Settings GUI: " + e.getMessage());
            String errorMsg = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_settings.refresh_error", "&cFailed to refresh company settings.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
}
