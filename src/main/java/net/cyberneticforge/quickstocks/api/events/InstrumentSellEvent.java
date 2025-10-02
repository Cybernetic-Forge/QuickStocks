package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player sells an instrument (stock, crypto, item).
 * This event is cancellable - cancel to prevent the sale.
 */
public class InstrumentSellEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player seller;
    private final String instrumentId;
    private final String instrumentSymbol;
    private final int quantity;
    private final double pricePerUnit;
    private final double totalRevenue;
    
    public InstrumentSellEvent(Player seller, String instrumentId, String instrumentSymbol, 
                               int quantity, double pricePerUnit, double totalRevenue) {
        this.seller = seller;
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalRevenue = totalRevenue;
    }
    
    public Player getSeller() {
        return seller;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public String getInstrumentSymbol() {
        return instrumentSymbol;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getPricePerUnit() {
        return pricePerUnit;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
