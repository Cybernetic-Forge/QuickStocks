package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player buys an instrument (stock, crypto, item).
 * This event is cancellable - cancel to prevent the purchase.
 */
@Getter
@SuppressWarnings("unused")
public class InstrumentBuyEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player buyer;
    private final String instrumentId;
    private final String instrumentSymbol;
    private final int quantity;
    private final double pricePerUnit;
    private final double totalCost;
    
    public InstrumentBuyEvent(Player buyer, String instrumentId, String instrumentSymbol, 
                              int quantity, double pricePerUnit, double totalCost) {
        this.buyer = buyer;
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalCost = totalCost;
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
