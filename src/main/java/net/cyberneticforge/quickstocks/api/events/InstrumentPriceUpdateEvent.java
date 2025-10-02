package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when an instrument's price is updated.
 * This event is not cancellable - the price update has already occurred.
 */
public class InstrumentPriceUpdateEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String instrumentId;
    private final String instrumentSymbol;
    private final double oldPrice;
    private final double newPrice;
    private final double changePercent;
    private final long timestamp;
    
    public InstrumentPriceUpdateEvent(String instrumentId, String instrumentSymbol, 
                                      double oldPrice, double newPrice, long timestamp) {
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.changePercent = oldPrice > 0 ? ((newPrice - oldPrice) / oldPrice) * 100.0 : 0.0;
        this.timestamp = timestamp;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }
    
    public double getOldPrice() {
        return oldPrice;
    }
    
    public double getNewPrice() {
        return newPrice;
    }
    
    public double getChangePercent() {
        return changePercent;
    }
    
    public double getPriceChange() {
        return newPrice - oldPrice;
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
