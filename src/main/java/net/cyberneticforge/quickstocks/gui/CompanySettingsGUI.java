package net.cyberneticforge.quickstocks.gui;

import net.cyberneticforge.quickstocks.core.model.Company;
import net.cyberneticforge.quickstocks.core.model.CompanyJob;
import net.cyberneticforge.quickstocks.core.services.CompanyService;
import net.cyberneticforge.quickstocks.utils.ChatUT;
import net.cyberneticforge.quickstocks.utils.GUIConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Company Settings GUI for managing company settings and viewing information
 */
public class CompanySettingsGUI implements InventoryHolder {
    
    private static final Logger logger = Logger.getLogger(CompanySettingsGUI.class.getName());
    
    private final Player player;
    private final CompanyService companyService;
    private final GUIConfigManager guiConfig;
    private Company company;  // Not final so it can be refreshed
    private final Inventory inventory;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public CompanySettingsGUI(Player player, CompanyService companyService, Company company, GUIConfigManager guiConfig) {
        this.player = player;
        this.companyService = companyService;
        this.company = company;
        this.guiConfig = guiConfig;
        
        int guiSize = guiConfig.getInt("company_settings.size", 54);
        String title = guiConfig.getString("company_settings.title", "&6Company: &f{company_name}")
            .replace("{company_name}", company.getName());
        
        this.inventory = Bukkit.createInventory(this, guiSize, 
            ChatUT.serialize(ChatUT.hexComp(title)));
        
        setupGUI();
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * Gets the company associated with this GUI
     */
    public Company getCompany() {
        return company;
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
            String errorMsg = guiConfig.getString("company_settings.error_message", "&cFailed to load company settings.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
    
    /**
     * Adds company information display
     */
    private void addCompanyInfo() {
        try {
            // Company info item
            Material companyMaterial = guiConfig.getItemMaterial("company_settings.company_info", Material.GOLDEN_HELMET);
            int companySlot = guiConfig.getItemSlot("company_settings.company_info", 4);
            ItemStack companyItem = new ItemStack(companyMaterial);
            ItemMeta meta = companyItem.getItemMeta();
            
            String companyName = guiConfig.getString("company_settings.company_info.name", "&6{company_name}")
                .replace("{company_name}", company.getName());
            meta.setDisplayName(ChatUT.serialize(ChatUT.hexComp(companyName)));
            
            List<String> lorePatt = guiConfig.getItemLoreStrings("company_settings.company_info");
            List<String> lore = new ArrayList<>();
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(company.getOwnerUuid()));
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
            
            for (String line : lorePatt) {
                String processedLine = line
                    .replace("{company_type}", company.getType())
                    .replace("{balance}", String.format("%.2f", company.getBalance()))
                    .replace("{owner_name}", ownerName)
                    .replace("{created_date}", dateFormat.format(new Date(company.getCreatedAt())));
                lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            meta.setLore(lore);
            companyItem.setItemMeta(meta);
            inventory.setItem(companySlot, companyItem);
            
            // Balance display
            Material balanceMaterial = guiConfig.getItemMaterial("company_settings.balance_display", Material.GOLD_INGOT);
            int balanceSlot = guiConfig.getItemSlot("company_settings.balance_display", 0);
            ItemStack balanceItem = new ItemStack(balanceMaterial);
            ItemMeta balanceMeta = balanceItem.getItemMeta();
            balanceMeta.setDisplayName(guiConfig.getItemNameString("company_settings.balance_display"));
            
            List<String> balanceLorePatt = guiConfig.getItemLoreStrings("company_settings.balance_display");
            List<String> balanceLore = new ArrayList<>();
            for (String line : balanceLorePatt) {
                String processedLine = line.replace("{balance}", String.format("%.2f", company.getBalance()));
                balanceLore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            balanceMeta.setLore(balanceLore);
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
            Optional<CompanyJob> jobOpt = companyService.getPlayerJob(company.getId(), playerUuid);
            
            if (jobOpt.isEmpty()) {
                return;
            }
            
            CompanyJob job = jobOpt.get();
            
            Material jobMaterial = guiConfig.getItemMaterial("company_settings.player_job", Material.NAME_TAG);
            int jobSlot = guiConfig.getItemSlot("company_settings.player_job", 8);
            ItemStack jobItem = new ItemStack(jobMaterial);
            ItemMeta meta = jobItem.getItemMeta();
            meta.setDisplayName(guiConfig.getItemNameString("company_settings.player_job"));
            
            List<String> lorePatt = guiConfig.getItemLoreStrings("company_settings.player_job");
            List<String> lore = new ArrayList<>();
            
            for (String line : lorePatt) {
                String processedLine = line.replace("{job_title}", job.getTitle());
                lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
            }
            
            String permPrefix = guiConfig.getString("company_settings.player_job.permission_prefix", "&a✓ &f");
            String noPerm = guiConfig.getString("company_settings.player_job.no_permissions", "&cNo special permissions");
            
            boolean hasPerms = false;
            if (job.canManageCompany()) {
                lore.add(ChatUT.serialize(ChatUT.hexComp(permPrefix + "Manage Company")));
                hasPerms = true;
            }
            if (job.canInvite()) {
                lore.add(ChatUT.serialize(ChatUT.hexComp(permPrefix + "Invite Players")));
                hasPerms = true;
            }
            if (job.canCreateTitles()) {
                lore.add(ChatUT.serialize(ChatUT.hexComp(permPrefix + "Create Job Titles")));
                hasPerms = true;
            }
            if (job.canWithdraw()) {
                lore.add(ChatUT.serialize(ChatUT.hexComp(permPrefix + "Withdraw Funds")));
                hasPerms = true;
            }
            
            if (!hasPerms) {
                lore.add(ChatUT.serialize(ChatUT.hexComp(noPerm)));
            }
            
            meta.setLore(lore);
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
            Optional<CompanyJob> jobOpt = companyService.getPlayerJob(company.getId(), playerUuid);
            
            // View Employees button
            addButton("view_employees", null);
            
            // View Jobs button
            addButton("view_jobs", null);
            
            // Deposit button
            addButton("deposit", null);
            
            // Withdraw button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canWithdraw()) {
                addButton("withdraw", null);
            }
            
            // Assign Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canManageCompany()) {
                addButton("assign_job", null);
            }
            
            // Invite Player button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canInvite()) {
                addButton("invite_player", null);
            }
            
            // Create Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                addButton("create_job", null);
            }

            // Edit Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                addButton("edit_job", null);
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
        addButton("refresh", null);
        
        // Close button
        addButton("close", null);
    }
    
    /**
     * Helper method to add a button from config
     */
    private void addButton(String buttonName, CompanyJob job) {
        String path = "company_settings." + buttonName;
        Material material = guiConfig.getItemMaterial(path, Material.STONE);
        int slot = guiConfig.getItemSlot(path, 0);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(guiConfig.getItemNameString(path));
        
        List<String> lorePatt = guiConfig.getItemLoreStrings(path);
        List<String> lore = new ArrayList<>();
        for (String line : lorePatt) {
            String processedLine = line.replace("{company_name}", company.getName());
            lore.add(ChatUT.serialize(ChatUT.hexComp(processedLine)));
        }
        
        meta.setLore(lore);
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
            Optional<Company> updatedCompanyOpt = companyService.getCompanyById(company.getId());
            if (updatedCompanyOpt.isPresent()) {
                // Replace with the updated company object
                this.company = updatedCompanyOpt.get();
                
                // Rebuild GUI
                setupGUI();
            }
        } catch (Exception e) {
            logger.warning("Error refreshing Company Settings GUI: " + e.getMessage());
            String errorMsg = guiConfig.getString("company_settings.refresh_error", "&cFailed to refresh company settings.");
            player.sendMessage(ChatUT.hexComp(errorMsg));
        }
    }
}
