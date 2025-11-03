package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents default plot permissions for a company.
 * Used as a template when creating new plots.
 */
@Getter
@Setter
public class DefaultPlotPermissions {
    // Map of job title to permission settings
    private Map<String, JobPlotPermissions> permissionsByJob = new HashMap<>();
    
    @Getter
    @Setter
    public static class JobPlotPermissions {
        private boolean canBuild = true;
        private boolean canInteract = true;
        private boolean canContainer = true;
    }
}
