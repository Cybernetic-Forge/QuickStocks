package net.cyberneticforge.quickstocks.core.model;

import lombok.Getter;

/**
 * Represents permissions for a specific job role on a specific plot.
 */
@Getter
public class PlotPermission {
    private final String id;
    private final String plotId;
    private final String jobId;
    private final boolean canBuild;      // Can break/place blocks
    private final boolean canInteract;   // Can use buttons, levers, doors, etc.
    private final boolean canContainer;  // Can access containers (chests, furnaces, etc.)
    
    public PlotPermission(String id, String plotId, String jobId, 
                          boolean canBuild, boolean canInteract, boolean canContainer) {
        this.id = id;
        this.plotId = plotId;
        this.jobId = jobId;
        this.canBuild = canBuild;
        this.canInteract = canInteract;
        this.canContainer = canContainer;
    }
    
    public boolean canBuild() {
        return canBuild;
    }
    
    public boolean canInteract() {
        return canInteract;
    }
    
    public boolean canContainer() {
        return canContainer;
    }
}
