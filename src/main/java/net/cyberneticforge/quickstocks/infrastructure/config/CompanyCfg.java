package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Configuration manager for company system settings.
 * Loads configuration from companies.yml using YamlParser.
 */
@Getter
public class CompanyCfg {

    private final YamlParser config;
    private CompanyConfig companyConfig;
    
    public CompanyCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "companies.yml");
        loadValues();
    }
    
    /**
     * Loads all configuration values from the YAML file and populates CompanyConfig
     */
    private void loadValues() {
        companyConfig = new CompanyConfig();
        
        // Basic settings
        companyConfig.setEnabled(config.getBoolean("companies.enabled", true));
        companyConfig.setCreationCost(config.getDouble("companies.creationCost", 1000.0));
        
        // Default types
        List<String> defaultTypes = config.getStringList("companies.defaultTypes");
        if (defaultTypes.isEmpty()) {
            defaultTypes = Arrays.asList("PRIVATE", "PUBLIC", "DAO");
        }
        companyConfig.setDefaultTypes(defaultTypes);
        
        // Default job titles
        List<String> defaultJobTitles = config.getStringList("companies.defaultJobTitles");
        if (defaultJobTitles.isEmpty()) {
            defaultJobTitles = Arrays.asList("CEO", "CFO", "EMPLOYEE");
        }
        companyConfig.setDefaultJobTitles(defaultJobTitles);
        
        // Permissions by title
        Map<String, CompanyConfig.JobPermissions> permissionsByTitle = new HashMap<>();
        ConfigurationSection permsSection = config.getConfigurationSection("companies.permissionsByTitle");
        if (permsSection != null) {
            for (String title : permsSection.getKeys(false)) {
                CompanyConfig.JobPermissions perms = new CompanyConfig.JobPermissions();
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
            CompanyConfig.JobPermissions ceoPerms = new CompanyConfig.JobPermissions();
            ceoPerms.setCanManageCompany(true);
            ceoPerms.setCanInvite(true);
            ceoPerms.setCanCreateJobTitles(true);
            ceoPerms.setCanWithdraw(true);
            ceoPerms.setCanManageSalaries(true);
            permissionsByTitle.put("CEO", ceoPerms);
            
            CompanyConfig.JobPermissions cfoPerms = new CompanyConfig.JobPermissions();
            cfoPerms.setCanWithdraw(true);
            cfoPerms.setCanManageSalaries(true);
            permissionsByTitle.put("CFO", cfoPerms);
            
            CompanyConfig.JobPermissions employeePerms = new CompanyConfig.JobPermissions();
            permissionsByTitle.put("EMPLOYEE", employeePerms);
        }
        companyConfig.setPermissionsByTitle(permissionsByTitle);
        
        // Salary settings
        List<String> paymentCycles = config.getStringList("companies.salaries.paymentCycles");
        if (paymentCycles.isEmpty()) {
            paymentCycles = Arrays.asList("1h", "24h", "1w", "2w", "1m");
        }
        companyConfig.setPaymentCycles(paymentCycles);
        companyConfig.setDefaultJobSalary(config.getDouble("companies.salaries.defaultJobSalary", 0.0));
        companyConfig.setOfflinePayment(config.getBoolean("companies.salaries.offlinePayment", true));
        
        // ChestShop settings
        companyConfig.setChestShopEnabled(config.getBoolean("companies.chestshop.enabled", true));
        companyConfig.setChestShopCompanyMinBalance(config.getDouble("companies.chestshop.companyMinBalance", 1000.0));
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }
}
