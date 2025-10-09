package net.cyberneticforge.quickstocks.infrastructure.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration holder for company system settings.
 */
@Getter @Setter
public class CompanyConfig {
    
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
    
    public CompanyConfig() {
        // Set default permissions
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
        
        // Set default market balance thresholds
        marketBalanceThresholds.put("PUBLIC", 10000.0);
        marketBalanceThresholds.put("DAO", 15000.0);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isChestShopEnabled() {
        return chestShopEnabled;
    }

    @Setter
    @Getter
    public static class JobPermissions {
        private boolean canManageCompany = false;
        private boolean canInvite = false;
        private boolean canCreateJobTitles = false;
        private boolean canWithdraw = false;
        private boolean canManageChestShop = false;
        private boolean canManageSalaries = false;
    }
}
