package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPermissions {
    private boolean canManageCompany = false;
    private boolean canInvite = false;
    private boolean canCreateJobTitles = false;
    private boolean canWithdraw = false;
    private boolean canManageChestShop = false;
    private boolean canManageSalaries = false;
}