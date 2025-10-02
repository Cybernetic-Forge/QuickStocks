package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when the market opens for trading.
 * This event is not cancellable - the market has already opened.
 */
public class MarketOpenEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final long timestamp;
    
    public MarketOpenEvent(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
