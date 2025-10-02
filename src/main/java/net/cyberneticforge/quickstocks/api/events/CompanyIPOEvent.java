package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a company goes public (IPO - Initial Public Offering).
 * This event is cancellable - cancel to prevent the IPO.
 */
public class CompanyIPOEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final String companyId;
    private final String companyName;
    private final Player initiator;
    
    public CompanyIPOEvent(String companyId, String companyName, Player initiator) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.initiator = initiator;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public Player getInitiator() {
        return initiator;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
