package com.example.quickstocks.infrastructure.config;

import java.util.*;

/**
 * Configuration holder for company system settings.
 */
public class CompanyConfig {
    
    private boolean enabled = true;
    private double creationCost = 1000.0;
    private List<String> defaultTypes = Arrays.asList("PRIVATE", "PUBLIC", "DAO");
    private List<String> defaultJobTitles = Arrays.asList("CEO", "CFO", "EMPLOYEE");
    private Map<String, JobPermissions> permissionsByTitle = new HashMap<>();
    
    public CompanyConfig() {
        // Set default permissions
        JobPermissions ceoPerms = new JobPermissions();
        ceoPerms.setCanManageCompany(true);
        ceoPerms.setCanInvite(true);
        ceoPerms.setCanCreateJobTitles(true);
        ceoPerms.setCanWithdraw(true);
        permissionsByTitle.put("CEO", ceoPerms);
        
        JobPermissions cfoPerms = new JobPermissions();
        cfoPerms.setCanWithdraw(true);
        permissionsByTitle.put("CFO", cfoPerms);
        
        JobPermissions employeePerms = new JobPermissions();
        permissionsByTitle.put("EMPLOYEE", employeePerms);
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
    
    public static class JobPermissions {
        private boolean canManageCompany = false;
        private boolean canInvite = false;
        private boolean canCreateJobTitles = false;
        private boolean canWithdraw = false;
        
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
    }
}
