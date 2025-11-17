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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GUI for viewing and managing company employees
 */
public class CompanyEmployeesGUI implements InventoryHolder {

    private static final PluginLogger logger = QuickStocksPlugin.getPluginLogger();

    private final Player player;
    @Getter
    private final Company company;
    private final Inventory inventory;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CompanyEmployeesGUI(Player player, Company company) {
        this.player = player;
        this.company = company;

        int guiSize = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_employees.size", 54);
        String title = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_employees.title", "&6{company_name} - Employees")
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

            // Add employee list
            addEmployees();

        } catch (Exception e) {
            logger.warning("Error setting up Company Employees GUI for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatUT.hexComp("&cFailed to load employees list."));
        }
    }

    private void addEmployees() {
        try {
            // Query employees from database
            List<Map<String, Object>> employeeData = QuickStocksPlugin.getDatabaseManager().getDb().query(
                    "SELECT ce.player_uuid, ce.joined_at, cj.title, cj.id as job_id " +
                            "FROM company_employees ce " +
                            "INNER JOIN company_jobs cj ON ce.job_id = cj.id " +
                            "WHERE ce.company_id = ? " +
                            "ORDER BY ce.joined_at",
                    company.getId()
            );

            int slot = 9; // Start from row 2
            for (Map<String, Object> data : employeeData) {
                if (slot >= 45) break; // Leave bottom row for navigation

                String playerUuid = (String) data.get("player_uuid");
                String jobTitle = (String) data.get("title");
                String jobId = (String) data.get("job_id");
                long joinedAt = ((Number) data.get("joined_at")).longValue();

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(playerUuid));

                ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setOwningPlayer(offlinePlayer);
                meta.displayName(ChatUT.hexComp("&e" + offlinePlayer.getName()));

                List<Component> lore = new ArrayList<>();
                lore.add(ChatUT.hexComp("&7Job: &f" + jobTitle));
                lore.add(ChatUT.hexComp("&7Joined: &f" + dateFormat.format(new Date(joinedAt))));
                lore.add(ChatUT.hexComp(""));
                
                // Check if player has permission to assign jobs
                Optional<CompanyJob> playerJob = QuickStocksPlugin.getCompanyService().getPlayerJob(company.getId(), player.getUniqueId().toString());
                if (playerJob.isPresent() && playerJob.get().canManageCompany()) {
                    lore.add(ChatUT.hexComp("&aClick to assign a different job"));
                }

                meta.lore(lore);
                item.setItemMeta(meta);

                inventory.setItem(slot, item);
                slot++;
            }

        } catch (SQLException e) {
            logger.warning("Error loading employees: " + e.getMessage());
        }
    }

    private void addNavigationButtons() {
        try {
            // Back button
            int backSlot = QuickStocksPlugin.getGuiConfig().getConfig().getInt("company_employees.back.slot", 49);
            Material backMaterial = Material.valueOf(QuickStocksPlugin.getGuiConfig().getConfig().getString("company_employees.back.material", "ARROW"));
            String backName = QuickStocksPlugin.getGuiConfig().getConfig().getString("company_employees.back.name", "&cBack");

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
