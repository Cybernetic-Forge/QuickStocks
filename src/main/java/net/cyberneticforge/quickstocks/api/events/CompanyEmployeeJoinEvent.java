package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player joins a company (accepts an invitation).
 * This event is not cancellable - the join has already been processed.
 */
@Getter
@SuppressWarnings("unused")
public class CompanyEmployeeJoinEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String companyId;
    private final String companyName;
    private final Player employee;
    private final String jobTitle;
    
    public CompanyEmployeeJoinEvent(String companyId, String companyName, Player employee, String jobTitle) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.employee = employee;
        this.jobTitle = jobTitle;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
