package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player leaves a company (voluntarily or kicked).
 * This event is not cancellable - the action has already been processed.
 */
public class CompanyEmployeeLeaveEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String companyId;
    private final String companyName;
    private final Player employee;
    private final boolean wasKicked;
    
    public CompanyEmployeeLeaveEvent(String companyId, String companyName, Player employee, boolean wasKicked) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.employee = employee;
        this.wasKicked = wasKicked;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public Player getEmployee() {
        return employee;
    }
    
    public boolean wasKicked() {
        return wasKicked;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
