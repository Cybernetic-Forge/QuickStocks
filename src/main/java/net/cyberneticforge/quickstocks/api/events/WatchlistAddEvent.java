package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when an instrument is added to a player's watchlist.
 * This event is cancellable - cancel to prevent adding to watchlist.
 */
public class WatchlistAddEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    private final Player player;
    private final String instrumentId;
    private final String instrumentSymbol;
    
    public WatchlistAddEvent(Player player, String instrumentId, String instrumentSymbol) {
        this.player = player;
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public String getInstrumentSymbol() {
        return instrumentSymbol;
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
