package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player leaves a company (voluntarily or kicked).
 * This event is not cancellable - the action has already been processed.
 */
@Getter
@SuppressWarnings("unused")
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
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
