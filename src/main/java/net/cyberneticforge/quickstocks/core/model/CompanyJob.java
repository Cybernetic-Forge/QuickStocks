package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a job title/role within a company.
 */
@Getter
@Setter
public class CompanyJob {
    private final String id;
    private final String companyId;
    private final String title;
    private boolean canInvite;
    private boolean canCreateTitles;
    private boolean canWithdraw;
    private boolean canManageCompany;
    private boolean canManageChestShop;
    private boolean canManageSalaries;
    private boolean canManagePlots;
    
    public CompanyJob(String id, String companyId, String title, boolean canInvite, 
                     boolean canCreateTitles, boolean canWithdraw, boolean canManageCompany, 
                     boolean canManageChestShop, boolean canManageSalaries, boolean canManagePlots) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.canInvite = canInvite;
        this.canCreateTitles = canCreateTitles;
        this.canWithdraw = canWithdraw;
        this.canManageCompany = canManageCompany;
        this.canManageChestShop = canManageChestShop;
        this.canManageSalaries = canManageSalaries;
        this.canManagePlots = canManagePlots;
    }
    
    public boolean canInvite() {
        return canInvite;
    }
    
    public boolean canCreateTitles() {
        return canCreateTitles;
    }
    
    public boolean canWithdraw() {
        return canWithdraw;
    }
    
    public boolean canManageCompany() {
        return canManageCompany;
    }
    
    public boolean canManageChestShop() {
        return canManageChestShop;
    }
    
    public boolean canManageSalaries() {
        return canManageSalaries;
    }
    
    public boolean canManagePlots() {
        return canManagePlots;
    }
}
