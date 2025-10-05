package net.cyberneticforge.quickstocks.infrastructure.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration holder for company system settings.
 */
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
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public double getCreationCost() {
        return creationCost;
    }
    
    public void setCreationCost(double creationCost) {
        this.creationCost = creationCost;
    }
    
    public List<String> getDefaultTypes() {
        return defaultTypes;
    }
    
    public void setDefaultTypes(List<String> defaultTypes) {
        this.defaultTypes = defaultTypes;
    }
    
    public List<String> getDefaultJobTitles() {
        return defaultJobTitles;
    }
    
    public void setDefaultJobTitles(List<String> defaultJobTitles) {
        this.defaultJobTitles = defaultJobTitles;
    }
    
    public Map<String, JobPermissions> getPermissionsByTitle() {
        return permissionsByTitle;
    }
    
    public void setPermissionsByTitle(Map<String, JobPermissions> permissionsByTitle) {
        this.permissionsByTitle = permissionsByTitle;
    }
    
    public List<String> getMarketableTypes() {
        return marketableTypes;
    }
    
    public void setMarketableTypes(List<String> marketableTypes) {
        this.marketableTypes = marketableTypes;
    }
    
    public Map<String, Double> getMarketBalanceThresholds() {
        return marketBalanceThresholds;
    }
    
    public void setMarketBalanceThresholds(Map<String, Double> marketBalanceThresholds) {
        this.marketBalanceThresholds = marketBalanceThresholds;
    }
    
    public double getDefaultMarketPercentage() {
        return defaultMarketPercentage;
    }
    
    public void setDefaultMarketPercentage(double defaultMarketPercentage) {
        this.defaultMarketPercentage = defaultMarketPercentage;
    }
    
    public boolean isChestShopEnabled() {
        return chestShopEnabled;
    }
    
    public void setChestShopEnabled(boolean chestShopEnabled) {
        this.chestShopEnabled = chestShopEnabled;
    }
    
    public double getChestShopCompanyMinBalance() {
        return chestShopCompanyMinBalance;
    }
    
    public void setChestShopCompanyMinBalance(double chestShopCompanyMinBalance) {
        this.chestShopCompanyMinBalance = chestShopCompanyMinBalance;
    }
    
    public List<String> getPaymentCycles() {
        return paymentCycles;
    }
    
    public void setPaymentCycles(List<String> paymentCycles) {
        this.paymentCycles = paymentCycles;
    }
    
    public double getDefaultJobSalary() {
        return defaultJobSalary;
    }
    
    public void setDefaultJobSalary(double defaultJobSalary) {
        this.defaultJobSalary = defaultJobSalary;
    }
    
    public boolean isOfflinePayment() {
        return offlinePayment;
    }
    
    public void setOfflinePayment(boolean offlinePayment) {
        this.offlinePayment = offlinePayment;
    }
    
    public static class JobPermissions {
        private boolean canManageCompany = false;
        private boolean canInvite = false;
        private boolean canCreateJobTitles = false;
        private boolean canWithdraw = false;
        private boolean canManageChestShop = false;
        private boolean canManageSalaries = false;
        
        public boolean isCanManageCompany() {
            return canManageCompany;
        }
        
        public void setCanManageCompany(boolean canManageCompany) {
            this.canManageCompany = canManageCompany;
        }
        
        public boolean isCanInvite() {
            return canInvite;
        }
        
        public void setCanInvite(boolean canInvite) {
            this.canInvite = canInvite;
        }
        
        public boolean isCanCreateJobTitles() {
            return canCreateJobTitles;
        }
        
        public void setCanCreateJobTitles(boolean canCreateJobTitles) {
            this.canCreateJobTitles = canCreateJobTitles;
        }
        
        public boolean isCanWithdraw() {
            return canWithdraw;
        }
        
        public void setCanWithdraw(boolean canWithdraw) {
            this.canWithdraw = canWithdraw;
        }
        
        public boolean isCanManageChestShop() {
            return canManageChestShop;
        }
        
        public void setCanManageChestShop(boolean canManageChestShop) {
            this.canManageChestShop = canManageChestShop;
        }
        
        public boolean isCanManageSalaries() {
            return canManageSalaries;
        }
        
        public void setCanManageSalaries(boolean canManageSalaries) {
            this.canManageSalaries = canManageSalaries;
        }
    }
}
