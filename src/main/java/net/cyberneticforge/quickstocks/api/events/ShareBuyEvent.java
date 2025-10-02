package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a player buys company shares.
 * This event is cancellable - cancel to prevent the purchase.
 */
public class ShareBuyEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player buyer;
    private final String companyId;
    private final String companyName;
    private final int quantity;
    private final double pricePerShare;
    private final double totalCost;
    
    public ShareBuyEvent(Player buyer, String companyId, String companyName, 
                         int quantity, double pricePerShare, double totalCost) {
        this.buyer = buyer;
        this.companyId = companyId;
        this.companyName = companyName;
        this.quantity = quantity;
        this.pricePerShare = pricePerShare;
        this.totalCost = totalCost;
    }
    
    public Player getBuyer() {
        return buyer;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getPricePerShare() {
        return pricePerShare;
    }
    
    public double getTotalCost() {
        return totalCost;
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
