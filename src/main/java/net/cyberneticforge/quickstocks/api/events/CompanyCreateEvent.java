package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import net.cyberneticforge.quickstocks.core.model.Company;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a company is created.
 * This event is cancellable - cancel to prevent company creation.
 */
@Getter
@SuppressWarnings("unused")
public class CompanyCreateEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player creator;
    private final String companyName;
    private final String companyType;
    private Company company; // Set after creation if not cancelled

    public CompanyCreateEvent(Player creator, String companyName, String companyType) {
        this.creator = creator;
        this.companyName = companyName;
        this.companyType = companyType;
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
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
