package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when the market opens for trading.
 * This event is not cancellable - the market has already opened.
 */
@Getter
@SuppressWarnings("unused")
public class MarketOpenEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final long timestamp;
    
    public MarketOpenEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
