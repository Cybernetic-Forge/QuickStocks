package net.cyberneticforge.quickstocks.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player's wallet balance changes.
 * This event is not cancellable - the balance change has already occurred.
 */
@Getter
@SuppressWarnings("unused")
public class WalletBalanceChangeEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    public enum ChangeReason {
        DEPOSIT,
        WITHDRAW,
        PURCHASE,
        SALE,
        ADMIN,
        OTHER
    }
    
    private final Player player;
    private final double oldBalance;
    private final double newBalance;
    private final ChangeReason reason;
    
    public WalletBalanceChangeEvent(Player player, double oldBalance, double newBalance, ChangeReason reason) {
        this.player = player;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.reason = reason;
    }

    public double getChange() {
        return newBalance - oldBalance;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
