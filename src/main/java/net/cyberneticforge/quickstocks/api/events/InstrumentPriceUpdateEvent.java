package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an instrument's price is updated.
 * This event is not cancellable - the price update has already occurred.
 */
@Getter
@SuppressWarnings("unused")
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

    public double getPriceChange() {
        return newPrice - oldPrice;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
