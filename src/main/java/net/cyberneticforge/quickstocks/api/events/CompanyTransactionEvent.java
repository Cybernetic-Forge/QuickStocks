package net.cyberneticforge.quickstocks.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a company transaction occurs (deposit or withdrawal).
 * This event is cancellable - cancel to prevent the transaction.
 */
public class CompanyTransactionEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;
    
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW
    }
    
    private final String companyId;
    private final String companyName;
    private final Player player;
    private final double amount;
    private final TransactionType type;
    
    public CompanyTransactionEvent(String companyId, String companyName, Player player, double amount, TransactionType type) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.player = player;
        this.amount = amount;
        this.type = type;
    }
    
    public String getCompanyId() {
        return companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public boolean isDeposit() {
        return type == TransactionType.DEPOSIT;
    }
    
    public boolean isWithdrawal() {
        return type == TransactionType.WITHDRAW;
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
