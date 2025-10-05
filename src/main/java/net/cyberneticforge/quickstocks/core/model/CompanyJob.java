package net.cyberneticforge.quickstocks.core.model;

/**
 * Represents a job title/role within a company.
 */
public class CompanyJob {
    private final String id;
    private final String companyId;
    private final String title;
    private final boolean canInvite;
    private final boolean canCreateTitles;
    private final boolean canWithdraw;
    private final boolean canManageCompany;
    private final boolean canManageChestShop;
    private final boolean canManageSalaries;
    
    public CompanyJob(String id, String companyId, String title, boolean canInvite, 
                     boolean canCreateTitles, boolean canWithdraw, boolean canManageCompany, 
                     boolean canManageChestShop, boolean canManageSalaries) {
        this.id = id;
        this.companyId = companyId;
        this.title = title;
        this.canInvite = canInvite;
        this.canCreateTitles = canCreateTitles;
        this.canWithdraw = canWithdraw;
        this.canManageCompany = canManageCompany;
        this.canManageChestShop = canManageChestShop;
        this.canManageSalaries = canManageSalaries;
    }
    
    // Overloaded constructor for backward compatibility
    public CompanyJob(String id, String companyId, String title, boolean canInvite, 
                     boolean canCreateTitles, boolean canWithdraw, boolean canManageCompany, 
                     boolean canManageChestShop) {
        this(id, companyId, title, canInvite, canCreateTitles, canWithdraw, 
             canManageCompany, canManageChestShop, false);
    }
    
    public String getId() {
        return id;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getTitle() {
        return title;
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
}
