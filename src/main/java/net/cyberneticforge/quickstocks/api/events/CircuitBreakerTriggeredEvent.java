package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a circuit breaker is triggered on an instrument.
 * Circuit breakers halt trading when price changes exceed configured thresholds.
 * This event is not cancellable - the circuit breaker has already been triggered.
 */
public class CircuitBreakerTriggeredEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final String instrumentId;
    private final String instrumentSymbol;
    private final double priceChangePercent;
    private final boolean isHalted;
    private final long timestamp;
    
    public CircuitBreakerTriggeredEvent(String instrumentId, String instrumentSymbol, 
                                        double priceChangePercent, boolean isHalted, long timestamp) {
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
        this.priceChangePercent = priceChangePercent;
        this.isHalted = isHalted;
        this.timestamp = timestamp;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }
    
    public double getPriceChangePercent() {
        return priceChangePercent;
    }
    
    public boolean isHalted() {
        return isHalted;
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
