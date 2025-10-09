package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when the market closes for trading.
 * This event is not cancellable - the market has already closed.
 */
@Getter
@SuppressWarnings("unused")
public class MarketCloseEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final long timestamp;
    
    public MarketCloseEvent(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
