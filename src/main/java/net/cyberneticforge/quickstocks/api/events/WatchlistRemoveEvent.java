package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when an instrument is removed from a player's watchlist.
 * This event is cancellable - cancel to prevent removal from watchlist.
 */
@SuppressWarnings("unused")
public class WatchlistRemoveEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    @Getter
    private final Player player;
    @Getter
    private final String instrumentId;
    @Getter
    private final String instrumentSymbol;
    
    public WatchlistRemoveEvent(Player player, String instrumentId, String instrumentSymbol) {
        this.player = player;
        this.instrumentId = instrumentId;
        this.instrumentSymbol = instrumentSymbol;
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
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
