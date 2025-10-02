package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a custom cryptocurrency is created.
 * This event is cancellable - cancel to prevent crypto creation.
 */
public class CryptoCreateEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player creator;
    private final String symbol;
    private final String name;
    private String cryptoId; // Set after creation if not cancelled
    
    public CryptoCreateEvent(Player creator, String symbol, String name) {
        this.creator = creator;
        this.symbol = symbol;
        this.name = name;
    }
    
    public Player getCreator() {
        return creator;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCryptoId() {
        return cryptoId;
    }
    
    public void setCryptoId(String cryptoId) {
        this.cryptoId = cryptoId;
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
