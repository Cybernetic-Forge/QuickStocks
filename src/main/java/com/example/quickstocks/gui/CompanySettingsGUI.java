package com.example.quickstocks.gui;

import com.example.quickstocks.core.model.Company;
import com.example.quickstocks.core.model.CompanyJob;
import com.example.quickstocks.core.services.CompanyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
    private static final int GUI_SIZE = 54; // 6 rows
    
    private final Player player;
    private final CompanyService companyService;
    private Company company;  // Not final so it can be refreshed
    private final Inventory inventory;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    public CompanySettingsGUI(Player player, CompanyService companyService, Company company) {
        this.player = player;
        this.companyService = companyService;
        this.company = company;
        this.inventory = Bukkit.createInventory(this, GUI_SIZE, 
            ChatColor.GOLD + "Company: " + ChatColor.WHITE + company.getName());
        
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
            player.sendMessage(ChatColor.RED + "Failed to load company settings.");
        }
    }
    
    /**
     * Adds company information display
     */
    private void addCompanyInfo() {
        try {
            // Company info item (top left)
            ItemStack companyItem = new ItemStack(Material.GOLDEN_HELMET);
            ItemMeta meta = companyItem.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + company.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + company.getType());
            lore.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + "$" + String.format("%.2f", company.getBalance()));
            
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(company.getOwnerUuid()));
            String ownerName = owner.getName() != null ? owner.getName() : "Unknown";
            lore.add(ChatColor.GRAY + "Owner: " + ChatColor.YELLOW + ownerName);
            lore.add(ChatColor.GRAY + "Created: " + ChatColor.WHITE + dateFormat.format(new Date(company.getCreatedAt())));
            
            meta.setLore(lore);
            companyItem.setItemMeta(meta);
            inventory.setItem(4, companyItem);
            
            // Balance display
            ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta balanceMeta = balanceItem.getItemMeta();
            balanceMeta.setDisplayName(ChatColor.GOLD + "Company Balance");
            
            List<String> balanceLore = new ArrayList<>();
            balanceLore.add(ChatColor.GREEN + "$" + String.format("%.2f", company.getBalance()));
            balanceLore.add("");
            balanceLore.add(ChatColor.GRAY + "Use deposit/withdraw commands");
            balanceLore.add(ChatColor.GRAY + "to manage funds");
            
            balanceMeta.setLore(balanceLore);
            balanceItem.setItemMeta(balanceMeta);
            inventory.setItem(0, balanceItem);
            
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
            
            ItemStack jobItem = new ItemStack(Material.NAME_TAG);
            ItemMeta meta = jobItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Your Position");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + job.getTitle());
            lore.add("");
            lore.add(ChatColor.GRAY + "Permissions:");
            
            if (job.canManageCompany()) {
                lore.add(ChatColor.GREEN + "✓ " + ChatColor.WHITE + "Manage Company");
            }
            if (job.canInvite()) {
                lore.add(ChatColor.GREEN + "✓ " + ChatColor.WHITE + "Invite Players");
            }
            if (job.canCreateTitles()) {
                lore.add(ChatColor.GREEN + "✓ " + ChatColor.WHITE + "Create Job Titles");
            }
            if (job.canWithdraw()) {
                lore.add(ChatColor.GREEN + "✓ " + ChatColor.WHITE + "Withdraw Funds");
            }
            
            if (!job.canManageCompany() && !job.canInvite() && !job.canCreateTitles() && !job.canWithdraw()) {
                lore.add(ChatColor.RED + "No special permissions");
            }
            
            meta.setLore(lore);
            jobItem.setItemMeta(meta);
            inventory.setItem(8, jobItem);
            
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
            ItemStack employeesItem = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta employeesMeta = employeesItem.getItemMeta();
            employeesMeta.setDisplayName(ChatColor.AQUA + "View Employees");
            List<String> employeesLore = new ArrayList<>();
            employeesLore.add(ChatColor.GRAY + "Click to view all employees");
            employeesLore.add(ChatColor.YELLOW + "Command: /company employees " + company.getName());
            employeesMeta.setLore(employeesLore);
            employeesItem.setItemMeta(employeesMeta);
            inventory.setItem(19, employeesItem);
            
            // View Jobs button
            ItemStack jobsItem = new ItemStack(Material.WRITABLE_BOOK);
            ItemMeta jobsMeta = jobsItem.getItemMeta();
            jobsMeta.setDisplayName(ChatColor.AQUA + "View Job Titles");
            List<String> jobsLore = new ArrayList<>();
            jobsLore.add(ChatColor.GRAY + "Click to view all job titles");
            jobsLore.add(ChatColor.YELLOW + "Command: /company jobs " + company.getName());
            jobsMeta.setLore(jobsLore);
            jobsItem.setItemMeta(jobsMeta);
            inventory.setItem(20, jobsItem);
            
            // Deposit button
            ItemStack depositItem = new ItemStack(Material.HOPPER);
            ItemMeta depositMeta = depositItem.getItemMeta();
            depositMeta.setDisplayName(ChatColor.GREEN + "Deposit Funds");
            List<String> depositLore = new ArrayList<>();
            depositLore.add(ChatColor.GRAY + "Click to deposit funds");
            depositLore.add(ChatColor.YELLOW + "Command: /company deposit " + company.getName() + " <amount>");
            depositMeta.setLore(depositLore);
            depositItem.setItemMeta(depositMeta);
            inventory.setItem(21, depositItem);
            
            // Withdraw button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canWithdraw()) {
                ItemStack withdrawItem = new ItemStack(Material.DISPENSER);
                ItemMeta withdrawMeta = withdrawItem.getItemMeta();
                withdrawMeta.setDisplayName(ChatColor.GOLD + "Withdraw Funds");
                List<String> withdrawLore = new ArrayList<>();
                withdrawLore.add(ChatColor.GRAY + "Click to withdraw funds");
                withdrawLore.add(ChatColor.YELLOW + "Command: /company withdraw " + company.getName() + " <amount>");
                withdrawMeta.setLore(withdrawLore);
                withdrawItem.setItemMeta(withdrawMeta);
                inventory.setItem(22, withdrawItem);
            }
            
            // Invite Player button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canInvite()) {
                ItemStack inviteItem = new ItemStack(Material.PAPER);
                ItemMeta inviteMeta = inviteItem.getItemMeta();
                inviteMeta.setDisplayName(ChatColor.GREEN + "Invite Player");
                List<String> inviteLore = new ArrayList<>();
                inviteLore.add(ChatColor.GRAY + "Click to invite a player");
                inviteLore.add(ChatColor.YELLOW + "Command: /company invite " + company.getName() + " <player> <job>");
                inviteMeta.setLore(inviteLore);
                inviteItem.setItemMeta(inviteMeta);
                inventory.setItem(24, inviteItem);
            }
            
            // Create Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                ItemStack createJobItem = new ItemStack(Material.BOOK);
                ItemMeta createJobMeta = createJobItem.getItemMeta();
                createJobMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Create Job Title");
                List<String> createJobLore = new ArrayList<>();
                createJobLore.add(ChatColor.GRAY + "Click to create a new job");
                createJobLore.add(ChatColor.YELLOW + "Command: /company createjob " + company.getName());
                createJobLore.add(ChatColor.YELLOW + "         <title> <permissions>");
                createJobMeta.setLore(createJobLore);
                createJobItem.setItemMeta(createJobMeta);
                inventory.setItem(25, createJobItem);
            }
            
            // Edit Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canCreateTitles()) {
                ItemStack editJobItem = new ItemStack(Material.WRITABLE_BOOK);
                ItemMeta editJobMeta = editJobItem.getItemMeta();
                editJobMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Edit Job Title");
                List<String> editJobLore = new ArrayList<>();
                editJobLore.add(ChatColor.GRAY + "Click to edit an existing job");
                editJobLore.add(ChatColor.YELLOW + "Command: /company editjob " + company.getName());
                editJobLore.add(ChatColor.YELLOW + "         <title> <permissions>");
                editJobMeta.setLore(editJobLore);
                editJobItem.setItemMeta(editJobMeta);
                inventory.setItem(26, editJobItem);
            }
            
            // Assign Job button (if player has permission)
            if (jobOpt.isPresent() && jobOpt.get().canManageCompany()) {
                ItemStack assignJobItem = new ItemStack(Material.ENCHANTED_BOOK);
                ItemMeta assignJobMeta = assignJobItem.getItemMeta();
                assignJobMeta.setDisplayName(ChatColor.AQUA + "Assign Job Title");
                List<String> assignJobLore = new ArrayList<>();
                assignJobLore.add(ChatColor.GRAY + "Click to assign a job to employee");
                assignJobLore.add(ChatColor.YELLOW + "Command: /company assignjob " + company.getName());
                assignJobLore.add(ChatColor.YELLOW + "         <player> <job>");
                assignJobMeta.setLore(assignJobLore);
                assignJobItem.setItemMeta(assignJobMeta);
                inventory.setItem(23, assignJobItem);
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
        ItemStack refreshItem = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.YELLOW + "Refresh");
        List<String> refreshLore = new ArrayList<>();
        refreshLore.add(ChatColor.GRAY + "Click to refresh company info");
        refreshMeta.setLore(refreshLore);
        refreshItem.setItemMeta(refreshMeta);
        inventory.setItem(49, refreshItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        List<String> closeLore = new ArrayList<>();
        closeLore.add(ChatColor.GRAY + "Click to close this menu");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
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
            player.sendMessage(ChatColor.RED + "Failed to refresh company settings.");
        }
    }
}
