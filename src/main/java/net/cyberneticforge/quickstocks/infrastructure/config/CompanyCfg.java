package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import net.cyberneticforge.quickstocks.QuickStocksPlugin;
import net.cyberneticforge.quickstocks.core.model.JobPermissions;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    // Plot/land ownership settings
    private boolean plotsEnabled = true;
    private double buyPlotPrice = 10000.0;
    private double sellPlotPrice = 8000.0;
    private double plotRent = -1.0;
    private String plotRentInterval = "monthly";
    
    // Terrain message settings
    private boolean terrainMessagesEnabled = true;
    private String terrainDisplayMode = "ACTIONBAR";
    private String terrainEnterMessage = "&aEntering &e%company%&a territory";
    private String terrainLeaveMessage = "&7Leaving &e%company%&7 territory";
    private String terrainWildernessMessage = "&7Entering wilderness";
    
    // Debt management settings
    private double allowedDebtChestShops = -5000.0;
    private double allowedDebtPlots = -10000.0;
    private double allowedDebtSalaries = -3000.0;
    
    public CompanyCfg() {
        config = YamlParser.loadOrExtract(QuickStocksPlugin.getInstance(), "companies.yml");
        addMissingDefaults();
        loadValues();
    }
    
    /**
     * Adds missing configuration entries with default values
     */
    private void addMissingDefaults() {
        // Basic settings
        config.addMissing("companies.enabled", true);
        config.addMissing("companies.creationCost", 1000.0);
        
        // Default types
        config.addMissing("companies.defaultTypes", Arrays.asList("PRIVATE", "PUBLIC", "DAO"));
        
        // Default job titles
        config.addMissing("companies.defaultJobTitles", Arrays.asList("CEO", "CFO", "EMPLOYEE"));
        
        // Permissions by title
        config.addMissing("companies.permissionsByTitle.CEO.canManageCompany", true);
        config.addMissing("companies.permissionsByTitle.CEO.canInvite", true);
        config.addMissing("companies.permissionsByTitle.CEO.canCreateJobTitles", true);
        config.addMissing("companies.permissionsByTitle.CEO.canWithdraw", true);
        config.addMissing("companies.permissionsByTitle.CEO.canManageSalaries", true);
        
        config.addMissing("companies.permissionsByTitle.CFO.canWithdraw", true);
        config.addMissing("companies.permissionsByTitle.CFO.canInvite", false);
        config.addMissing("companies.permissionsByTitle.CFO.canCreateJobTitles", false);
        config.addMissing("companies.permissionsByTitle.CFO.canManageSalaries", true);
        
        config.addMissing("companies.permissionsByTitle.EMPLOYEE.canInvite", false);
        config.addMissing("companies.permissionsByTitle.EMPLOYEE.canWithdraw", false);
        config.addMissing("companies.permissionsByTitle.EMPLOYEE.canCreateJobTitles", false);
        config.addMissing("companies.permissionsByTitle.EMPLOYEE.canManageSalaries", false);
        
        // Salary settings
        config.addMissing("companies.salaries.paymentCycles", Arrays.asList("1h", "24h", "1w", "2w", "1m"));
        config.addMissing("companies.salaries.defaultJobSalary", 0.0);
        config.addMissing("companies.salaries.offlinePayment", true);
        
        // ChestShop settings
        config.addMissing("companies.chestshop.enabled", true);
        config.addMissing("companies.chestshop.companyMinBalance", 1000.0);
        
        // Plot settings
        config.addMissing("companies.plots.enabled", true);
        config.addMissing("companies.plots.buyPlotPrice", 10000.0);
        config.addMissing("companies.plots.sellPlotPrice", 8000.0);
        config.addMissing("companies.plots.plotRent", -1.0);
        config.addMissing("companies.plots.plotRentInterval", "monthly");
        
        // Terrain message settings
        config.addMissing("companies.plots.terrainMessages.enabled", true);
        config.addMissing("companies.plots.terrainMessages.displayMode", "ACTIONBAR");
        config.addMissing("companies.plots.terrainMessages.enterMessage", "&aEntering &e%company%&a territory");
        config.addMissing("companies.plots.terrainMessages.leaveMessage", "&7Leaving &e%company%&7 territory");
        config.addMissing("companies.plots.terrainMessages.wildernessMessage", "&7Entering wilderness");
        
        // Debt management settings
        config.addMissing("companies.allowedDebts.chestshops", -5000.0);
        config.addMissing("companies.allowedDebts.companyPlots", -10000.0);
        config.addMissing("companies.allowedDebts.salaries", -3000.0);
        
        config.saveChanges();
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
                perms.setCanManagePlots(config.getBoolean(basePath + ".canManagePlots", false));
                
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
            ceoPerms.setCanManagePlots(true);
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
        
        // Plot settings
        setPlotsEnabled(config.getBoolean("companies.plots.enabled", true));
        setBuyPlotPrice(config.getDouble("companies.plots.buyPlotPrice", 10000.0));
        setSellPlotPrice(config.getDouble("companies.plots.sellPlotPrice", 8000.0));
        setPlotRent(config.getDouble("companies.plots.plotRent", -1.0));
        setPlotRentInterval(config.getString("companies.plots.plotRentInterval", "monthly"));
        
        // Terrain message settings
        setTerrainMessagesEnabled(config.getBoolean("companies.plots.terrainMessages.enabled", true));
        setTerrainDisplayMode(config.getString("companies.plots.terrainMessages.displayMode", "ACTIONBAR"));
        setTerrainEnterMessage(config.getString("companies.plots.terrainMessages.enterMessage", "&aEntering &e%company%&a territory"));
        setTerrainLeaveMessage(config.getString("companies.plots.terrainMessages.leaveMessage", "&7Leaving &e%company%&7 territory"));
        setTerrainWildernessMessage(config.getString("companies.plots.terrainMessages.wildernessMessage", "&7Entering wilderness"));
        
        // Debt management settings
        setAllowedDebtChestShops(config.getDouble("companies.allowedDebts.chestshops", -5000.0));
        setAllowedDebtPlots(config.getDouble("companies.allowedDebts.companyPlots", -10000.0));
        setAllowedDebtSalaries(config.getDouble("companies.allowedDebts.salaries", -3000.0));
    }
    
    /**
     * Reloads the configuration from disk
     */
    public void reload() {
        config.reload();
        loadValues();
    }
}
