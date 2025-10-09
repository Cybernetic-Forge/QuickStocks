package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a circuit breaker is triggered on an instrument.
 * Circuit breakers halt trading when price changes exceed configured thresholds.
 * This event is not cancellable - the circuit breaker has already been triggered.
 */
@SuppressWarnings("unused")
@Getter
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

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
