package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.JobPermissions;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Configuration manager for company system settings.
 * Loads configuration from companies.yml using YamlParser.
 */
@Getter
@Setter
public class CompanyCfg {

    private final YamlParser config;

    private boolean enabled = true;
    private double creationCost = 1000.0;
    private List<String> defaultTypes = Arrays.asList("PRIVATE", "PUBLIC", "DAO");
    private List<String> defaultJobTitles = Arrays.asList("CEO", "CFO", "EMPLOYEE");
    private Map<String, JobPermissions> permissionsByTitle = new HashMap<>();

    // Market-related settings
    private List<String> marketableTypes = Arrays.asList("PUBLIC", "DAO");
    private Map<String, Double> marketBalanceThresholds = new HashMap<>();
    private double defaultMarketPercentage = 70.0;

    // ChestShop integration settings
    private boolean chestShopEnabled = true;
    private double chestShopCompanyMinBalance = 1000.0;

    // Salary settings
    private List<String> paymentCycles = Arrays.asList("1h", "24h", "1w", "2w", "1m");
    private double defaultJobSalary = 0.0;
    private boolean offlinePayment = true;
    
    public CompanyCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "companies.yml");
        loadValues();
    }
    
    /**
     * Loads all configuration values from the YAML file and populates CompanyConfig
     */
    private void loadValues() {
        
        // Basic settings
        setEnabled(config.getBoolean("companies.enabled", true));
        setCreationCost(config.getDouble("companies.creationCost", 1000.0));
        
        // Default types
        List<String> defaultTypes = config.getStringList("companies.defaultTypes");
        if (defaultTypes.isEmpty()) {
            defaultTypes = Arrays.asList("PRIVATE", "PUBLIC", "DAO");
        }
        setDefaultTypes(defaultTypes);
        
        // Default job titles
        List<String> defaultJobTitles = config.getStringList("companies.defaultJobTitles");
        if (defaultJobTitles.isEmpty()) {
            defaultJobTitles = Arrays.asList("CEO", "CFO", "EMPLOYEE");
        }
        setDefaultJobTitles(defaultJobTitles);
        
        // Permissions by title
        Map<String, JobPermissions> permissionsByTitle = new HashMap<>();
        ConfigurationSection permsSection = config.getConfigurationSection("companies.permissionsByTitle");
        if (permsSection != null) {
            for (String title : permsSection.getKeys(false)) {
                JobPermissions perms = new JobPermissions();
                String basePath = "companies.permissionsByTitle." + title;
                
                perms.setCanManageCompany(config.getBoolean(basePath + ".canManageCompany", false));
                perms.setCanInvite(config.getBoolean(basePath + ".canInvite", false));
                perms.setCanCreateJobTitles(config.getBoolean(basePath + ".canCreateJobTitles", false));
                perms.setCanWithdraw(config.getBoolean(basePath + ".canWithdraw", false));
                perms.setCanManageSalaries(config.getBoolean(basePath + ".canManageSalaries", false));
                
                permissionsByTitle.put(title, perms);
            }
        }
        
        // Apply defaults if no permissions configured
        if (permissionsByTitle.isEmpty()) {
            JobPermissions ceoPerms = new JobPermissions();
            ceoPerms.setCanManageCompany(true);
            ceoPerms.setCanInvite(true);
            ceoPerms.setCanCreateJobTitles(true);
            ceoPerms.setCanWithdraw(true);
            ceoPerms.setCanManageSalaries(true);
            permissionsByTitle.put("CEO", ceoPerms);
            
            JobPermissions cfoPerms = new JobPermissions();
            cfoPerms.setCanWithdraw(true);
            cfoPerms.setCanManageSalaries(true);
            permissionsByTitle.put("CFO", cfoPerms);
            
            JobPermissions employeePerms = new JobPermissions();
            permissionsByTitle.put("EMPLOYEE", employeePerms);
        }
        setPermissionsByTitle(permissionsByTitle);
        
        // Salary settings
        List<String> paymentCycles = config.getStringList("companies.salaries.paymentCycles");
        if (paymentCycles.isEmpty()) {
            paymentCycles = Arrays.asList("1h", "24h", "1w", "2w", "1m");
        }
        setPaymentCycles(paymentCycles);
        setDefaultJobSalary(config.getDouble("companies.salaries.defaultJobSalary", 0.0));
        setOfflinePayment(config.getBoolean("companies.salaries.offlinePayment", true));
        
        // ChestShop settings
        setChestShopEnabled(config.getBoolean("companies.chestshop.enabled", true));
        setChestShopCompanyMinBalance(config.getDouble("companies.chestshop.companyMinBalance", 1000.0));
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }
}
